package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClientEntity
import com.example.ui.theme.RoseError
import com.example.ui.viewmodel.QuotationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
    viewModel: QuotationViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onSelectClientForQuotation: (Long) -> Unit
) {
    val clients by viewModel.allClients.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddClientDialog by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<ClientEntity?>(null) }

    val filtered = remember(clients, searchQuery) {
        clients.filter {
            searchQuery.isBlank() ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.companyName.contains(searchQuery, ignoreCase = true) ||
                    it.mobile.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clients CRM", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("clients_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingClient = null
                    showAddClientDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_client_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Client")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search clients by name, company, or phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("search_clients_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Total Clients: ${filtered.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filtered, key = { it.id }) { client ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(client.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    if (client.companyName.isNotBlank()) {
                                        Text(client.companyName, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { onSelectClientForQuotation(client.id) },
                                        modifier = Modifier.size(32.dp).testTag("client_create_quote_${client.id}")
                                    ) {
                                        Icon(Icons.Outlined.PostAdd, contentDescription = "New Quote", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = {
                                            editingClient = client
                                            showAddClientDialog = true
                                        },
                                        modifier = Modifier.size(32.dp).testTag("client_edit_${client.id}")
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteClient(client) },
                                        modifier = Modifier.size(32.dp).testTag("client_delete_${client.id}")
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = RoseError)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (client.mobile.isNotBlank() || client.email.isNotBlank()) {
                                Text("Ph: ${client.mobile}  •  ${client.email}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (client.gstin.isNotBlank()) {
                                Text("GSTIN: ${client.gstin}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            }
                            if (client.address.isNotBlank()) {
                                Text("Address: ${client.address}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddClientDialog) {
        NewClientDialog(
            onDismiss = { showAddClientDialog = false },
            onSave = { c ->
                val clientToSave = if (editingClient != null) {
                    c.copy(id = editingClient!!.id)
                } else {
                    c
                }
                viewModel.saveClient(clientToSave)
                showAddClientDialog = false
            }
        )
    }
}
