package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkspaceEntity
import com.example.data.model.WorkspaceMemberEntity
import com.example.data.model.WorkspacePermissions
import com.example.ui.viewmodel.QuotationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceManagementScreen(
    viewModel: QuotationViewModel,
    onNavigateBack: () -> Unit
) {
    val allWorkspaces by viewModel.allWorkspaces.collectAsState()
    val currentWorkspaceId by viewModel.currentWorkspaceId.collectAsState()
    val currentWorkspace by viewModel.currentWorkspace.collectAsState()
    val members by viewModel.workspaceMembers.collectAsState()
    val userRole by viewModel.currentUserRole.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var memberToEditRole by remember { mutableStateOf<WorkspaceMemberEntity?>(null) }
    var showPermissionsMatrix by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workspace & Team", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.AddBusiness, contentDescription = "New Workspace")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Workspace Switcher Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ACTIVE WORKSPACE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    currentWorkspace?.name ?: "My Workspace",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${currentWorkspace?.companyName ?: "Architecture Studio"} • ${currentWorkspace?.businessType ?: "Architecture"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AssistChip(
                                onClick = { /* Role Info */ },
                                label = { Text(userRole, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Switch Workspace:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allWorkspaces.forEach { ws ->
                                val isSelected = ws.id == currentWorkspaceId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.switchWorkspace(ws.id) },
                                    label = { Text(ws.name, maxLines = 1) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Another Workspace")
                        }
                    }
                }
            }

            // Team Members Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TEAM MEMBERS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("${members.size} active members in this workspace", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { showInviteDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("invite_member_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invite", fontSize = 12.sp)
                    }
                }
            }

            items(members) { member ->
                MemberCard(
                    member = member,
                    onEditRole = { memberToEditRole = member },
                    onRemove = { viewModel.removeMember(member) }
                )
            }

            // Role Permissions Matrix Toggle
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPermissionsMatrix = !showPermissionsMatrix },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Roles & Permissions Matrix", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("View access rights for Owner, Designer, Accountant, Viewer", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(
                            if (showPermissionsMatrix) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    if (showPermissionsMatrix) {
                        HorizontalDivider()
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RoleMatrixRow("Owner", "Full access to all clients, projects, profit margins, costs, billing & settings")
                            RoleMatrixRow("Admin", "Full access to quotations, rates, templates & team. Billing excluded.")
                            RoleMatrixRow("Manager", "Create/edit quotations, view profit margins, view costs, manage rates")
                            RoleMatrixRow("Designer", "Create & edit quotations and specifications. Cost & profit hidden.")
                            RoleMatrixRow("Accountant", "View quotations, invoices & cost metrics. Cannot edit architectural drawings.")
                            RoleMatrixRow("Viewer", "Read-only access to proposals. Cannot edit, view costs, or export PDF.")
                        }
                    }
                }
            }
        }
    }

    // Dialog: Create Workspace
    if (showCreateDialog) {
        CreateWorkspaceDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, company, type ->
                viewModel.createWorkspace(name, company, type)
                showCreateDialog = false
            }
        )
    }

    // Dialog: Invite Member
    if (showInviteDialog) {
        InviteMemberDialog(
            onDismiss = { showInviteDialog = false },
            onInvite = { name, email, role ->
                viewModel.addWorkspaceMember(name, email, role)
                showInviteDialog = false
            }
        )
    }

    // Dialog: Change Role
    memberToEditRole?.let { member ->
        ChangeRoleDialog(
            member = member,
            onDismiss = { memberToEditRole = null },
            onSelectRole = { newRole ->
                viewModel.updateMemberRole(member, newRole)
                memberToEditRole = null
            }
        )
    }
}

@Composable
fun MemberCard(
    member: WorkspaceMemberEntity,
    onEditRole: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        member.userName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(member.userName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(member.userEmail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onEditRole,
                    shape = RoundedCornerShape(6.dp),
                    color = when (member.role.uppercase()) {
                        "OWNER" -> Color(0xFFE0E7FF)
                        "DESIGNER" -> Color(0xFFFEF3C7)
                        "MANAGER" -> Color(0xFFDCFCE7)
                        "ACCOUNTANT" -> Color(0xFFF3E8FF)
                        else -> Color(0xFFF1F5F9)
                    }
                ) {
                    Text(
                        member.role,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (member.role != "OWNER") {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RoleMatrixRow(role: String, desc: String) {
    Column {
        Text(role, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkspaceDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, company: String, type: String) -> Unit
) {
    var wsName by remember { mutableStateOf("") }
    var compName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Architecture") }
    val types = listOf("Architecture", "Interior Design", "Construction", "Modular Furniture", "Contractor", "Freelance")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Workspace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Each workspace maintains complete data isolation, team members, and brand settings.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = wsName,
                    onValueChange = { wsName = it },
                    label = { Text("Workspace Name (e.g. Knot Living)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = compName,
                    onValueChange = { compName = it },
                    label = { Text("Legal Company Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Business Type:", style = MaterialTheme.typography.labelSmall)
                Column {
                    types.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { t ->
                                FilterChip(
                                    selected = selectedType == t,
                                    onClick = { selectedType = t },
                                    label = { Text(t, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(wsName.ifBlank { "New Workspace" }, compName.ifBlank { wsName }, selectedType) },
                enabled = wsName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun InviteMemberDialog(
    onDismiss: () -> Unit,
    onInvite: (name: String, email: String, role: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("DESIGNER") }
    val roles = listOf("ADMIN", "MANAGER", "DESIGNER", "ACCOUNTANT", "VIEWER")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Team Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Work Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Assign Role:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    roles.take(3).forEach { r ->
                        FilterChip(
                            selected = selectedRole == r,
                            onClick = { selectedRole = r },
                            label = { Text(r, fontSize = 11.sp) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    roles.drop(3).forEach { r ->
                        FilterChip(
                            selected = selectedRole == r,
                            onClick = { selectedRole = r },
                            label = { Text(r, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onInvite(name, email, selectedRole) },
                enabled = name.isNotBlank() && email.isNotBlank()
            ) {
                Text("Send Invite")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ChangeRoleDialog(
    member: WorkspaceMemberEntity,
    onDismiss: () -> Unit,
    onSelectRole: (String) -> Unit
) {
    val roles = listOf("ADMIN", "MANAGER", "DESIGNER", "ACCOUNTANT", "VIEWER")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Role for ${member.userName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current role: ${member.role}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                roles.forEach { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRole(r) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = member.role.equals(r, ignoreCase = true),
                            onClick = { onSelectRole(r) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(r, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
