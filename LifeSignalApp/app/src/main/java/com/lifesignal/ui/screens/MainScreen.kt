package com.lifesignal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifesignal.ui.navigation.Screen
import com.lifesignal.ui.screens.home.HomeScreen
import com.lifesignal.ui.screens.network.NetworkScreen
import com.lifesignal.ui.screens.profile.ProfileScreen

// Define three bottom navigation tabs
sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home)
    object Network : BottomNavItem(Screen.Network.route, "Network", Icons.Default.People)
    object Profile : BottomNavItem(Screen.Profile.route, "Profile", Icons.Default.Person)
}

@Composable
fun MainScreen(
    onSignOut: () -> Unit,
    onAddFriendClick: () -> Unit = {},
    onShareProfileClick: () -> Unit = {},
    onFriendClick: (String, String, Boolean, String) -> Unit = { _, _, _, _ -> },
    onGroupClick: (String) -> Unit = {},
    onAddGroupClick: () -> Unit = {},
    onAddContactClick: () -> Unit = {},
    onPrivacySecurityClick: () -> Unit = {},
    onNotificationPreferencesClick: () -> Unit = {},
    onCheckInSettingsClick: () -> Unit = {},
    onCheckInHistoryClick: () -> Unit = {}
) {
    val bottomNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            val items = listOf(
                BottomNavItem.Home,
                BottomNavItem.Network,
                BottomNavItem.Profile
            )
            val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            // Replicate the bottom navigation bar style from React
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .shadow(elevation = 24.dp, spotColor = Color(0x0F191C21), ambientColor = Color(0x0F191C21))
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    items.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, fontWeight = FontWeight.Bold) },
                            selected = selected,
                            onClick = {
                                bottomNavController.navigate(item.route) {
                                    popUpTo(bottomNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        composable(BottomNavItem.Home.route) { HomeScreen(onCheckInHistoryClick = onCheckInHistoryClick) }
        composable(BottomNavItem.Network.route) { 
            NetworkScreen(
                onAddFriendClick = onAddFriendClick, 
                onShareProfileClick = onShareProfileClick, 
                onFriendClick = onFriendClick, 
                onGroupClick = onGroupClick, 
                onAddGroupClick = onAddGroupClick
            ) 
        }
        composable(BottomNavItem.Profile.route) {  
                ProfileScreen(
                    onSignOut = onSignOut,
                    onShareProfileClick = onShareProfileClick,
                    onAddContactClick = onAddContactClick,
                    onPrivacySecurityClick = onPrivacySecurityClick,
                    onNotificationPreferencesClick = onNotificationPreferencesClick,
                    onCheckInSettingsClick = onCheckInSettingsClick
                ) 
            }
        }
    }
}
