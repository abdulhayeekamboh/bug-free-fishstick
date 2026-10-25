package com.odialer.pro

import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        setContent { ODialerTheme { InCallScreen { finish() } } }
    }
}

@Composable
fun InCallScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val calls by CallManager.calls.collectAsState()
    val tick by CallManager.tick.collectAsState()
    val audio by CallManager.audio.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); now = System.currentTimeMillis() }
    }
    val call = calls.firstOrNull()
    LaunchedEffect(call) {
        if (call == null) { delay(700); onClose() }
    }

    val bg = Brush.verticalGradient(listOf(Color(0xFF0E4A33), Color(0xFF0A0B0D)))
    Box(Modifier.fillMaxSize().background(bg)) {
        if (call == null) {
            Text("Call ended", color = Color.White, fontSize = 20.sp, modifier = Modifier.align(Alignment.Center))
            return@Box
        }
        @Suppress("UNUSED_VARIABLE") val t = tick
        val state = call.state
        val number = call.details.handle?.schemeSpecificPart ?: ""
        val name = remember(number) { contactName(ctx, number) }
        val title = name ?: number.ifEmpty { "Unknown" }
        val muted = audio?.isMuted == true
        val speaker = audio?.route == CallAudioState.ROUTE_SPEAKER
        val ringing = state == Call.STATE_RINGING
        val held = state == Call.STATE_HOLDING
        val connect = call.details.connectTimeMillis
        val status = when {
            ringing -> "Incoming call"
            held -> "On hold"
            state == Call.STATE_DIALING || state == Call.STATE_CONNECTING -> "Calling…"
            state == Call.STATE_ACTIVE && connect > 0 -> fmtDur(maxOf(0, now - connect))
            state == Call.STATE_DISCONNECTED -> "Call ended"
            else -> ""
        }

        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))
            Box(
                Modifier.size(110.dp).clip(CircleShape).background(Color(0x26FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    title.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?",
                    color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Light
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            if (name != null) Text(number, color = Color(0xB3FFFFFF), fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Text(status, color = Color(0xCCFFFFFF), fontSize = 17.sp)
            Spacer(Modifier.height(14.dp))
            if (RecorderService.running) {
                Row(
                    Modifier.clip(CircleShape).background(Color(0x33F2514A))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Red))
                    Spacer(Modifier.width(8.dp))
                    Text("Recording", color = Color.White, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.weight(1f))

            if (!ringing) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RoundBtn(if (muted) Icons.Filled.MicOff else Icons.Filled.Mic, "Mute", muted) {
                        CallManager.service?.setMuted(!muted)
                    }
                    RoundBtn(Icons.Filled.VolumeUp, "Speaker", speaker) {
                        CallManager.service?.setAudioRoute(
                            if (speaker) CallAudioState.ROUTE_EARPIECE else CallAudioState.ROUTE_SPEAKER
                        )
                    }
                    RoundBtn(Icons.Filled.Pause, "Hold", held) {
                        if (held) call.unhold() else call.hold()
                    }
                }
                Spacer(Modifier.height(48.dp))
                BigBtn(Icons.Filled.CallEnd, Red) { call.disconnect() }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BigBtn(Icons.Filled.CallEnd, Red) { call.reject(false, null) }
                    BigBtn(Icons.Filled.Call, Green) { call.answer(VideoProfile.STATE_AUDIO_ONLY) }
                }
            }
        }
    }
}

@Composable
private fun RoundBtn(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(66.dp).clip(CircleShape)
                .background(if (active) Color.White else Color(0x33FFFFFF))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = if (active) Color.Black else Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color(0xCCFFFFFF), fontSize = 12.sp)
    }
}

@Composable
private fun BigBtn(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(76.dp).clip(CircleShape).background(color).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(34.dp))
    }
}
