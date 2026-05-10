package com.mobilexread.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.mobilexread.ui.navigation.AppNavigation
import com.mobilexread.ui.navigation.Routes
import com.mobilexread.ui.theme.MobileXReadTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startArticleId = intent?.getLongExtra(EXTRA_ARTICLE_ID, -1L)
            ?.takeIf { it >= 0 }

        setContent {
            MobileXReadTheme {
                val navController = rememberNavController()
                val startDest = if (startArticleId != null)
                    Routes.articleDetail(startArticleId)
                else
                    Routes.ARTICLE_LIST

                AppNavigation(
                    navController = navController,
                    startDestination = startDest
                )
            }
        }
    }

    companion object {
        const val EXTRA_ARTICLE_ID = "article_id"
    }
}
