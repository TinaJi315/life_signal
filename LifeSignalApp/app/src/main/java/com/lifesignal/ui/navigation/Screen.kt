package com.lifesignal.ui.navigation

sealed class Screen(val route: String) {
    // Auth
    object Login : Screen("login")
    
    // Main Tabs
    object Home : Screen("home")
    object Network : Screen("network")
    object Profile : Screen("profile")
    
    // Sub-screens
    object AddContact : Screen("add_contact")
    object AddFriend : Screen("add_friend")
    object FriendDetail : Screen("friend_detail/{friendId}") {
        fun createRoute(friendId: String) = "friend_detail/$friendId"
    }
}
