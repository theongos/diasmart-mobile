package com.diabeto.voip

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log

/**
 * Android TelecomManager ConnectionService.
 * Enables native call UI (lock screen answer, notification shade controls).
 */
class CallConnectionService : ConnectionService() {

    companion object {
        private const val TAG = "CallConnectionService"
        var currentConnection: CallConnection? = null
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(TAG, "onCreateIncomingConnection")

        val extras = request?.extras
        val callerName = extras?.getString("callerNom") ?: "Appel entrant"

        val connection = CallConnection(this).apply {
            setCallerDisplayName(callerName, TelecomManager.PRESENTATION_ALLOWED)
            setAddress(request?.address, TelecomManager.PRESENTATION_ALLOWED)
            setInitializing()
            setRinging()
            connectionProperties = Connection.PROPERTY_SELF_MANAGED
            audioModeIsVoip = true
        }

        currentConnection = connection
        return connection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(TAG, "onCreateOutgoingConnection")

        val connection = CallConnection(this).apply {
            setInitializing()
            setDialing()
            connectionProperties = Connection.PROPERTY_SELF_MANAGED
            audioModeIsVoip = true
        }

        currentConnection = connection
        return connection
    }

    class CallConnection(private val service: CallConnectionService) : Connection() {

        override fun onAnswer() {
            Log.d(TAG, "onAnswer (native)")
            setActive()
            // Trigger accept in CallManager
            CallManagerProvider.callManager?.acceptCall()
        }

        override fun onReject() {
            Log.d(TAG, "onReject (native)")
            setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
            destroy()
            CallManagerProvider.callManager?.rejectCall()
        }

        override fun onDisconnect() {
            Log.d(TAG, "onDisconnect (native)")
            setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            destroy()
            CallManagerProvider.callManager?.endCall()
        }

        override fun onAbort() {
            Log.d(TAG, "onAbort")
            setDisconnected(DisconnectCause(DisconnectCause.CANCELED))
            destroy()
        }

        fun setCallActive() {
            setActive()
        }

        fun endConnection(cause: Int = DisconnectCause.REMOTE) {
            setDisconnected(DisconnectCause(cause))
            destroy()
        }
    }
}

/**
 * Simple provider to access CallManager from ConnectionService.
 * In production, use Hilt EntryPoint instead.
 */
object CallManagerProvider {
    var callManager: CallManager? = null
}
