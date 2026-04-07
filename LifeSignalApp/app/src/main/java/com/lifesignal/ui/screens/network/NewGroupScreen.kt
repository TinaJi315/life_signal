package com.lifesignal.ui.screens.network

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifesignal.data.model.Friend

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupScreen(onBack: () -> Unit, viewModel: NetworkViewModel = viewModel()) {
    var groupName by remember { mutableStateOf("") }
    
    // Fetch accepted friends as candidate members
    val friends by viewModel.friends.collectAsState()
    val selectedMembers = remember { mutableStateListOf<Friend>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Group", fontWeight = FontWeight.Black, fontSize = 24.sp) },
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
            Spacer(modifier = Modifier.height(16.dp))

            // GROUP NAME section
            Text("GROUP NAME", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                placeholder = { Text("e.g. Sunday Hiking Club", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // SELECT MEMBERS section
            Text("SELECT MEMBERS", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (friends.isEmpty()) {
                    Text("No friends available to add.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    friends.forEach { friend ->
                        val isSelected = selectedMembers.contains(friend)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (isSelected) selectedMembers.remove(friend) else selectedMembers.add(friend)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(friend.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }

                            // Radio button visualization
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(2.dp, if (isSelected) MaterialTheme.colorScheme.secondary else Color.LightGray, CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(modifier = Modifier.size(10.dp).background(Color.White, CircleShape))
                                }
                            }
                        }
                    }
                }
            }
        }

            Spacer(modifier = Modifier.weight(1f))

            // Create Group button
            Button(
                onClick = {
                    viewModel.createGroup(groupName, selectedMembers.map { it.id })
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Create Group", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
            }
        }
    }
}
