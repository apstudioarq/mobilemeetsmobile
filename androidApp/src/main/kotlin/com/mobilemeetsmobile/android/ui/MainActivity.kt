package com.mobilemeetsmobile.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mobilemeetsmobile.android.ui.navigation.AppNavigation
import com.mobilemeetsmobile.android.ui.theme.DarkBackground
import com.mobilemeetsmobile.android.ui.theme.MobileMeetsMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileMeetsMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground,
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
