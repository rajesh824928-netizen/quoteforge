package com.example.ui.screens

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
import com.example.data.model.ProjectEntity
import com.example.domain.engine.CurrencyFormatter
import com.example.ui.theme.RoseError
import com.example.ui.viewmodel.QuotationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: QuotationViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val projects by viewModel.allProjects.collectAsState()
    val clients by viewModel.allClients.collectAsState()
    val clientMap = remember(clients) { clients.associateBy { it.id } }

    var searchQuery by remember { mutableStateOf("") }
    var showAddProjectDialog by remember { mutableStateOf(false) }

    val filtered = remember(projects, searchQuery) {
        projects.filter {
            searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.projectType.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects & Sites", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("projects_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddProjectDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_project_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Project")
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
                placeholder = { Text("Search projects...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Total Projects: ${filtered.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filtered, key = { it.id }) { project ->
                    val client = clientMap[project.clientId]

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
                                    Text(project.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Client: ${client?.name ?: "Unknown"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }

                                IconButton(
                                    onClick = { viewModel.deleteProject(project) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = RoseError)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text(project.projectType, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                if (project.area.isNotBlank()) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                        Text(project.area, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                if (project.budget > 0) {
                                    Text(
                                        "Budget: " + CurrencyFormatter.formatInr(project.budget, false),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (project.address.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Site: ${project.address}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddProjectDialog) {
        val firstClient = clients.firstOrNull()
        NewProjectDialog(
            clientId = firstClient?.id ?: 1,
            onDismiss = { showAddProjectDialog = false },
            onSave = { p ->
                viewModel.saveProject(p)
                showAddProjectDialog = false
            }
        )
    }
}
