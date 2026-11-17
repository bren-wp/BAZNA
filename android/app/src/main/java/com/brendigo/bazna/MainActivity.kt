package com.brendigo.bazna

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.brendigo.bazna.ui.SidrenaApp
import com.brendigo.bazna.ui.SidrenaSplashScreen
import com.brendigo.bazna.ui.SidrenaTheme

class MainActivity : ComponentActivity() {
    private val model: SidrenaViewModel by lazy { ViewModelProvider(this)[SidrenaViewModel::class.java] }

    override fun onResume() {
        super.onResume()
        // Foreground-only refresh; never schedule hidden recurring network work.
        model.refreshIfStale()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SidrenaTheme {
                var showingSplash by rememberSaveable { mutableStateOf(true) }
                if (showingSplash) {
                    SidrenaSplashScreen(onFinished = { showingSplash = false })
                } else {
                    SidrenaApp(model = model)
                }
            }
        }
    }
}
