package com.lifesignal.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
fun CheckInSettingsScreen(
    onBack: () -> Unit,
    viewModel: CheckInSettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check-in Settings", fontWeight = FontWeight.Black, fontSize = 22.sp) },
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
                    "Configure your daily check-in deadline and the grace period before an emergency alert is triggered.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

                // Time Picker setup
                var showTimePicker by remember { mutableStateOf(false) }
                val timePickerState = rememberTimePickerState(
                    initialHour = settings.checkInHour,
                    initialMinute = settings.checkInMinute,
                    is24Hour = false
                )

                if (showTimePicker) {
                    AlertDialog(
                        onDismissRequest = { showTimePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.setCheckInTime(timePickerState.hour, timePickerState.minute)
                                showTimePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        },
                        text = {
                            TimePicker(state = timePickerState)
                        }
                    )
                }

                Text("Daily Check-in Deadline", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "You must complete your check-in before this time each day.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true }
                ) {
                    val amPm = if (settings.checkInHour >= 12) "PM" else "AM"
                    val displayHour = if (settings.checkInHour % 12 == 0) 12 else settings.checkInHour % 12
                    val displayMinute = settings.checkInMinute.toString().padStart(2, '0')
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$amPm $displayHour:$displayMinute", fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Change", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Grace Period", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Buffer time after a missed check-in before notifying emergency contacts.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        SelectionRow(
                            title = "30 Minutes",
                            isSelected = settings.gracePeriodMinutes == 30,
                            onClick = { viewModel.setGracePeriodMinutes(30) }
                        )
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
                        SelectionRow(
                            title = "1 Hour",
                            isSelected = settings.gracePeriodMinutes == 60,
                            onClick = { viewModel.setGracePeriodMinutes(60) }
                        )
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
                        SelectionRow(
                            title = "2 Hours",
                            isSelected = settings.gracePeriodMinutes == 120,
                            onClick = { viewModel.setGracePeriodMinutes(120) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
