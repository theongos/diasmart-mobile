package com.diabeto.voip

import com.google.firebase.Timestamp

/**
 * Call history record stored in Firestore "call_history" collection.
 */
data class CallHistory(
    val callId: String = "",
    val callerUid: String = "",
    val calleeUid: String = "",
    val callerNom: String = "",
    val calleeNom: String = "",
    val type: String = "video", // "audio" or "video"
    val status: String = "", // "ended", "rejected", "timeout", "disconnected"
    val duration: Int = 0, // seconds
    val startedAt: Timestamp? = null,
    val connectedAt: Timestamp? = null,
    val endedAt: Timestamp? = null
) {
    val isOutgoing: Boolean
        get() = false // set by caller

    val isMissed: Boolean
        get() = status == "timeout" || status == "disconnected"

    val isRejected: Boolean
        get() = status == "rejected"

    val formattedDuration: String
        get() {
            if (duration == 0) return ""
            val mins = duration / 60
            val secs = duration % 60
            return "%d:%02d".format(mins, secs)
        }
}
