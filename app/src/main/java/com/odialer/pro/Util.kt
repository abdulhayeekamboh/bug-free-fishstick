package com.odialer.pro

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

fun has(c: Context, p: String) =
    ContextCompat.checkSelfPermission(c, p) == PackageManager.PERMISSION_GRANTED

fun recDir(c: Context): File =
    File(c.getExternalFilesDir(null) ?: c.filesDir, "CallRecordings").apply { mkdirs() }

fun contactName(c: Context, number: String): String? {
    if (number.isBlank() || !has(c, Manifest.permission.READ_CONTACTS)) return null
    return try {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)
        )
        c.contentResolver.query(
            uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null
        )?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (e: Exception) {
        null
    }
}

fun fmtDur(ms: Long): String {
    val s = ms / 1000
    return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}

fun fmtDate(ms: Long): String =
    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(ms)

fun placeCall(c: Context, number: String) {
    if (number.isBlank()) return
    Prefs.setPending(c, number)
    val uri = Uri.fromParts("tel", number, null)
    if (has(c, Manifest.permission.CALL_PHONE)) {
        c.getSystemService(TelecomManager::class.java).placeCall(uri, Bundle())
    } else {
        c.startActivity(Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

// ---- call log ----
data class CallItem(val number: String, val name: String?, val type: Int, val date: Long, val duration: Long)

fun loadCalls(c: Context): List<CallItem> {
    if (!has(c, Manifest.permission.READ_CALL_LOG)) return emptyList()
    val out = mutableListOf<CallItem>()
    try {
        c.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE,
                CallLog.Calls.DATE, CallLog.Calls.DURATION
            ),
            null, null, "${CallLog.Calls.DATE} DESC"
        )?.use { cur ->
            var n = 0
            while (cur.moveToNext() && n < 300) {
                out += CallItem(
                    cur.getString(0) ?: "", cur.getString(1),
                    cur.getInt(2), cur.getLong(3), cur.getLong(4)
                )
                n++
            }
        }
    } catch (e: Exception) {
    }
    return out
}

// ---- contacts ----
data class ContactItem(val name: String, val number: String)

fun loadContacts(c: Context): List<ContactItem> {
    if (!has(c, Manifest.permission.READ_CONTACTS)) return emptyList()
    val out = LinkedHashMap<String, ContactItem>()
    try {
        c.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
        )?.use { cur ->
            while (cur.moveToNext()) {
                val name = cur.getString(0) ?: continue
                val num = cur.getString(1) ?: continue
                out.putIfAbsent(name + num.filter { it.isDigit() }, ContactItem(name, num))
            }
        }
    } catch (e: Exception) {
    }
    return out.values.toList()
}

// ---- recordings ----
data class Rec(val file: File, val number: String, val incoming: Boolean, val time: Long)

fun listRecs(c: Context): List<Rec> =
    (recDir(c).listFiles { f -> f.extension == "m4a" } ?: emptyArray())
        .sortedByDescending { it.lastModified() }
        .map { parseRec(it) }

fun parseRec(f: File): Rec {
    val p = f.nameWithoutExtension.split("_", limit = 4)
    val t = try {
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).parse(p[0] + "_" + p[1])!!.time
    } catch (e: Exception) {
        f.lastModified()
    }
    return Rec(f, p.getOrElse(3) { "unknown" }, p.getOrElse(2) { "in" } == "in", t)
}

fun exportToMusic(c: Context, f: File): Boolean {
    return try {
        val v = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, f.name)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/CallRecordings")
            }
        }
        val uri = c.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, v)
            ?: return false
        c.contentResolver.openOutputStream(uri)?.use { o ->
            f.inputStream().use { it.copyTo(o) }
        }
        true
    } catch (e: Exception) {
        false
    }
}
