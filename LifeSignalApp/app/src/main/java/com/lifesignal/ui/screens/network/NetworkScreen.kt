package com.lifesignal.ui.screens.network

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.items
import com.lifesignal.data.model.Group

@Composable
fun NetworkScreen(
    onAddFriendClick: () -> Unit,
    onShareProfileClick: () -> Unit,
    onFriendClick: (friendId: String, name: String, isSafe: Boolean, time: String) -> Unit,
    onGroupClick: () -> Unit,
    onAddGroupClick: () -> Unit,
    viewModel: NetworkViewModel = viewModel()
) {
    val friends by viewModel.friends.collectAsState()
    val groups by viewModel.groups.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            // Title
            item {
                Text(
                    text = "Your Network",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Friends Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Friends", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onAddFriendClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Friend", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Friends List
            if (friends.isEmpty()) {
                item {
                    Text("No friends yet. Start adding!", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                items(friends) { friend ->
                    val isSafe = friend.status == "safe"
                    // Extract initials, default to "U" if name is empty
                    val initials = if (friend.name.isNotBlank()) friend.name.trim().split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase() else "U"
                    FriendItem(
                        name = friend.name,
                        status = if (isSafe) "Safe" else "Overdue",
                        time = "Recently",
                        isSafe = isSafe,
                        initials = initials,
                        onClick = { onFriendClick(friend.id, friend.name, isSafe, friend.lastUpdated.ifBlank { "Recently" }) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Groups Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Groups", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onAddGroupClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Group", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Group Cards
            if (groups.isEmpty()) {
                item {
                    Text("No groups yet. Create one!", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                }
            } else {
                items(groups) { group ->
                    GroupCard(
                        group = group, 
                        onGroupClick = onGroupClick, 
                        onRemindAll = { viewModel.remindAllGroupCheckIn(group) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun GroupCard(group: Group, onGroupClick: () -> Unit, onRemindAll: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable { onGroupClick() }
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(group.name, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text("${group.memberCount} Members", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
                // Placeholder overlapping avatars UI for demo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).background(Color.DarkGray, CircleShape), contentAlignment = Alignment.Center) { Text("M1", color = Color.White, fontSize = 10.sp) }
                    Box(modifier = Modifier.size(36.dp).offset(x = (-8).dp).background(Color.Gray, CircleShape).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape), contentAlignment = Alignment.Center) { Text("M2", color = Color.White, fontSize = 10.sp) }
                    Box(modifier = Modifier.size(36.dp).offset(x = (-16).dp).background(MaterialTheme.colorScheme.primary, CircleShape).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape), contentAlignment = Alignment.Center) {
                        Text("+${maxOf(0, group.memberCount - 2)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onRemindAll,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), // Dark blue
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("REMIND ALL TO CHECK-IN", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun FriendItem(name: String, status: String, time: String, isSafe: Boolean, initials: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar with Status Dot
                Box(modifier = Modifier.size(56.dp)) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
                    }
                    val dotColor = if (isSafe) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(16.dp)
                            .background(dotColor, CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(name, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = if (isSafe) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                        Text(status, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("•  $time", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
