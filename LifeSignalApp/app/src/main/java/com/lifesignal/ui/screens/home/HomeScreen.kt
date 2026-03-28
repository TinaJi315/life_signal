package com.lifesignal.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeScreen(
    onCheckInHistoryClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val isCheckedIn by viewModel.isCheckedIn.collectAsState()
    val isCheckingIn by viewModel.isCheckingIn.collectAsState()
    val lastCheckIn by viewModel.lastCheckIn.collectAsState()
    val nextCheckInText by viewModel.nextCheckInText.collectAsState()
    val remainingTime by viewModel.remainingTimeText.collectAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.doCheckIn()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                if (isCheckedIn) {
                    CheckedInView(
                        lastTime = lastCheckIn?.timestamp?.let {
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it)
                        } ?: "Just now",
                        lastLocation = lastCheckIn?.location?.takeIf { it.isNotBlank() && it != "Unknown" } ?: "Locating...",
                        nextCheckInText = nextCheckInText,
                        onDebugReset = { viewModel.debugResetState() },
                        onHistoryClick = onCheckInHistoryClick
                    )
                } else {
                    NotCheckedInView(
                        isCheckingIn = isCheckingIn,
                        remainingHours = remainingTime.first,
                        remainingMinutes = remainingTime.second,
                        onCheckInClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.SEND_SMS,
                                    Manifest.permission.READ_PHONE_STATE
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotCheckedInView(
    isCheckingIn: Boolean,
    remainingHours: String,
    remainingMinutes: String,
    onCheckInClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "You haven't checked in today",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 38.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Start)
        )
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .height(8.dp)
                .width(96.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.tertiary)
                .align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Big Safe Button
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .blur(50.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), CircleShape)
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .size(280.dp)
                    .clickable(enabled = !isCheckingIn, onClick = onCheckInClick)
                    .border(12.dp, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), CircleShape),
                shadowElevation = 24.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCheckingIn) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(64.dp))
                    } else {
                        Text(
                            text = "SAFE",
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Dynamic countdown
        Text(
            text = "TIME REMAINING TO CHECK IN:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(remainingHours, fontSize = 48.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text("h", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp, end = 8.dp))
            Text(remainingMinutes, fontSize = 48.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text("m", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
        }
    }
}

@Composable
fun CheckedInView(
    lastTime: String,
    lastLocation: String,
    nextCheckInText: String,
    onDebugReset: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Checked-in",
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            text = "Your safety status has been updated.",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Checked In Circle (debug reset tap)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(32.dp))
                .border(4.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(32.dp))
                .clickable { onDebugReset() }
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Checked-in Today",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Next check-in — dynamic
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Next Check-in", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(nextCheckInText, fontSize = 20.sp, fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Last Record — clickable, goes to history
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onHistoryClick() }
        ) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.secondary.copy(0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.History, contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Last Record", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Today $lastTime", fontSize = 20.sp, fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface)
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "View All",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
