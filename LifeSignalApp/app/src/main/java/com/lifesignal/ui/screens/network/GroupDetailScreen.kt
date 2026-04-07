package com.lifesignal.ui.screens.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class GroupMember(
    val name: String,
    val isSafe: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    onBack: () -> Unit,
    onRemindAll: () -> Unit,
    onAddMember: () -> Unit,
    viewModel: GroupDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val authRepo = remember { com.lifesignal.data.repository.AuthRepository() }
    val currentUid = authRepo.currentUid

    LaunchedEffect(groupId, currentUid) {
        if (currentUid != null) {
            viewModel.loadGroupData(groupId, currentUid)
        }
    }

    val group by viewModel.group.collectAsStateWithLifecycle()
    val memberStatuses by viewModel.memberStatuses.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
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
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (group == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Group not found")
            }
        } else {
            val validGroup = group!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
            ) {
                // Group title
                Text(
                    text = validGroup.name,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Member Avatars row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Three overlapping circles (avatars)
                    Box(modifier = Modifier.width(80.dp).height(40.dp)) {
                        Box(modifier = Modifier.size(40.dp).background(Color.Gray, CircleShape))
                        Box(modifier = Modifier.size(40.dp).offset(x = 24.dp).background(Color.DarkGray, CircleShape))
                        Box(
                            modifier = Modifier.size(40.dp).offset(x = 48.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.width(36.dp))
                    Text("${validGroup.memberCount} Members", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // REMIND ALL MEMBERS big button
                Button(
                    onClick = {
                        if (currentUid != null) {
                            viewModel.remindAllGroupCheckIn(validGroup.id, currentUid)
                            onRemindAll()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2D8C))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "REMIND ALL MEMBERS",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Member Status section
                Text("Member Status", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    memberStatuses.forEach { status ->
                        MemberStatusRow(GroupMember(status.name, status.status == "safe"))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Add Member Button
                Surface(
                    onClick = onAddMember,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Add Member", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun MemberStatusRow(member: GroupMember) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dotColor = if (member.isSafe) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                Box(
                    modifier = Modifier.size(14.dp).background(dotColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(member.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            val statusText = if (member.isSafe) "SAFE" else "OVERDUE"
            val statusColor = if (member.isSafe) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
            Text(statusText, fontSize = 14.sp, fontWeight = FontWeight.Black, color = statusColor, letterSpacing = 1.sp)
        }
    }
}
