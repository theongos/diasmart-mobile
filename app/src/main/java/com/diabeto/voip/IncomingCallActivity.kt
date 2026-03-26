package com.diabeto.voip

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diabeto.ui.theme.DiabetoTheme
import com.diabeto.ui.theme.Primary

/**
 * Full-screen incoming call activity.
 * Shows on lock screen with accept/reject buttons.
 * Similar to WhatsApp/native phone incoming call UI.
 */
class IncomingCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(KeyguardManager::class.java)
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        val callerNom = intent.getStringExtra("callerNom") ?: "Appel entrant"
        val callType = intent.getStringExtra("callType") ?: "video"
        val isVideo = callType == "video"

        setContent {
            DiabetoTheme {
                IncomingCallScreen(
                    callerNom = callerNom,
                    isVideo = isVideo,
                    onAccept = {
                        CallManagerProvider.callManager?.acceptCall()
                        finish()
                    },
                    onReject = {
                        CallManagerProvider.callManager?.rejectCall()
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun IncomingCallScreen(
    callerNom: String,
    isVideo: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    // Pulsating animation for avatar
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1628),
                        Color(0xFF0D2137),
                        Color(0xFF0A1628)
                    )
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
            // Top: Call type label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isVideo) "Appel video entrant" else "Appel audio entrant",
                    color = Primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DiaSmart",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }

            // Center: Avatar + Name
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Pulsating avatar circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.scale(pulseScale)
                ) {
                    // Outer glow ring
                    Surface(
                        modifier = Modifier.size(140.dp),
                        shape = CircleShape,
                        color = Primary.copy(alpha = 0.1f)
                    ) {}
                    // Inner circle with initial
                    Surface(
                        modifier = Modifier.size(110.dp),
                        shape = CircleShape,
                        color = Primary.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = callerNom.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = callerNom,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isVideo) "Appel video..." else "Appel audio...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
            }

            // Bottom: Accept / Reject buttons
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Reject
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onReject,
                        containerColor = Color(0xFFFF3B30),
                        contentColor = Color.White,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "Refuser",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Refuser",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }

                // Accept
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onAccept,
                        containerColor = Color(0xFF34C759),
                        contentColor = Color.White,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = "Accepter",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Accepter",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
