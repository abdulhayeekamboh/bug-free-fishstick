package com.odialer.pro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private var initial by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initial = intent?.data?.schemeSpecificPart ?: ""
        setContent { ODialerTheme { Root(initial) } }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        initial = intent?.data?.schemeSpecificPart ?: ""
    }
}
