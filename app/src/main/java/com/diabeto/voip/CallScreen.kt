package com.diabeto.voip

import android.view.ViewGroup
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/**
 * In-call screen with video renderers and controls.
 * Supports: audio-only, video, mute, camera toggle, speaker, switch camera.
 */
@Composable
fun CallScreen(
    callManager: CallManager,
    onNavigateBack: () -> Unit
) {
    val callInfo by callManager.callState.collectAsState()
    val localVideoTrack by callManager.localVideoTrack.collectAsState()
    val remoteVideoTrack by callManager.remoteVideoTrack.collectAsState()
    val eglContext = callManager.getEglContext()

    // Duration is now managed by CallManager
    val durationSeconds = callInfo.durationSeconds

    // Navigate back when call ends
    LaunchedEffect(callInfo.state) {
        if (callInfo.state == CallManager.CallState.ENDED || callInfo.state == CallManager.CallState.IDLE) {
            if (callInfo.callId.isEmpty()) {
                delay(500)
                onNavigateBack()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1628))
    ) {
        if (callInfo.isVideo && (callInfo.state == CallManager.CallState.CONNECTED || callInfo.state == CallManager.CallState.RECONNECTING)) {
            // Video call UI
            VideoCallContent(
                callInfo = callInfo,
                localVideoTrack = localVideoTrack,
                remoteVideoTrack = remoteVideoTrack,
                eglContext = eglContext,
                durationSeconds = durationSeconds,
                onEndCall = { callManager.endCall() },
                onToggleMute = { callManager.toggleMute() },
                onToggleCamera = { callManager.toggleCamera() },
                onSwitchCamera = { callManager.switchCamera() },
                onToggleSpeaker = { callManager.toggleSpeaker() }
            )
        } else {
            // Audio call or connecting state
            AudioCallContent(
                callInfo = callInfo,
                durationSeconds = durationSeconds,
                onEndCall = { callManager.endCall() },
                onToggleMute = { callManager.toggleMute() },
                onToggleSpeaker = { callManager.toggleSpeaker() }
            )
        }
    }
}

@Composable
private fun VideoCallContent(
    callInfo: CallManager.CallInfo,
    localVideoTrack: VideoTrack?,
    remoteVideoTrack: VideoTrack?,
    eglContext: EglBase.Context?,
    durationSeconds: Int,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Remote video (full screen)
        if (remoteVideoTrack != null && eglContext != null) {
            WebRTCVideoView(
                videoTrack = remoteVideoTrack,
                eglContext = eglContext,
                modifier = Modifier.fillMaxSize(),
                mirror = false
            )
        } else {
            // Placeholder while connecting
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A1628)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Connexion video...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 18.sp
                )
            }
        }

        // Local video (PIP - top right)
        if (localVideoTrack != null && eglContext != null && !callInfo.isCameraOff) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
                    .size(120.dp, 170.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                WebRTCVideoView(
                    videoTrack = localVideoTrack,
                    eglContext = eglContext,
                    modifier = Modifier.fillMaxSize(),
                    mirror = true
                )
            }
        }

        // Top bar: name + duration
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    callInfo.remoteNom,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatDuration(durationSeconds),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
            // Switch camera
            IconButton(
                onClick = onSwitchCamera,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.2f)
                )
            ) {
                Icon(Icons.Default.FlipCameraAndroid, "Changer camera", tint = Color.White)
            }
        }

        // Bottom controls
        CallControls(
            callInfo = callInfo,
            isVideo = true,
            onEndCall = onEndCall,
            onToggleMute = onToggleMute,
            onToggleCamera = onToggleCamera,
            onToggleSpeaker = onToggleSpeaker,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun AudioCallContent(
    callInfo: CallManager.CallInfo,
    durationSeconds: Int,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A1628), Color(0xFF0D2137), Color(0xFF0A1628))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 80.dp)
        ) {
            // Status text
            Text(
                text = when (callInfo.state) {
                    CallManager.CallState.CALLING -> "Appel en cours..."
                    CallManager.CallState.RINGING -> "Ca sonne..."
                    CallManager.CallState.CONNECTING -> "Connexion..."
                    CallManager.CallState.CONNECTED -> formatDuration(durationSeconds)
                    CallManager.CallState.RECONNECTING -> "Reconnexion..."
                    else -> ""
                },
                color = if (callInfo.state == CallManager.CallState.CONNECTED)
                    Color(0xFF00D2FF) else Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp
            )

            // Avatar + Name
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = Color(0xFF00D2FF).copy(
                        alpha = if (callInfo.state == CallManager.CallState.CONNECTED) 0.2f else pulseAlpha
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = callInfo.remoteNom.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = callInfo.remoteNom,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Controls
            CallControls(
                callInfo = callInfo,
                isVideo = false,
                onEndCall = onEndCall,
                onToggleMute = onToggleMute,
                onToggleCamera = null,
                onToggleSpeaker = onToggleSpeaker
            )
        }
    }
}

@Composable
private fun CallControls(
    callInfo: CallManager.CallInfo,
    isVideo: Boolean,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleCamera: (() -> Unit)?,
    onToggleSpeaker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 40.dp, start = 20.dp, end = 20.dp)
    ) {
        // Mute
        ControlButton(
            icon = if (callInfo.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            label = if (callInfo.isMuted) "Unmute" else "Mute",
            isActive = callInfo.isMuted,
            onClick = onToggleMute
        )

        // Speaker
        ControlButton(
            icon = if (callInfo.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
            label = "HP",
            isActive = callInfo.isSpeakerOn,
            onClick = onToggleSpeaker
        )

        // Camera (video only)
        if (isVideo && onToggleCamera != null) {
            ControlButton(
                icon = if (callInfo.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                label = "Camera",
                isActive = callInfo.isCameraOff,
                onClick = onToggleCamera
            )
        }

        // Hangup
        FloatingActionButton(
            onClick = onEndCall,
            containerColor = Color(0xFFFF3B30),
            contentColor = Color.White,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                Icons.Default.CallEnd,
                contentDescription = "Raccrocher",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (isActive) Color.White.copy(alpha = 0.3f)
                else Color.White.copy(alpha = 0.1f)
            )
        ) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

@Composable
fun WebRTCVideoView(
    videoTrack: VideoTrack,
    eglContext: EglBase.Context,
    modifier: Modifier = Modifier,
    mirror: Boolean = false
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                init(eglContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setMirror(mirror)
                setEnableHardwareScaler(true)
                videoTrack.addSink(this)
            }
        },
        modifier = modifier,
        onRelease = { renderer ->
            try {
                videoTrack.removeSink(renderer)
                renderer.release()
            } catch (e: Exception) { /* ignore */ }
        }
    )
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
