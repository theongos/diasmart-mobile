package com.diabeto.voip

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

/**
 * Production-grade Firestore-based signaling for WebRTC.
 *
 * Collection structure:
 *   calls/{callId} → { callerUid, calleeUid, callerNom, calleeNom, type, status, offer, answer }
 *   calls/{callId}/iceCandidates/{id} → { candidate, sdpMid, sdpMLineIndex, fromUid }
 *
 * Status flow: calling → accepted → connected → ended
 *
 * Key fixes over previous version:
 * ─ Offer is always available when callee processes it (no race condition)
 * ─ ICE candidates are listened IMMEDIATELY after call creation (both sides)
 * ─ FCM push notification sent via Cloud Function trigger on call creation
 * ─ ICE restart uses versioned fields to support multiple restarts
 */
class FirestoreSignaling {

    companion object {
        private const val TAG = "FirestoreSignaling"
        private const val CALLS_COLLECTION = "calls"
        private const val ICE_COLLECTION = "iceCandidates"
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var callListener: ListenerRegistration? = null
    private var iceListener: ListenerRegistration? = null
    private var incomingCallListener: ListenerRegistration? = null

    val currentUid: String get() = auth.currentUser?.uid ?: ""

    // ── Callbacks ──
    var onCallIncoming: ((CallDoc) -> Unit)? = null
    var onCallAccepted: ((String) -> Unit)? = null
    var onCallEnded: ((String, String) -> Unit)? = null
    var onRemoteOffer: ((String, String) -> Unit)? = null
    var onRemoteAnswer: ((String, String) -> Unit)? = null
    var onRemoteIceCandidate: ((String, String, Int) -> Unit)? = null
    var onIceRestartOffer: ((String, String) -> Unit)? = null
    var onIceRestartAnswer: ((String, String) -> Unit)? = null

    data class CallDoc(
        val callId: String = "",
        val callerUid: String = "",
        val calleeUid: String = "",
        val callerNom: String = "",
        val calleeNom: String = "",
        val type: String = "video",
        val status: String = "calling",
        val offer: String? = null,
        val answer: String? = null,
        val endReason: String? = null,
        val createdAt: Timestamp? = null
    )

    // ═══════════════════════════════════════════════════════════
    // INCOMING CALL LISTENER
    // ═══════════════════════════════════════════════════════════

    fun listenForIncomingCalls() {
        val uid = currentUid
        if (uid.isEmpty()) {
            Log.w(TAG, "Cannot listen — no current user")
            return
        }

        incomingCallListener?.remove()
        Log.d(TAG, "Listening for incoming calls (uid=$uid)")

        incomingCallListener = db.collection(CALLS_COLLECTION)
            .whereEqualTo("calleeUid", uid)
            .whereEqualTo("status", "calling")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Incoming call listen error", error)
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val callDoc = CallDoc(
                            callId = doc.id,
                            callerUid = doc.getString("callerUid") ?: "",
                            calleeUid = doc.getString("calleeUid") ?: "",
                            callerNom = doc.getString("callerNom") ?: "Inconnu",
                            calleeNom = doc.getString("calleeNom") ?: "",
                            type = doc.getString("type") ?: "video",
                            status = doc.getString("status") ?: "calling",
                            offer = doc.getString("offer")
                        )
                        Log.d(TAG, "Incoming: ${callDoc.callId} from ${callDoc.callerNom} hasOffer=${callDoc.offer != null}")
                        onCallIncoming?.invoke(callDoc)
                    }
                }
            }
    }

    // ═══════════════════════════════════════════════════════════
    // CALL LIFECYCLE
    // ═══════════════════════════════════════════════════════════

    /**
     * Create a new call document with the SDP offer already included.
     * The callee will find the offer immediately when they read the document.
     */
    suspend fun initiateCall(
        calleeUid: String,
        callerNom: String,
        calleeNom: String,
        type: String,
        offer: String
    ): String {
        val callDoc = hashMapOf(
            "callerUid" to currentUid,
            "calleeUid" to calleeUid,
            "callerNom" to callerNom,
            "calleeNom" to calleeNom,
            "type" to type,
            "status" to "calling",
            "offer" to offer,
            "createdAt" to FieldValue.serverTimestamp()
        )

        val docRef = db.collection(CALLS_COLLECTION).add(callDoc).await()
        val callId = docRef.id
        Log.d(TAG, "Call created: $callId (with offer)")

        // Start listening for status/answer changes AND ICE candidates immediately
        listenToCall(callId)
        listenToIceCandidates(callId)
        return callId
    }

    /**
     * Callee accepts the call:
     * 1. Start listening to the call document FIRST (to get the offer)
     * 2. Start listening for ICE candidates
     * 3. Update status to "accepted"
     *
     * The order matters: we must be listening BEFORE we update status,
     * so we don't miss the offer that's already in the document.
     */
    suspend fun acceptCall(callId: String) {
        // Step 1: Start listening BEFORE updating status
        listenToCall(callId)
        listenToIceCandidates(callId)

        // Step 2: Small delay to ensure listeners are attached
        delay(100)

        // Step 3: Update status — this triggers the caller's listener
        db.collection(CALLS_COLLECTION).document(callId)
            .update("status", "accepted")
            .await()
        Log.d(TAG, "Call accepted: $callId")
    }

    suspend fun sendAnswer(callId: String, sdp: String) {
        db.collection(CALLS_COLLECTION).document(callId)
            .update("answer", sdp, "status", "connected")
            .await()
        Log.d(TAG, "Answer sent: $callId → status=connected")
    }

    suspend fun rejectCall(callId: String) {
        db.collection(CALLS_COLLECTION).document(callId)
            .update("status", "ended", "endReason", "rejected")
            .await()
    }

    suspend fun endCall(callId: String) {
        try {
            db.collection(CALLS_COLLECTION).document(callId)
                .update("status", "ended", "endReason", "ended")
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error ending call", e)
        }
        cleanup()
    }

    // ═══════════════════════════════════════════════════════════
    // ICE CANDIDATES
    // ═══════════════════════════════════════════════════════════

    suspend fun sendIceCandidate(callId: String, candidate: String, sdpMid: String, sdpMLineIndex: Int) {
        if (callId.isEmpty()) return
        val iceDoc = hashMapOf(
            "candidate" to candidate,
            "sdpMid" to sdpMid,
            "sdpMLineIndex" to sdpMLineIndex,
            "fromUid" to currentUid,
            "timestamp" to FieldValue.serverTimestamp()
        )
        try {
            db.collection(CALLS_COLLECTION).document(callId)
                .collection(ICE_COLLECTION).add(iceDoc).await()
        } catch (e: Exception) {
            Log.e(TAG, "ICE candidate send error", e)
        }
    }

    fun listenToIceCandidates(callId: String) {
        if (callId.isEmpty()) return
        iceListener?.remove()
        Log.d(TAG, "Listening for ICE candidates: $callId")

        iceListener = db.collection(CALLS_COLLECTION).document(callId)
            .collection(ICE_COLLECTION)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "ICE listen error", error)
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val fromUid = doc.getString("fromUid") ?: ""
                        if (fromUid != currentUid) {
                            val candidate = doc.getString("candidate") ?: return@forEach
                            val sdpMid = doc.getString("sdpMid") ?: return@forEach
                            val sdpMLineIndex = doc.getLong("sdpMLineIndex")?.toInt() ?: return@forEach
                            Log.d(TAG, "ICE from remote: sdpMid=$sdpMid")
                            onRemoteIceCandidate?.invoke(candidate, sdpMid, sdpMLineIndex)
                        }
                    }
                }
            }
    }

    // ═══════════════════════════════════════════════════════════
    // ICE RESTART
    // ═══════════════════════════════════════════════════════════

    suspend fun sendIceRestartOffer(callId: String, sdp: String) {
        val restartId = System.currentTimeMillis().toString()
        try {
            db.collection(CALLS_COLLECTION).document(callId)
                .update(
                    "iceRestartOffer", sdp,
                    "iceRestartId", restartId,
                    "iceRestartFromUid", currentUid
                )
                .await()
            Log.d(TAG, "ICE restart offer sent (id=$restartId)")
        } catch (e: Exception) {
            Log.e(TAG, "ICE restart offer error", e)
        }
    }

    suspend fun sendIceRestartAnswer(callId: String, sdp: String) {
        try {
            db.collection(CALLS_COLLECTION).document(callId)
                .update("iceRestartAnswer", sdp)
                .await()
            Log.d(TAG, "ICE restart answer sent")
        } catch (e: Exception) {
            Log.e(TAG, "ICE restart answer error", e)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CALL DOCUMENT LISTENER
    // ═══════════════════════════════════════════════════════════

    private fun listenToCall(callId: String) {
        callListener?.remove()

        var offerProcessed = false
        var answerProcessed = false
        var acceptedNotified = false
        var lastIceRestartId: String? = null
        var lastIceRestartAnswerProcessed = false

        callListener = db.collection(CALLS_COLLECTION).document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Call listen error", error)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val status = snapshot.getString("status") ?: return@addSnapshotListener
                val offer = snapshot.getString("offer")
                val answer = snapshot.getString("answer")
                val isCallee = snapshot.getString("calleeUid") == currentUid

                // ICE restart fields
                val iceRestartOffer = snapshot.getString("iceRestartOffer")
                val iceRestartAnswer = snapshot.getString("iceRestartAnswer")
                val iceRestartId = snapshot.getString("iceRestartId")
                val iceRestartFromUid = snapshot.getString("iceRestartFromUid")

                Log.d(TAG, "Doc update: status=$status hasOffer=${offer != null} hasAnswer=${answer != null} isCallee=$isCallee offerProcessed=$offerProcessed answerProcessed=$answerProcessed")

                // ── CRITICAL: Process offer for callee FIRST, before status handling ──
                // The offer is in the document from the start (set during initiateCall).
                // We process it as soon as we see it, regardless of status.
                if (!offerProcessed && offer != null && isCallee) {
                    offerProcessed = true
                    Log.d(TAG, "Processing offer for callee (status=$status)")
                    onRemoteOffer?.invoke(callId, offer)
                }

                // ── Status handling ──
                when (status) {
                    "accepted" -> {
                        if (!acceptedNotified) {
                            acceptedNotified = true
                            onCallAccepted?.invoke(callId)
                        }
                    }
                    "connected" -> {
                        // Process answer for caller
                        if (!answerProcessed && answer != null && !isCallee) {
                            answerProcessed = true
                            Log.d(TAG, "Processing answer for caller")
                            onRemoteAnswer?.invoke(callId, answer)
                        }
                    }
                    "ended" -> {
                        val reason = snapshot.getString("endReason") ?: "ended"
                        onCallEnded?.invoke(callId, reason)
                        cleanup()
                        return@addSnapshotListener
                    }
                }

                // ── ICE restart handling ──
                if (iceRestartOffer != null && iceRestartId != null &&
                    iceRestartFromUid != currentUid && iceRestartId != lastIceRestartId) {
                    lastIceRestartId = iceRestartId
                    lastIceRestartAnswerProcessed = false
                    Log.d(TAG, "Processing ICE restart offer (id=$iceRestartId)")
                    onIceRestartOffer?.invoke(callId, iceRestartOffer)
                }

                if (iceRestartAnswer != null && iceRestartFromUid == currentUid &&
                    !lastIceRestartAnswerProcessed) {
                    lastIceRestartAnswerProcessed = true
                    Log.d(TAG, "Processing ICE restart answer")
                    onIceRestartAnswer?.invoke(callId, iceRestartAnswer)
                }
            }
    }

    // ═══════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════

    fun cleanup() {
        callListener?.remove()
        callListener = null
        iceListener?.remove()
        iceListener = null
    }

    fun stopListening() {
        incomingCallListener?.remove()
        incomingCallListener = null
        cleanup()
    }
}
