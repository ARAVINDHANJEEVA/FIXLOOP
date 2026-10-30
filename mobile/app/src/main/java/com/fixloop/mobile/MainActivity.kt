package com.fixloop.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.fixloop.mobile.state.SentinelAppState
import com.fixloop.mobile.ui.navigation.SentinelNavHost
import com.fixloop.mobile.ui.theme.BgPaper
import com.fixloop.mobile.ui.theme.FixLoopTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FixLoopTheme {
                val appState = remember { SentinelAppState() }
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgPaper
                ) {
                    SentinelNavHost(
                        navController = navController,
                        appState = appState
                    )
                }
            }
        }
    }
}
