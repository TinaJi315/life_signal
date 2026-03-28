package com.lifesignal.ui.screens.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendDetailScreen(
    friendId: String,
    friendName: String,
    isSafe: Boolean,
    lastTime: String,
    onBack: () -> Unit,
    onViewProfile: () -> Unit,
    viewModel: NetworkViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Real-time friend data from Firestore
    val friend by viewModel.getFriendById(friendId).collectAsState(initial = null)

    var showMenu by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }

    // --- Dialogs ---
    
    // Remove confirmation
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Friend", fontWeight = FontWeight.Black) },
            text = { Text("Remove ${friend?.name ?: friendName} from your network?") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    viewModel.removeFriend(friendId) { onBack() }
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Remove", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel") } }
        )
    }

    // Block confirmation
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block ${friend?.name ?: friendName}", fontWeight = FontWeight.Black) },
            text = { Text("This will remove them from your friends and prevent them from seeing your status. You can undo this in settings.") },
            confirmButton = {
                TextButton(onClick = {
                    showBlockDialog = false
                    viewModel.blockUser(friendId) { onBack() }
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Block", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") } }
        )
    }

    // Report Dialog
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report User", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text("Why are you reporting this user?")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("Reason (e.g. harassment, spam)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.reportUser(friendId, reportReason) {
                            scope.launch { snackbarHostState.showSnackbar("Report submitted") }
                        }
                        showReportDialog = false
                    },
                    enabled = reportReason.isNotBlank()
                ) {
                    Text("Submit Report", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Send Reminder", fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    showMenu = false
                                    viewModel.sendReminder(friendId) {
                                        scope.launch { snackbarHostState.showSnackbar("Reminder sent to ${friend?.name ?: friendName}") }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Block", fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    showMenu = false
                                    showBlockDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Report",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showReportDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Avatar with Status Dot
            Box(modifier = Modifier.size(120.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(26.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val dotColor = if (isSafe) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 0.dp, y = (-4).dp)
                        .size(24.dp)
                        .background(dotColor, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = friend?.name ?: friendName,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            val statusText = if (isSafe) "CURRENTLY SAFE" else "NO CHECK-IN"
            Text(
                text = statusText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Last check-in card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "LAST CHECK-IN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val displayTime = friend?.lastUpdated?.takeIf { it.isNotBlank() } ?: lastTime.ifBlank { "—" }
                        Text(displayTime, fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        Text("Today", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Check-in history card — real data from Firestore
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "CHECK-IN HISTORY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val rate = friend?.checkinSuccessRate?.takeIf { it.isNotBlank() } ?: "100% SUCCESS RATE"
                        val since = friend?.memberSince?.takeIf { it.isNotBlank() } ?: "Since joining"
                        Text(rate, fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        Text(since, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onViewProfile,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("View Profile", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Button(
                    onClick = { showRemoveDialog = true },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
