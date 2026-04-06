package com.lifesignal.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifesignal.ui.theme.Warning
import com.lifesignal.ui.theme.WarningContainer
import com.lifesignal.ui.theme.OnWarning
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
    val userStatus by viewModel.userStatus.collectAsState()

    // 构建权限列表（包含 POST_NOTIFICATIONS）
    val permissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

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
                if (isCheckedIn && userStatus == "safe") {
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
                        userStatus = userStatus,
                        onCheckInClick = {
                            locationPermissionLauncher.launch(permissions)
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
    userStatus: String,
    onCheckInClick: () -> Unit
) {
    // 根据状态动态计算颜色
    val buttonColor by animateColorAsState(
        targetValue = when (userStatus) {
            "warning" -> Warning
            "emergency" -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.secondary
        },
        label = "buttonColor"
    )
    val buttonBorderColor by animateColorAsState(
        targetValue = when (userStatus) {
            "warning" -> Warning.copy(alpha = 0.3f)
            "emergency" -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        },
        label = "buttonBorderColor"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = when (userStatus) {
                "warning" -> "Check-in overdue"
                "emergency" -> "Emergency triggered"
                else -> "You haven't checked in today"
            },
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 38.sp,
            color = when (userStatus) {
                "warning" -> Warning
                "emergency" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.align(Alignment.Start)
        )
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .height(8.dp)
                .width(96.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when (userStatus) {
                        "warning" -> Warning
                        "emergency" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                )
                .align(Alignment.Start)
        )

        // 状态横幅
        AnimatedVisibility(visible = userStatus == "warning" || userStatus == "emergency") {
            StatusBanner(userStatus = userStatus)
        }

        Spacer(modifier = Modifier.height(if (userStatus != "safe") 24.dp else 48.dp))

        // Big Safe Button — 颜色随状态变化
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .blur(50.dp)
                    .background(buttonColor.copy(alpha = 0.2f), CircleShape)
            )
            Surface(
                shape = CircleShape,
                color = buttonColor,
                modifier = Modifier
                    .size(280.dp)
                    .clickable(enabled = !isCheckingIn, onClick = onCheckInClick)
                    .border(12.dp, buttonBorderColor, CircleShape),
                shadowElevation = 24.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCheckingIn) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(64.dp))
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (userStatus == "emergency") {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                text = when (userStatus) {
                                    "emergency" -> "RESOLVE"
                                    "warning" -> "SAFE"
                                    else -> "SAFE"
                                },
                                color = Color.White,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 4.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Dynamic countdown
        Text(
            text = when (userStatus) {
                "warning" -> "EMERGENCY COUNTDOWN:"
                "emergency" -> "EMERGENCY ACTIVE"
                else -> "TIME REMAINING TO CHECK IN:"
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            color = when (userStatus) {
                "warning" -> Warning
                "emergency" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        if (userStatus != "emergency") {
            Row(verticalAlignment = Alignment.Bottom) {
                val countdownColor by animateColorAsState(
                    targetValue = when (userStatus) {
                        "warning" -> Warning
                        else -> MaterialTheme.colorScheme.primary
                    },
                    label = "countdownColor"
                )
                Text(remainingHours, fontSize = 48.sp, fontWeight = FontWeight.Black, color = countdownColor)
                Text("h", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = countdownColor,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp, end = 8.dp))
                Text(remainingMinutes, fontSize = 48.sp, fontWeight = FontWeight.Black, color = countdownColor)
                Text("m", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = countdownColor,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
            }
        }
    }
}

/**
 * 状态横幅 — 警告 / 紧急
 */
@Composable
fun StatusBanner(userStatus: String) {
    val backgroundColor = when (userStatus) {
        "warning" -> WarningContainer
        "emergency" -> MaterialTheme.colorScheme.errorContainer
        else -> Color.Transparent
    }
    val textColor = when (userStatus) {
        "warning" -> OnWarning
        "emergency" -> MaterialTheme.colorScheme.onErrorContainer
        else -> Color.Transparent
    }
    val icon = when (userStatus) {
        "warning" -> "⚠️"
        "emergency" -> "\uD83D\uDEA8"
        else -> ""
    }
    val message = when (userStatus) {
        "warning" -> "您已错过签到时间。请立即签到以取消紧急倒计时。"
        "emergency" -> "紧急联系人已收到警报通知。请签到以解除紧急状态。"
        else -> ""
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp
            )
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
