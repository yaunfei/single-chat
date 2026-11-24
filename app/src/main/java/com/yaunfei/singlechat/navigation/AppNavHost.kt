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
import com.yaunfei.singlechat.feature.contacts.ContactsScreen
import com.yaunfei.singlechat.feature.conversations.ConversationListScreen
import com.yaunfei.singlechat.feature.profile.ProfileScreen

object Routes {
    const val Conversations = "conversations"
    const val Contacts = "contacts"
    const val Profile = "profile"
    const val Chat = "chat/{conversationId}"

    fun chat(conversationId: String): String = "chat/${Uri.encode(conversationId)}"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    fun selectTab(index: Int) {
        val route = when (index) {
            0 -> Routes.Conversations
            1 -> Routes.Contacts
            else -> Routes.Profile
        }
        navController.navigate(route) {
            popUpTo(Routes.Conversations) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openChat(conversationId: String) {
        navController.navigate(Routes.chat(conversationId))
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Conversations,
    ) {
        composable(Routes.Conversations) {
            ConversationListScreen(
                onOpenChat = ::openChat,
                onTabSelect = ::selectTab,
            )
        }
        composable(Routes.Contacts) {
            ContactsScreen(
                onOpenChat = ::openChat,
                onTabSelect = ::selectTab,
            )
        }
        composable(Routes.Profile) {
            ProfileScreen(
                onTabSelect = ::selectTab,
            )
        }
        composable(
            route = Routes.Chat,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
        ) {
            ChatScreen(onBack = { navController.popBackStack() })
        }
    }
}
