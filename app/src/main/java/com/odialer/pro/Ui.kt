package com.odialer.pro

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.provider.Settings
import android.telecom.TelecomManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ───────────────────────── Root ─────────────────────────

@Composable
fun Root(initial: String) {
    val ctx = LocalContext.current
    var tab by remember(initial) { mutableIntStateOf(if (initial.isNotBlank()) 1 else 0) }
    var refresh by remember { mutableIntStateOf(0) }

    val perms = remember {
        buildList {
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.ANSWER_PHONE_CALLS)
            add(Manifest.permission.PROCESS_OUTGOING_CALLS)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refresh++ }
    LaunchedEffect(Unit) {
        val missing = perms.filter { !has(ctx, it) }
        if (missing.isNotEmpty()) launcher.launch(missing.toTypedArray())
    }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val items = listOf(
        "Recents" to Icons.Filled.History,
        "Keypad" to Icons.Filled.Dialpad,
        "Contacts" to Icons.Filled.Contacts,
        "Records" to Icons.Filled.Mic,
        "Settings" to Icons.Filled.Settings
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                items.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, label) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            indicatorColor = Green,
                            selectedTextColor = Green
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> RecentsScreen(refresh)
                1 -> KeypadScreen(initial)
                2 -> ContactsScreen(refresh)
                3 -> RecordsScreen(refresh)
                else -> SettingsScreen(refresh)
            }
        }
    }
}

@Composable
fun Title(text: String) {
    Text(
        text, fontSize = 34.sp, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 12.dp)
    )
}

@Composable
fun Empty(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
fun Avatar(text: String, color: Color = Green) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text.firstOrNull { it.isLetter() }?.uppercase() ?: "#",
            color = color, fontWeight = FontWeight.SemiBold, fontSize = 18.sp
        )
    }
}

