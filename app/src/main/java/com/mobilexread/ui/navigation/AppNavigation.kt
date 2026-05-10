package com.mobilexread.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mobilexread.ui.screens.ArticleDetailScreen
import com.mobilexread.ui.screens.ArticleListScreen
import com.mobilexread.ui.screens.ModelSetupScreen
import com.mobilexread.ui.screens.SettingsScreen

object Routes {
    const val ARTICLE_LIST = "article_list"
    const val ARTICLE_DETAIL = "article_detail/{articleId}"
    const val SETTINGS = "settings"
    const val MODEL_SETUP = "model_setup"

    fun articleDetail(id: Long) = "article_detail/$id"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Routes.ARTICLE_LIST
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ARTICLE_LIST) {
            ArticleListScreen(
                onArticleClick = { id -> navController.navigate(Routes.articleDetail(id)) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            route = Routes.ARTICLE_DETAIL,
            arguments = listOf(navArgument("articleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getLong("articleId") ?: return@composable
            ArticleDetailScreen(
                articleId = articleId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onSetupModel = { navController.navigate(Routes.MODEL_SETUP) }
            )
        }
        composable(Routes.MODEL_SETUP) {
            ModelSetupScreen(
                onBack = { navController.popBackStack() },
                onModelImported = { navController.popBackStack() }
            )
        }
    }
}
