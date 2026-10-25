package com.odialer.pro

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecorderService : Service() {

    companion object {
        @Volatile var running = false
        val SOURCE_NAMES = listOf(
            "Call stream (best quality)", "Voice recognition", "Voice communication", "Microphone"
        )

        fun start(c: Context, number: String?, incoming: Boolean) {
            val i = Intent(c, RecorderService::class.java)
                .putExtra("n", number).putExtra("in", incoming)
            ContextCompat.startForegroundService(c, i)
        }
    }

    private val sources = intArrayOf(
        MediaRecorder.AudioSource.VOICE_CALL,
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        MediaRecorder.AudioSource.MIC
    )

    private val handler = Handler(Looper.getMainLooper())
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var idx = 0
    private var peak = 0
    private var elapsed = 0
    private var number: String? = null
    private var incoming = false
    private var boosted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startFg()
        if (running) return START_NOT_STICKY
        if (!has(this, Manifest.permission.RECORD_AUDIO)) {
            stopSelf(); return START_NOT_STICKY
        }
        running = true
        number = intent?.getStringExtra("n")
        incoming = intent?.getBooleanExtra("in", false) ?: false

        if (Prefs.speakerBoost(this)) {
            try {
                getSystemService(AudioManager::class.java).isSpeakerphoneOn = true
                boosted = true
            } catch (e: Exception) {
            }
        }
        begin(Prefs.source(this).coerceIn(0, sources.size - 1))
        return START_NOT_STICKY
    }

    private fun startFg() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("rec", "Call service", NotificationManager.IMPORTANCE_MIN)
        )
        val n: Notification = NotificationCompat.Builder(this, "rec")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("oDialer Pro")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(
            this, 42, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )
    }

    private fun begin(start: Int) {
        var i = start
        while (i < sources.size) {
            idx = i
            if (tryStart(sources[i])) return
            i++
        }
        stopSelf()
    }

    private fun newFile(): File {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val num = (number ?: "").filter { it.isDigit() || it == '+' }.ifEmpty { "unknown" }
        return File(recDir(this), "${ts}_${if (incoming) "in" else "out"}_$num.m4a")
    }

    private fun tryStart(src: Int): Boolean {
        releaseRecorder(true)
        return try {
            val f = newFile()
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()
            r.setAudioSource(src)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(44100)
            r.setAudioEncodingBitRate(96000)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            file = f
            peak = 0
            elapsed = 0
            handler.removeCallbacks(monitor)
            handler.postDelayed(monitor, 500)
            true
        } catch (e: Exception) {
            releaseRecorder(true)
            false
        }
    }

    // If a source records pure silence for 5 s, switch to the next one automatically.
    private val monitor = object : Runnable {
        override fun run() {
            val r = recorder ?: return
            val amp = try { r.maxAmplitude } catch (e: Exception) { 0 }
            if (amp > peak) peak = amp
            elapsed += 500
            if (peak > 0) {
                Prefs.setSource(this@RecorderService, idx)
                return
            }
            if (elapsed >= 5000) {
                if (idx < sources.size - 1) begin(idx + 1)
                return
            }
            handler.postDelayed(this, 500)
        }
    }

    private fun releaseRecorder(delete: Boolean) {
        val r = recorder
        var ok = true
        if (r != null) {
            try { r.stop() } catch (e: Exception) { ok = false }
            try { r.release() } catch (e: Exception) { }
        }
        recorder = null
        val f = file
        if (f != null && (delete || !ok || f.length() < 1500)) f.delete()
        file = null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        releaseRecorder(false)
        if (boosted) {
            try { getSystemService(AudioManager::class.java).isSpeakerphoneOn = false } catch (e: Exception) { }
        }
        running = false
        super.onDestroy()
    }
}
