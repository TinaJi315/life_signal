package com.lifesignal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lifesignal.ui.screens.MainScreen
import com.lifesignal.ui.screens.auth.LoginScreen
import com.lifesignal.ui.screens.network.AddFriendScreen
import com.lifesignal.ui.screens.network.ShareProfileScreen
import com.lifesignal.ui.screens.network.FriendDetailScreen
import com.lifesignal.ui.screens.network.GroupDetailScreen
import com.lifesignal.ui.screens.network.ReminderSentScreen
import com.lifesignal.ui.screens.network.AddMemberScreen
import com.lifesignal.ui.screens.network.FriendProfileScreen
import com.lifesignal.ui.screens.network.NewGroupScreen
import com.lifesignal.ui.screens.profile.ProfileScreen
import com.lifesignal.ui.screens.profile.AddContactScreen
import com.lifesignal.ui.screens.profile.PrivacySecurityScreen
import com.lifesignal.ui.screens.profile.NotificationPreferencesScreen
import com.lifesignal.ui.screens.home.CheckInHistoryScreen

@Composable
fun LifeSignalAppNavHost() {
    val navController = rememberNavController()

    // 完整的 App 路由，起点是 LoginScreen
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // 1. 登录与注册
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. 主页面骨架 (包含底部导航的 Home, Network, Profile)
        composable(Screen.Home.route) {
            // MainScreen 内含自己的底部 NavBar 切换逻辑
            MainScreen(
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onAddFriendClick = {
                    navController.navigate(Screen.AddFriend.route)
                },
                onShareProfileClick = {
                    navController.navigate("share_profile")
                },
                onFriendClick = { friendId, friendName, isSafe, time ->
                    navController.navigate("friend_detail/$friendId/$friendName/$isSafe/$time")
                },
                onGroupClick = {
                    navController.navigate("group_detail")
                },
                onAddGroupClick = {
                    navController.navigate("new_group")
                },
                onAddContactClick = {
                    navController.navigate("add_contact")
                },
                onPrivacySecurityClick = {
                    navController.navigate("privacy_security")
                },
                onNotificationPreferencesClick = {
                    navController.navigate("notification_preferences")
                },
                onCheckInHistoryClick = {
                    navController.navigate("checkin_history")
                }
            )
        }
        
        // 3. 全屏子页面
        composable(Screen.AddFriend.route) {
            AddFriendScreen(onBack = { navController.popBackStack() })
        }
        composable("share_profile") {
            ShareProfileScreen(onBack = { navController.popBackStack() })
        }
        composable("friend_detail/{friendId}/{name}/{safe}/{time}") { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val safe = backStackEntry.arguments?.getString("safe")?.toBoolean() ?: true
            val time = backStackEntry.arguments?.getString("time") ?: ""
            FriendDetailScreen(
                friendId = friendId,
                friendName = name,
                isSafe = safe,
                lastTime = time,
                onBack = { navController.popBackStack() },
                onViewProfile = { navController.navigate("friend_profile/$friendId/$name") }
            )
        }
        composable("friend_profile/{friendId}/{name}") { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            FriendProfileScreen(
                friendId = friendId,
                name = name,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            )
        }
        composable("new_group") {
            NewGroupScreen(onBack = { navController.popBackStack() })
        }
        composable("group_detail") {
            GroupDetailScreen(
                onBack = { navController.popBackStack() },
                onRemindAll = { navController.navigate("reminder_sent") },
                onAddMember = { navController.navigate("add_member") }
            )
        }
        composable("reminder_sent") {
            ReminderSentScreen(onBack = { navController.popBackStack() })
        }
        composable("add_member") {
            AddMemberScreen(onBack = { navController.popBackStack() })
        }
        composable("add_contact") {
            AddContactScreen(onBack = { navController.popBackStack() })
        }
        composable("privacy_security") {
            PrivacySecurityScreen(onBack = { navController.popBackStack() })
        }
        composable("notification_preferences") {
            NotificationPreferencesScreen(onBack = { navController.popBackStack() })
        }
        composable("checkin_history") {
            CheckInHistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
