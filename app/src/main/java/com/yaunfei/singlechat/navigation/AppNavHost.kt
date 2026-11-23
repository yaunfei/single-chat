package com.yaunfei.singlechat.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yaunfei.singlechat.feature.chat.ChatScreen
import com.yaunfei.singlechat.feature.conversations.ConversationListScreen
import com.yaunfei.singlechat.feature.settings.SettingsScreen

object Routes {
    const val Conversations = "conversations"
    const val Chat = "chat/{conversationId}"
    const val Settings = "settings"

    fun chat(conversationId: String): String = "chat/${Uri.encode(conversationId)}"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.Conversations,
    ) {
        composable(Routes.Conversations) {
            ConversationListScreen(
                onOpenChat = { conversationId ->
                    navController.navigate(Routes.chat(conversationId))
                },
                onOpenSettings = { navController.navigate(Routes.Settings) },
            )
        }
        composable(
            route = Routes.Chat,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
        ) {
            ChatScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Settings) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
