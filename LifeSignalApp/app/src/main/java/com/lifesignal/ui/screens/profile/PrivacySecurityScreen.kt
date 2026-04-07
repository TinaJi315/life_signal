package com.lifesignal.ui.screens.profile

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifesignal.data.repository.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val authRepo = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    // Password reset dialog state
    var showResetDialog by remember { mutableStateOf(false) }
    var resetSent by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Security", fontWeight = FontWeight.Black, fontSize = 22.sp) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Manage your data, privacy, and account security.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // -- Permissions --
            SectionCard {
                SectionHeader(icon = Icons.Default.LocationOn, label = "Permissions")

                Divider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(vertical = 0.dp)
                )

                SettingActionRow(
                    title = "Location Access",
                    subtitle = "Required for check-in location tagging"
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Manage", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -- Account Security --
            SectionCard {
                SectionHeader(icon = Icons.Default.Lock, label = "Account Security")

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                SettingActionRow(
                    title = "Change Password",
                    subtitle = "Send a reset link to your email"
                ) {
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reset", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -- About --
            SectionCard {
                SectionHeader(icon = Icons.Default.Info, label = "About")

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                InfoRow(label = "App Version", value = "1.0.0")
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                InfoRow(label = "Privacy Policy", value = "lifesignal.app/privacy")
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                InfoRow(label = "Terms of Service", value = "lifesignal.app/terms")
            }
        }
    }

    // Password reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Password", fontWeight = FontWeight.Black) },
            text = {
                if (resetSent) {
                    Text("✅ Password reset email has been sent. Please check your inbox.")
                } else {
                    Text("We'll send a password reset link to your registered email address.")
                }
            },
            confirmButton = {
                if (!resetSent) {
                    Button(
                        onClick = {
                            scope.launch {
                                val email = authRepo.currentUser?.email ?: return@launch
                                authRepo.resetPassword(email)
                                resetSent = true
                            }
                        }
                    ) {
                        Text("Send Reset Email")
                    }
                } else {
                    Button(onClick = { showResetDialog = false; resetSent = false }) {
                        Text("Done")
                    }
                }
            },
            dismissButton = {
                if (!resetSent) {
                    TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
                }
            }
        )
    }
}

// -- Shared widgets --

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp), content = content)
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}

@Composable
private fun SettingActionRow(title: String, subtitle: String, action: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
