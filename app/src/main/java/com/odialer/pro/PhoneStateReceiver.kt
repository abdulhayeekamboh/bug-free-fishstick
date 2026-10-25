package com.odialer.pro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class PhoneStateReceiver : BroadcastReceiver() {

    companion object {
        var last: String = TelephonyManager.EXTRA_STATE_IDLE
        var ringNumber: String? = null
        var incoming = false
    }

    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            Prefs.setPending(c, i.getStringExtra(Intent.EXTRA_PHONE_NUMBER))
            return
        }
        if (i.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val st = i.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val num = i.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        if (st == last) {
            if (!num.isNullOrEmpty() && ringNumber.isNullOrEmpty()) ringNumber = num
            return
        }
        val prev = last
        last = st

        when (st) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                incoming = true
                ringNumber = num
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (prev != TelephonyManager.EXTRA_STATE_RINGING) incoming = false
                if (!Prefs.autoRecord(c)) return
                val number = if (incoming) ringNumber else Prefs.pending(c)
                RecorderService.start(c, number, incoming)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                c.stopService(Intent(c, RecorderService::class.java))
                incoming = false
                ringNumber = null
                Prefs.setPending(c, null)
            }
        }
    }
}
