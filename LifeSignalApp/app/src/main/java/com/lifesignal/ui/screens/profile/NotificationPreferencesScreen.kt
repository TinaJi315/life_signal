package com.lifesignal.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesScreen(
    onBack: () -> Unit,
    viewModel: NotificationPreferencesViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Preferences", fontWeight = FontWeight.Black, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Configure when and how you receive alerts.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        NotificationToggleRow(
                            title = "Check-in Alerts",
                            subtitle = "Notify when a friend misses their check-in",
                            checked = settings.missedCheckIn,
                            onCheckedChange = { viewModel.setCheckInAlerts(it) }
                        )

                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                        NotificationToggleRow(
                            title = "Group Reminders",
                            subtitle = "Receive group-wide check-in reminders",
                            checked = settings.groupReminder,
                            onCheckedChange = { viewModel.setGroupReminders(it) }
                        )

                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                        NotificationToggleRow(
                            title = "New Friend Alerts",
                            subtitle = "Notify when someone adds you as a friend",
                            checked = settings.newFriend,
                            onCheckedChange = { viewModel.setNewFriendAlerts(it) }
                        )

                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                        NotificationToggleRow(
                            title = "System Alerts",
                            subtitle = "Important updates and announcements",
                            checked = settings.systemAlerts,
                            onCheckedChange = { viewModel.setSystemAlerts(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
