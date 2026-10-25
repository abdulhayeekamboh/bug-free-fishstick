package com.odialer.pro

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import kotlinx.coroutines.flow.MutableStateFlow

object CallManager {
    val calls = MutableStateFlow<List<Call>>(emptyList())
    val tick = MutableStateFlow(0)
    val audio = MutableStateFlow<CallAudioState?>(null)
    var service: InCallService? = null

    private val cb = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            tick.value++
            if (state == Call.STATE_ACTIVE) ensureRecording(call)
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            tick.value++
        }
    }

    private fun ensureRecording(call: Call) {
        val s = service ?: return
        if (!Prefs.autoRecord(s) || RecorderService.running) return
        val number = call.details.handle?.schemeSpecificPart
        val incoming = if (android.os.Build.VERSION.SDK_INT >= 29)
            call.details.callDirection == Call.Details.DIRECTION_INCOMING else false
        RecorderService.start(s, number, incoming)
    }

    fun add(c: Call) {
        c.registerCallback(cb)
        calls.value = calls.value + c
        tick.value++
    }

    fun remove(c: Call) {
        c.unregisterCallback(cb)
        calls.value = calls.value - c
        tick.value++
    }
}
