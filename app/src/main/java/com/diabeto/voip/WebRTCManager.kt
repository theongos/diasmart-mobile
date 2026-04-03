package com.diabeto.voip

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.diabeto.BuildConfig
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production-grade WebRTC peer connection manager.
 *
 * Key improvements over basic implementation:
 * ─────────────────────────────────────────────
 * 1. Reliable TURN servers with TCP/TLS fallback (firewall bypass)
 * 2. onTrack-based remote media (Unified Plan compliant)
 * 3. ICE restart support for network recovery
 * 4. Adaptive video resolution (480p default, not 720p)
 * 5. SDP bandwidth limiting for low-bandwidth networks
 * 6. Network change detection (WiFi↔4G auto-recovery)
 * 7. DataChannel heartbeat for dead connection detection
 * 8. Comprehensive logging for debugging
 */
@Singleton
class WebRTCManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "WebRTCManager"

        // ── ICE Server Configuration ──
        // Multiple STUN + TURN with UDP/TCP/TLS for maximum connectivity
        // TCP variants bypass restrictive firewalls (common in Africa/enterprise networks)
        fun buildIceServers(): List<PeerConnection.IceServer> {
            val turnUser = BuildConfig.TURN_USERNAME
            val turnPass = BuildConfig.TURN_PASSWORD
            return listOf(
                // STUN (free, reliable Google servers)
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                    .createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302")
                    .createIceServer(),
                PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302")
                    .createIceServer(),
                PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302")
                    .createIceServer(),

                // TURN - UDP (fastest, works in most cases)
                PeerConnection.IceServer.builder("turn:a.relay.metered.ca:80?transport=udp")
                    .setUsername(turnUser).setPassword(turnPass)
                    .createIceServer(),

                // TURN - TCP (firewall bypass when UDP is blocked)
                PeerConnection.IceServer.builder("turn:a.relay.metered.ca:80?transport=tcp")
                    .setUsername(turnUser).setPassword(turnPass)
                    .createIceServer(),

                // TURN - TLS on 443 (maximum firewall bypass — looks like HTTPS traffic)
                PeerConnection.IceServer.builder("turn:a.relay.metered.ca:443?transport=tcp")
                    .setUsername(turnUser).setPassword(turnPass)
                    .createIceServer(),

                // TURNS - secure relay (last resort, works through most corporate firewalls)
                PeerConnection.IceServer.builder("turns:a.relay.metered.ca:443?transport=tcp")
                    .setUsername(turnUser).setPassword(turnPass)
                    .createIceServer(),
            )
        }

        // Video quality presets (adaptive based on network)
        const val VIDEO_WIDTH_LOW = 320
        const val VIDEO_HEIGHT_LOW = 240
        const val VIDEO_FPS_LOW = 15

        const val VIDEO_WIDTH_MED = 480
        const val VIDEO_HEIGHT_MED = 360
        const val VIDEO_FPS_MED = 20

        const val VIDEO_WIDTH_HIGH = 640
        const val VIDEO_HEIGHT_HIGH = 480
        const val VIDEO_FPS_HIGH = 24

        // Max bitrates for SDP munging
        const val AUDIO_BITRATE_KBPS = 32     // Opus is efficient at 32kbps
        const val VIDEO_BITRATE_LOW_KBPS = 150
        const val VIDEO_BITRATE_MED_KBPS = 400
        const val VIDEO_BITRATE_HIGH_KBPS = 800
    }

    // ── State ──
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var localVideoSource: VideoSource? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var eglBase: EglBase? = null
    private var dataChannel: DataChannel? = null

    // Network monitoring
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Heartbeat
    private var heartbeatRunnable: Runnable? = null
    private var lastHeartbeatReceived = 0L
    private val HEARTBEAT_INTERVAL_MS = 5000L
    private val HEARTBEAT_TIMEOUT_MS = 15000L

    // Current video quality
    private var currentVideoWidth = VIDEO_WIDTH_MED
    private var currentVideoHeight = VIDEO_HEIGHT_MED
    private var currentVideoFps = VIDEO_FPS_MED

    private var isVideoCall = false

    // ── Callbacks (volatile for cross-thread visibility during dispose) ──
    @Volatile var onIceCandidate: ((IceCandidate) -> Unit)? = null
    @Volatile var onRemoteVideoTrack: ((VideoTrack) -> Unit)? = null
    @Volatile var onRemoteAudioTrack: ((AudioTrack) -> Unit)? = null
    @Volatile var onConnectionStateChange: ((PeerConnection.IceConnectionState) -> Unit)? = null
    @Volatile var onPeerConnectionStateChange: ((PeerConnection.PeerConnectionState) -> Unit)? = null
    @Volatile var onLocalVideoTrack: ((VideoTrack) -> Unit)? = null
    @Volatile var onNetworkChanged: (() -> Unit)? = null
    @Volatile var onHeartbeatTimeout: (() -> Unit)? = null

    // ═══════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════

    fun initialize() {
        if (peerConnectionFactory != null) return

        eglBase = EglBase.create()

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions()
        )

        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .setUseStereoInput(false)
            .setUseStereoOutput(false)
            .createAudioDeviceModule()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    eglBase!!.eglBaseContext,
                    true,   // enableIntelVp8Encoder
                    true    // enableH264HighProfile
                )
            )
            .setVideoDecoderFactory(
                DefaultVideoDecoderFactory(eglBase!!.eglBaseContext)
            )
            .setOptions(PeerConnectionFactory.Options().apply {
                disableNetworkMonitor = false
            })
            .createPeerConnectionFactory()

        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        Log.d(TAG, "✅ PeerConnectionFactory initialized")
    }

    fun getEglContext(): EglBase.Context? = eglBase?.eglBaseContext

    // ═══════════════════════════════════════════════════════════
    // PEER CONNECTION
    // ═══════════════════════════════════════════════════════════

    fun createPeerConnection(isVideoCall: Boolean): PeerConnection? {
        this.isVideoCall = isVideoCall

        val iceServers = buildIceServers()
        Log.d(TAG, "Creating PeerConnection with ${iceServers.size} ICE servers")

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType = PeerConnection.IceTransportsType.ALL

            // Aggressive ICE candidate gathering for unstable networks
            iceCandidatePoolSize = 2

            // Bundle all media over one connection (reduces NAT holes needed)
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE

            // Enable ICE restart support
            iceCheckMinInterval = 1000

            // Use all available network interfaces
            candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.ALL
        }

        val observer = object : PeerConnection.Observer {

            override fun onIceCandidate(candidate: IceCandidate) {
                Log.d(TAG, "🧊 ICE candidate: type=${candidate.sdp.substringBefore(" ")} mid=${candidate.sdpMid}")
                onIceCandidate?.invoke(candidate)
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                Log.d(TAG, "ICE candidates removed: ${candidates?.size}")
            }

            // DEPRECATED in Unified Plan — but keep as fallback
            override fun onAddStream(stream: MediaStream) {
                Log.d(TAG, "📡 onAddStream (legacy): id=${stream.id} audio=${stream.audioTracks.size} video=${stream.videoTracks.size}")
            }

            override fun onRemoveStream(stream: MediaStream) {
                Log.d(TAG, "Stream removed: ${stream.id}")
            }

            override fun onDataChannel(channel: DataChannel) {
                Log.d(TAG, "📊 DataChannel received: ${channel.label()}")
                if (channel.label() == "heartbeat") {
                    setupHeartbeatReceiver(channel)
                }
            }

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "⚠️ Renegotiation needed")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                Log.d(TAG, "🧊 ICE connection: $state")
                onConnectionStateChange?.invoke(state)
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(TAG, "ICE receiving: $receiving")
            }

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                Log.d(TAG, "🧊 ICE gathering: $state")
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState) {
                Log.d(TAG, "📶 Signaling: $state")
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                Log.d(TAG, "🔌 Connection state: $newState")
                onPeerConnectionStateChange?.invoke(newState)
            }

            override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {
                Log.d(TAG, "📍 Selected candidate pair changed")
            }

            // ── CRITICAL: onTrack for Unified Plan ──
            // This is how remote media arrives in Unified Plan (not onAddStream)
            override fun onTrack(transceiver: RtpTransceiver) {
                Log.d(TAG, "🎬 onTrack: kind=${transceiver.mediaType} mid=${transceiver.mid}")
                val track = transceiver.receiver.track()
                when (track) {
                    is VideoTrack -> {
                        Log.d(TAG, "✅ Remote VIDEO track received: ${track.id()}")
                        track.setEnabled(true)
                        mainHandler.post { onRemoteVideoTrack?.invoke(track) }
                    }
                    is AudioTrack -> {
                        Log.d(TAG, "✅ Remote AUDIO track received: ${track.id()}")
                        track.setEnabled(true)
                        mainHandler.post { onRemoteAudioTrack?.invoke(track) }
                    }
                    else -> Log.w(TAG, "Unknown track type: ${track?.kind()}")
                }
            }
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)

        if (peerConnection == null) {
            Log.e(TAG, "❌ Failed to create PeerConnection!")
            return null
        }

        // Add local audio ALWAYS
        startLocalAudio()

        // Add local video only for video calls
        if (isVideoCall) {
            startLocalVideo()
        }

        // Create heartbeat DataChannel
        createHeartbeatChannel()

        // Start network monitoring
        startNetworkMonitoring()

        Log.d(TAG, "✅ PeerConnection created (video=$isVideoCall)")
        return peerConnection
    }

    // ═══════════════════════════════════════════════════════════
    // LOCAL MEDIA
    // ═══════════════════════════════════════════════════════════

    private fun startLocalAudio() {
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        }

        val audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("audio_local_0", audioSource)
        localAudioTrack?.setEnabled(true)

        val result = peerConnection?.addTrack(localAudioTrack, listOf("local_stream"))
        Log.d(TAG, "Local audio track added: ${result != null}, enabled=${localAudioTrack?.enabled()}")
    }

    private fun startLocalVideo() {
        val factory = peerConnectionFactory ?: return

        localVideoSource = factory.createVideoSource(false)
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase!!.eglBaseContext)

        videoCapturer = createCameraCapturer()
        if (videoCapturer == null) {
            Log.e(TAG, "❌ No camera available!")
            return
        }

        videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource!!.capturerObserver)

        // Start with MEDIUM quality — adaptive for unstable networks
        val quality = detectNetworkQuality()
        applyVideoQuality(quality)

        localVideoTrack = factory.createVideoTrack("video_local_0", localVideoSource)
        localVideoTrack?.setEnabled(true)
        peerConnection?.addTrack(localVideoTrack, listOf("local_stream"))

        onLocalVideoTrack?.invoke(localVideoTrack!!)
        Log.d(TAG, "✅ Local video started: ${currentVideoWidth}x${currentVideoHeight}@${currentVideoFps}fps")
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        // Prefer front camera
        for (name in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                Log.d(TAG, "Using front camera: $name")
                return enumerator.createCapturer(name, null)
            }
        }
        // Fallback to any camera
        for (name in enumerator.deviceNames) {
            Log.d(TAG, "Fallback camera: $name")
            return enumerator.createCapturer(name, null)
        }
        return null
    }

    // ═══════════════════════════════════════════════════════════
    // ADAPTIVE VIDEO QUALITY
    // ═══════════════════════════════════════════════════════════

    enum class NetworkQuality { LOW, MEDIUM, HIGH }

    private fun detectNetworkQuality(): NetworkQuality {
        val cm = connectivityManager ?: return NetworkQuality.LOW
        val network = cm.activeNetwork ?: return NetworkQuality.LOW
        val caps = cm.getNetworkCapabilities(network) ?: return NetworkQuality.LOW

        val downKbps = caps.linkDownstreamBandwidthKbps
        Log.d(TAG, "Network bandwidth: ${downKbps}kbps")

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && downKbps > 5000 -> NetworkQuality.HIGH
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkQuality.MEDIUM
            downKbps > 3000 -> NetworkQuality.MEDIUM
            else -> NetworkQuality.LOW   // 2G/3G or low 4G
        }
    }

    private fun applyVideoQuality(quality: NetworkQuality) {
        when (quality) {
            NetworkQuality.LOW -> {
                currentVideoWidth = VIDEO_WIDTH_LOW
                currentVideoHeight = VIDEO_HEIGHT_LOW
                currentVideoFps = VIDEO_FPS_LOW
            }
            NetworkQuality.MEDIUM -> {
                currentVideoWidth = VIDEO_WIDTH_MED
                currentVideoHeight = VIDEO_HEIGHT_MED
                currentVideoFps = VIDEO_FPS_MED
            }
            NetworkQuality.HIGH -> {
                currentVideoWidth = VIDEO_WIDTH_HIGH
                currentVideoHeight = VIDEO_HEIGHT_HIGH
                currentVideoFps = VIDEO_FPS_HIGH
            }
        }
        try {
            videoCapturer?.startCapture(currentVideoWidth, currentVideoHeight, currentVideoFps)
            Log.d(TAG, "📹 Video quality: $quality → ${currentVideoWidth}x${currentVideoHeight}@${currentVideoFps}fps")
        } catch (e: Exception) {
            Log.e(TAG, "Error changing video capture", e)
        }
    }

    /**
     * Dynamically adjust video quality during a call.
     * Call this when network conditions change.
     */
    fun adaptVideoQuality() {
        if (!isVideoCall || videoCapturer == null) return
        val quality = detectNetworkQuality()
        try {
            videoCapturer?.changeCaptureFormat(currentVideoWidth, currentVideoHeight, currentVideoFps)
        } catch (e: Exception) {
            // changeCaptureFormat not supported on all devices, try restart
            try {
                videoCapturer?.stopCapture()
                applyVideoQuality(quality)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to adapt video quality", e2)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SDP BANDWIDTH LIMITING
    // ═══════════════════════════════════════════════════════════

    /**
     * Modify SDP to limit bandwidth — critical for 3G/4G in Africa.
     * Adds b=AS: lines to limit audio/video bitrate.
     */
    private fun limitSdpBandwidth(sdp: String, isVideo: Boolean): String {
        val lines = sdp.split("\r\n").toMutableList()
        val result = mutableListOf<String>()
        val quality = detectNetworkQuality()

        val videoBitrate = when (quality) {
            NetworkQuality.LOW -> VIDEO_BITRATE_LOW_KBPS
            NetworkQuality.MEDIUM -> VIDEO_BITRATE_MED_KBPS
            NetworkQuality.HIGH -> VIDEO_BITRATE_HIGH_KBPS
        }

        var inAudio = false
        var inVideo = false

        for (line in lines) {
            result.add(line)

            if (line.startsWith("m=audio")) {
                inAudio = true
                inVideo = false
            } else if (line.startsWith("m=video")) {
                inAudio = false
                inVideo = true
            }

            // Add bandwidth limit after c= line in each media section
            if (line.startsWith("c=IN") && inAudio) {
                result.add("b=AS:$AUDIO_BITRATE_KBPS")
            } else if (line.startsWith("c=IN") && inVideo && isVideo) {
                result.add("b=AS:$videoBitrate")
            }
        }

        val modified = result.joinToString("\r\n")
        Log.d(TAG, "SDP bandwidth: audio=${AUDIO_BITRATE_KBPS}kbps video=${videoBitrate}kbps (quality=$quality)")
        return modified
    }

    // ═══════════════════════════════════════════════════════════
    // HEARTBEAT (dead connection detection)
    // ═══════════════════════════════════════════════════════════

    private fun createHeartbeatChannel() {
        try {
            val init = DataChannel.Init().apply {
                ordered = false
                maxRetransmits = 0
            }
            dataChannel = peerConnection?.createDataChannel("heartbeat", init)
            dataChannel?.registerObserver(object : DataChannel.Observer {
                override fun onBufferedAmountChange(previousAmount: Long) {}
                override fun onStateChange() {
                    Log.d(TAG, "💓 Heartbeat channel state: ${dataChannel?.state()}")
                    if (dataChannel?.state() == DataChannel.State.OPEN) {
                        startHeartbeat()
                    }
                }
                override fun onMessage(buffer: DataChannel.Buffer) {
                    lastHeartbeatReceived = System.currentTimeMillis()
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "DataChannel not supported, skipping heartbeat", e)
        }
    }

    private fun setupHeartbeatReceiver(channel: DataChannel) {
        channel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}
            override fun onStateChange() {}
            override fun onMessage(buffer: DataChannel.Buffer) {
                lastHeartbeatReceived = System.currentTimeMillis()
                // Echo back
                try {
                    val ping = DataChannel.Buffer(
                        java.nio.ByteBuffer.wrap("pong".toByteArray()),
                        false
                    )
                    channel.send(ping)
                } catch (e: Exception) { /* ignore */ }
            }
        })
    }

    private fun startHeartbeat() {
        lastHeartbeatReceived = System.currentTimeMillis()
        heartbeatRunnable = object : Runnable {
            override fun run() {
                try {
                    if (dataChannel?.state() == DataChannel.State.OPEN) {
                        val ping = DataChannel.Buffer(
                            java.nio.ByteBuffer.wrap("ping".toByteArray()),
                            false
                        )
                        dataChannel?.send(ping)

                        // Check if remote is alive
                        val elapsed = System.currentTimeMillis() - lastHeartbeatReceived
                        if (elapsed > HEARTBEAT_TIMEOUT_MS && lastHeartbeatReceived > 0) {
                            Log.e(TAG, "💀 Heartbeat timeout! Last received ${elapsed}ms ago")
                            onHeartbeatTimeout?.invoke()
                            return
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Heartbeat send failed", e)
                }
                mainHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
            }
        }
        mainHandler.postDelayed(heartbeatRunnable!!, HEARTBEAT_INTERVAL_MS)
        Log.d(TAG, "💓 Heartbeat started (interval=${HEARTBEAT_INTERVAL_MS}ms, timeout=${HEARTBEAT_TIMEOUT_MS}ms)")
    }

    private fun stopHeartbeat() {
        heartbeatRunnable?.let { mainHandler.removeCallbacks(it) }
        heartbeatRunnable = null
    }

    // ═══════════════════════════════════════════════════════════
    // NETWORK MONITORING (WiFi ↔ 4G auto-recovery)
    // ═══════════════════════════════════════════════════════════

    private fun startNetworkMonitoring() {
        val cm = connectivityManager ?: return

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "🌐 Network available: $network")
                mainHandler.post { onNetworkChanged?.invoke() }
            }

            override fun onLost(network: Network) {
                Log.w(TAG, "🌐 Network lost: $network")
                mainHandler.post { onNetworkChanged?.invoke() }
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                // Network quality changed — adapt video
                if (isVideoCall) {
                    mainHandler.post { adaptVideoQuality() }
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            cm.registerNetworkCallback(request, networkCallback!!)
            Log.d(TAG, "🌐 Network monitoring started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    private fun stopNetworkMonitoring() {
        networkCallback?.let {
            try {
                connectivityManager?.unregisterNetworkCallback(it)
            } catch (e: Exception) { /* already unregistered */ }
        }
        networkCallback = null
    }

    // ═══════════════════════════════════════════════════════════
    // ICE RESTART (network recovery without full renegotiation)
    // ═══════════════════════════════════════════════════════════

    /**
     * Perform an ICE restart — creates a new offer with ice-restart flag.
     * This recovers the connection when network changes (WiFi↔4G)
     * WITHOUT needing to tear down and rebuild the entire call.
     */
    fun restartIce(callback: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false"))
        }

        Log.d(TAG, "🔄 Performing ICE restart...")

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                val limited = limitSdpBandwidth(sdp.description, isVideoCall)
                val newSdp = SessionDescription(sdp.type, limited)
                peerConnection?.setLocalDescription(LoggingSdpObserver("ICE restart setLocal"), newSdp)
                callback(newSdp)
            }
            override fun onCreateFailure(error: String) {
                Log.e(TAG, "❌ ICE restart offer failed: $error")
            }
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {}
        }, constraints)
    }

    // ═══════════════════════════════════════════════════════════
    // SDP NEGOTIATION
    // ═══════════════════════════════════════════════════════════

    fun createOffer(callback: (SessionDescription) -> Unit) {
        val pc = peerConnection
        if (pc == null) {
            Log.e(TAG, "❌ Cannot create offer - no PeerConnection")
            return
        }

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false"))
        }

        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                Log.d(TAG, "✅ Offer created (${sdp.description.length} chars)")
                val limited = limitSdpBandwidth(sdp.description, isVideoCall)
                val newSdp = SessionDescription(sdp.type, limited)
                pc.setLocalDescription(LoggingSdpObserver("setLocalOffer"), newSdp)
                callback(newSdp)
            }
            override fun onCreateFailure(error: String) {
                Log.e(TAG, "❌ Create offer FAILED: $error")
            }
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {}
        }, constraints)
    }

    fun createAnswer(callback: (SessionDescription) -> Unit) {
        val pc = peerConnection
        if (pc == null) {
            Log.e(TAG, "❌ Cannot create answer - no PeerConnection")
            return
        }

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false"))
        }

        pc.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                Log.d(TAG, "✅ Answer created (${sdp.description.length} chars)")
                val limited = limitSdpBandwidth(sdp.description, isVideoCall)
                val newSdp = SessionDescription(sdp.type, limited)
                pc.setLocalDescription(LoggingSdpObserver("setLocalAnswer"), newSdp)
                callback(newSdp)
            }
            override fun onCreateFailure(error: String) {
                Log.e(TAG, "❌ Create answer FAILED: $error")
            }
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {}
        }, constraints)
    }

    fun setRemoteDescription(sdp: SessionDescription, callback: (() -> Unit)? = null) {
        val pc = peerConnection
        if (pc == null) {
            Log.e(TAG, "❌ Cannot set remote desc - no PeerConnection")
            return
        }

        pc.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(TAG, "✅ Remote description set (type=${sdp.type})")
                callback?.invoke()
            }
            override fun onSetFailure(error: String) {
                Log.e(TAG, "❌ Set remote description FAILED: $error")
            }
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate): Boolean {
        return try {
            val result = peerConnection?.addIceCandidate(candidate)
            if (result != true) {
                Log.w(TAG, "⚠️ addIceCandidate returned false for ${candidate.sdpMid}")
            }
            result ?: false
        } catch (e: Exception) {
            Log.e(TAG, "❌ addIceCandidate exception", e)
            false
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CONTROLS
    // ═══════════════════════════════════════════════════════════

    fun switchCamera() {
        videoCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFront: Boolean) {
                Log.d(TAG, "📷 Camera switched: front=$isFront")
            }
            override fun onCameraSwitchError(error: String) {
                Log.e(TAG, "📷 Camera switch failed: $error")
            }
        })
    }

    fun toggleMicrophone(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
        Log.d(TAG, "🎤 Microphone: ${if (enabled) "ON" else "OFF"}")
    }

    fun toggleCamera(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
        Log.d(TAG, "📷 Camera: ${if (enabled) "ON" else "OFF"}")
    }

    /**
     * Get current connection stats for debugging.
     */
    fun getStats(callback: (RTCStatsReport) -> Unit) {
        peerConnection?.getStats { report ->
            callback(report)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════

    fun dispose() {
        Log.d(TAG, "Disposing WebRTC resources...")

        try { stopHeartbeat() } catch (e: Exception) { Log.w(TAG, "Heartbeat stop error", e) }
        try { stopNetworkMonitoring() } catch (e: Exception) { Log.w(TAG, "Network monitor stop error", e) }
        // Remove any pending handler callbacks
        mainHandler.removeCallbacksAndMessages(null)

        try {
            dataChannel?.close()
            dataChannel?.dispose()
        } catch (e: Exception) { Log.w(TAG, "DataChannel dispose error", e) }
        finally { dataChannel = null }

        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
        } catch (e: Exception) { Log.w(TAG, "Capturer dispose error", e) }
        finally { videoCapturer = null }

        try { localVideoTrack?.dispose() } catch (e: Exception) { Log.w(TAG, "VideoTrack dispose error", e) }
        finally { localVideoTrack = null }
        try { localAudioTrack?.dispose() } catch (e: Exception) { Log.w(TAG, "AudioTrack dispose error", e) }
        finally { localAudioTrack = null }
        try { localVideoSource?.dispose() } catch (e: Exception) { Log.w(TAG, "VideoSource dispose error", e) }
        finally { localVideoSource = null }

        try { surfaceTextureHelper?.dispose() } catch (e: Exception) { Log.w(TAG, "SurfaceHelper dispose error", e) }
        finally { surfaceTextureHelper = null }

        try {
            peerConnection?.close()
            peerConnection?.dispose()
        } catch (e: Exception) { Log.w(TAG, "PeerConnection dispose error", e) }
        finally { peerConnection = null }

        // Clear callbacks to prevent memory leaks
        onIceCandidate = null
        onRemoteVideoTrack = null
        onRemoteAudioTrack = null
        onConnectionStateChange = null
        onPeerConnectionStateChange = null
        onLocalVideoTrack = null
        onNetworkChanged = null
        onHeartbeatTimeout = null

        Log.d(TAG, "WebRTC resources disposed")
    }

    fun release() {
        dispose()
        try { peerConnectionFactory?.dispose() } catch (e: Exception) { Log.w(TAG, "Factory dispose error", e) }
        finally { peerConnectionFactory = null }
        try { eglBase?.release() } catch (e: Exception) { Log.w(TAG, "EGL release error", e) }
        finally { eglBase = null }
        Log.d(TAG, "WebRTC fully released")
    }

    // ═══════════════════════════════════════════════════════════
    // LOGGING HELPER
    // ═══════════════════════════════════════════════════════════

    private class LoggingSdpObserver(private val label: String) : SdpObserver {
        override fun onSetSuccess() {
            Log.d(TAG, "✅ $label: success")
        }
        override fun onSetFailure(error: String?) {
            Log.e(TAG, "❌ $label: FAILED — $error")
        }
        override fun onCreateSuccess(sdp: SessionDescription?) {}
        override fun onCreateFailure(error: String?) {}
    }
}
