package com.odialer.pro

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService

class CallService : InCallService() {

    override fun onCreate() {
        super.onCreate()
        CallManager.service = this
    }

    override fun onCallAdded(call: Call) {
        CallManager.add(call)
        startActivity(
            Intent(this, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    override fun onCallRemoved(call: Call) {
        CallManager.remove(call)
        if (CallManager.calls.value.isEmpty()) {
            stopService(Intent(this, RecorderService::class.java))
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        CallManager.audio.value = audioState
    }

    override fun onDestroy() {
        CallManager.service = null
        super.onDestroy()
    }
}
