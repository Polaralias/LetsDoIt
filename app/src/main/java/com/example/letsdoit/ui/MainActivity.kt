package com.example.letsdoit.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.letsdoit.ui.navigation.AppNavHost
import com.example.letsdoit.ui.theme.LetsDoItTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LetsDoItTheme {
                AppNavHost()
            }
        }
    }
}
