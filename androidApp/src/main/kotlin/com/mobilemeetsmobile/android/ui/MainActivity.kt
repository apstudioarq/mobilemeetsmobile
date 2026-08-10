package com.mobilemeetsmobile.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mobilemeetsmobile.android.ui.navigation.AppNavigation
import com.mobilemeetsmobile.android.ui.theme.DarkBackground
import com.mobilemeetsmobile.android.ui.theme.MobileMeetsMobileTheme
import com.mobilemeetsmobile.android.ui.splash.SplashScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileMeetsMobileTheme {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(900)
                    showSplash = false
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground,
                ) {
                    if (showSplash) {
                        SplashScreen()
                    } else {
                        AppNavigation()
                    }
                }
            }
        }
    }
}
