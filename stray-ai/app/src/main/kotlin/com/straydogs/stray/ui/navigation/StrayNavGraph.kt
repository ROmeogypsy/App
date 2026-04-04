package com.straydogs.stray.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.straydogs.stray.ui.screens.chat.ChatScreen
import com.straydogs.stray.ui.screens.conversations.ConversationsScreen
import com.straydogs.stray.ui.screens.models.ModelScreen
import com.straydogs.stray.ui.screens.settings.SettingsScreen

sealed class Route(val path: String) {
    data object Conversations : Route("conversations")
    data object Chat : Route("chat/{conversationId}") {
        fun withId(id: String) = "chat/$id"
    }
    data object Models : Route("models")
    data object Settings : Route("settings")
}

@Composable
fun StrayNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Conversations.path
    ) {
        composable(Route.Conversations.path) {
            ConversationsScreen(
                onOpenChat = { conversationId ->
                    navController.navigate(Route.Chat.withId(conversationId))
                },
                onOpenModels = {
                    navController.navigate(Route.Models.path)
                },
                onOpenSettings = {
                    navController.navigate(Route.Settings.path)
                }
            )
        }

        composable(
            route = Route.Chat.path,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            ChatScreen(
                conversationId = conversationId,
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Route.Settings.path) }
            )
        }

        composable(Route.Models.path) {
            ModelScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.Settings.path) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
