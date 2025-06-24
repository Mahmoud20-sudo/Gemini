package com.me.gemini.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.me.gemini.presentation.screen.GeminiChatScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "chat"
    ) {
        composable("chat") {
            GeminiChatScreen()
        }
    }
}