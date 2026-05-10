package com.mobilexread.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.mobilexread.ui.navigation.AppNavigation
import com.mobilexread.ui.theme.MobileXReadTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MobileXReadTheme {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}