// ───────────────────────── Keypad ─────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadScreen(initial: String) {
    val ctx = LocalContext.current
    var num by remember(initial) { mutableStateOf(initial) }
    val keys = listOf(
        "1" to "", "2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL", "6" to "MNO",
        "7" to "PQRS", "8" to "TUV", "9" to "WXYZ", "*" to "", "0" to "+", "#" to ""
    )
    val name = remember(num) { if (num.length >= 3) contactName(ctx, num) else null }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            num.ifEmpty { " " }, fontSize = 36.sp, fontWeight = FontWeight.Light,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(name ?: " ", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        keys.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { (k, s) ->
                    Column(
                        Modifier.size(76.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { num += k },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(k, fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
                        if (s.isNotEmpty()) {
                            Text(s, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.size(76.dp))
            Box(
                Modifier.size(76.dp).clip(CircleShape).background(Green)
                    .clickable { placeCall(ctx, num) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Call, "Call", tint = Color.White, modifier = Modifier.size(32.dp)) }
            Box(
                Modifier.size(76.dp).clip(CircleShape).combinedClickable(
                    onClick = { if (num.isNotEmpty()) num = num.dropLast(1) },
                    onLongClick = { num = "" }
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Backspace, "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ───────────────────────── Recents ─────────────────────────

@Composable
fun RecentsScreen(refresh: Int) {
    val ctx = LocalContext.current
    val calls = remember(refresh) { loadCalls(ctx) }
    Column(Modifier.fillMaxSize()) {
        Title("Recents")
        if (calls.isEmpty()) {
            Empty("No calls yet, or call log permission is off. Allow it in Settings.")
        } else {
            LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                items(calls) { c ->
                    val missed = c.type == CallLog.Calls.MISSED_TYPE ||
                        c.type == CallLog.Calls.REJECTED_TYPE
                    val icon = when (c.type) {
                        CallLog.Calls.INCOMING_TYPE -> Icons.Filled.CallReceived
                        CallLog.Calls.OUTGOING_TYPE -> Icons.Filled.CallMade
                        else -> Icons.Filled.CallMissed
                    }
                    val title = c.name ?: c.number.ifEmpty { "Unknown" }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .clickable { placeCall(ctx, c.number) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(title, if (missed) Red else Green)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                title, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (missed) Red else MaterialTheme.colorScheme.onBackground
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    icon, null, modifier = Modifier.size(14.dp),
                                    tint = if (missed) Red else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    fmtDate(c.date), fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Filled.Call, "Call", tint = Green)
                    }
                }
            }
        }
    }
}

// ───────────────────────── Contacts ─────────────────────────

@Composable
fun ContactsScreen(refresh: Int) {
    val ctx = LocalContext.current
    val all = remember(refresh) { loadContacts(ctx) }
    var q by remember { mutableStateOf("") }
    val shown = remember(q, all) {
        if (q.isBlank()) all
        else all.filter { it.name.contains(q, true) || it.number.filter { ch -> ch.isDigit() }.contains(q.filter { ch -> ch.isDigit() }.ifEmpty { "\u0000" }) }
    }
    Column(Modifier.fillMaxSize()) {
        Title("Contacts")
        OutlinedTextField(
            value = q, onValueChange = { q = it },
            placeholder = { Text("Search name or number") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true, shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
        )
        if (shown.isEmpty()) {
            Empty("No contacts to show. Check the contacts permission in Settings.")
        } else {
            LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                items(shown) { c ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .clickable { placeCall(ctx, c.number) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(c.name)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                            Text(c.number, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── Records ─────────────────────────

object Player {
    private var mp: MediaPlayer? = null
    private val _playing = MutableStateFlow<String?>(null)
    val playing: StateFlow<String?> = _playing

    fun toggle(path: String) {
        if (_playing.value == path) { stop(); return }
        stop()
        try {
            val p = MediaPlayer()
            p.setDataSource(path)
            p.prepare()
            p.setOnCompletionListener { Player.stop() }
            p.start()
            mp = p
            _playing.value = path
        } catch (e: Exception) {
            stop()
        }
    }

    fun stop() {
        try { mp?.release() } catch (e: Exception) { }
        mp = null
        _playing.value = null
    }
}

@Composable
fun RecordsScreen(refresh: Int) {
    val ctx = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    val recs = remember(refresh, version) { listRecs(ctx) }
    val playing by Player.playing.collectAsState()
    DisposableEffect(Unit) { onDispose { Player.stop() } }

    Column(Modifier.fillMaxSize()) {
        Title("Recordings")
        if (recs.isEmpty()) {
            Empty("No recordings yet. Make a call with auto-record on and it will appear here.")
        } else {
            LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                items(recs, key = { it.file.name }) { r ->
                    val name = remember(r.number) { contactName(ctx, r.number) }
                    val dur = remember(r.file.path) {
                        try {
                            val m = MediaMetadataRetriever()
                            m.setDataSource(r.file.path)
                            val d = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                            m.release(); fmtDur(d)
                        } catch (e: Exception) { "–" }
                    }
                    val isPlaying = playing == r.file.path
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(46.dp).clip(CircleShape)
                                .background(if (isPlaying) Green else Green.copy(alpha = 0.16f))
                                .clickable { Player.toggle(r.file.path) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play",
                                tint = if (isPlaying) Color.White else Green
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                name ?: r.number.replace("unknown", "Unknown number"),
                                fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${if (r.incoming) "Incoming" else "Outgoing"}  ${fmtDate(r.time)}  $dur",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fp", r.file)
                            val send = Intent(Intent.ACTION_SEND).setType("audio/mp4")
                                .putExtra(Intent.EXTRA_STREAM, uri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            ctx.startActivity(Intent.createChooser(send, "Share recording"))
                        }) { Icon(Icons.Filled.Share, "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = {
                            val ok = exportToMusic(ctx, r.file)
                            Toast.makeText(
                                ctx, if (ok) "Saved to Music/CallRecordings" else "Export failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }) { Icon(Icons.Filled.SaveAlt, "Save to Music", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = {
                            if (playing == r.file.path) Player.stop()
                            r.file.delete(); version++
                        }) { Icon(Icons.Filled.Delete, "Delete", tint = Red) }
                    }
                }
            }
        }
    }
}

// ───────────────────────── Settings ─────────────────────────

fun isDefaultDialer(c: Context): Boolean =
    try { c.getSystemService(TelecomManager::class.java).defaultDialerPackage == c.packageName }
    catch (e: Exception) { false }

fun accessibilityOn(c: Context): Boolean {
    val s = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return s?.contains(c.packageName) == true
}

fun requestDefaultDialer(a: Activity) {
    if (Build.VERSION.SDK_INT >= 29) {
        val rm = a.getSystemService(RoleManager::class.java)
        if (rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
            a.startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER), 1001)
        }
    } else {
        a.startActivity(
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, a.packageName)
        )
    }
}

@Composable
fun SettingsScreen(refresh: Int) {
    val ctx = LocalContext.current
    var auto by remember { mutableStateOf(Prefs.autoRecord(ctx)) }
    var boost by remember { mutableStateOf(Prefs.speakerBoost(ctx)) }
    val isDef = remember(refresh) { isDefaultDialer(ctx) }
    val accOn = remember(refresh) { accessibilityOn(ctx) }
    val micOk = remember(refresh) { has(ctx, Manifest.permission.RECORD_AUDIO) }
    val src = Prefs.source(ctx).coerceIn(0, RecorderService.SOURCE_NAMES.size - 1)

    Column(Modifier.fillMaxSize().verticalScrollCompat()) {
        Title("Settings")
        SectionCard {
            SwitchRow("Auto record calls", "Starts silently when a call connects. No beep, no announcement.", auto) {
                auto = it; Prefs.setAutoRecord(ctx, it)
            }
            SwitchRow(
                "Speaker boost", "Turns on the speaker during calls so the mic can also pick up the other person. Use this if the other side is missing in recordings.",
                boost
            ) { boost = it; Prefs.setSpeakerBoost(ctx, it) }
        }
        Spacer(Modifier.height(14.dp))
        SectionCard {
            ActionRow(
                "Microphone & phone permissions",
                if (micOk) "Granted" else "Missing: recording cannot work",
                "Open"
            ) { openAppInfo(ctx) }
            ActionRow(
                "Accessibility helper",
                if (accOn) "On" else "Off: turn on “oDialer Pro” under Installed apps",
                "Open"
            ) { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            ActionRow(
                "Default phone app",
                if (isDef) "oDialer Pro is your phone app" else "Optional: gives you the full in-call screen",
                if (isDef) "Done" else "Set"
            ) { (ctx as? Activity)?.let { requestDefaultDialer(it) } }
            ActionRow(
                "Battery",
                "Set to “No restrictions” and allow auto-start so recording is never killed",
                "Open"
            ) {
                try { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                catch (e: Exception) { openAppInfo(ctx) }
            }
        }
        Spacer(Modifier.height(14.dp))
        SectionCard {
            Column(Modifier.padding(16.dp)) {
                Text("Audio source in use", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    RecorderService.SOURCE_NAMES[src], fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "The app tries the best source first and automatically switches if a source records silence. Android 10+ blocks the call stream on most phones, so both voices usually need Speaker boost.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Recording laws differ by country. Make sure you are allowed to record the people you call.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

fun openAppInfo(c: Context) {
    c.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${c.packageName}"))
    )
}

@Composable
fun Modifier.verticalScrollCompat(): Modifier =
    this.then(Modifier.verticalScroll(rememberScrollState()))

@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface),
        content = content
    )
}

@Composable
fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Green, checkedThumbColor = Color.White)
        )
    }
}

@Composable
fun ActionRow(title: String, sub: String, button: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        TextButton(onClick = onClick) { Text(button, color = Green, fontWeight = FontWeight.SemiBold) }
    }
}
