package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClientEntity
import com.example.data.model.CompanyEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.QuotationEntity
import com.example.domain.engine.CurrencyFormatter
import com.example.domain.engine.ShareHelper
import com.example.ui.theme.*
import com.example.ui.viewmodel.QuotationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: QuotationViewModel,
    onNavigateToNewQuotation: (isEstimate: Boolean) -> Unit,
    onNavigateToEditQuotation: (Long) -> Unit,
    onNavigateToPreviewQuotation: (Long) -> Unit,
    onNavigateToQuotations: () -> Unit = {},
    onNavigateToClients: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToMaterials: () -> Unit,
    onNavigateToCompanies: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onNavigateToWorkspaces: () -> Unit = {},
    onNavigateToGoogleDrive: () -> Unit = {},
    onNavigateToChangelog: () -> Unit = {}
) {
    val context = LocalContext.current
    val quotations by viewModel.allQuotations.collectAsState()
    val clients by viewModel.allClients.collectAsState()
    val projects by viewModel.allProjects.collectAsState()
    val defaultCompany by viewModel.defaultCompany.collectAsState()
    val currentWorkspace by viewModel.currentWorkspace.collectAsState()
    val allWorkspaces by viewModel.allWorkspaces.collectAsState()
    val userRole by viewModel.currentUserRole.collectAsState()
    val driveConn by viewModel.driveConnection.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val filterState by viewModel.filterState.collectAsState()

    var showWorkspaceMenu by remember { mutableStateOf(false) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }

    val clientMap = remember(clients) { clients.associateBy { it.id } }
    val projectMap = remember(projects) { projects.associateBy { it.id } }


    val filteredQuotations = remember(quotations, filterState) {
        quotations.filter { q ->
            val client = clientMap[q.clientId]
            val project = q.projectId?.let { projectMap[it] }

            val matchesSearch = filterState.searchQuery.isBlank() ||
                    q.quotationNumber.contains(filterState.searchQuery, ignoreCase = true) ||
                    (client?.name?.contains(filterState.searchQuery, ignoreCase = true) == true) ||
                    (project?.name?.contains(filterState.searchQuery, ignoreCase = true) == true)

            val matchesStatus = when (filterState.statusFilter) {
                "ALL" -> true
                else -> q.status.equals(filterState.statusFilter, ignoreCase = true)
            }

            matchesSearch && matchesStatus
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Surface(
                            onClick = { showWorkspaceMenu = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.testTag("workspace_switcher_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentWorkspace?.name ?: "Knot Architects",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = "Switch Workspace",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "${currentWorkspace?.businessType ?: "Architecture"} • $userRole",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showWorkspaceMenu,
                            onDismissRequest = { showWorkspaceMenu = false }
                        ) {
                            Text(
                                "SWITCH WORKSPACE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            allWorkspaces.forEach { ws ->
                                val isSelected = ws.id == currentWorkspace?.id
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(ws.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                            Text(ws.businessType, fontSize = 10.sp, color = Color.Gray)
                                        }
                                    },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                    onClick = {
                                        viewModel.switchWorkspace(ws.id)
                                        showWorkspaceMenu = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Manage Workspaces & Team...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Outlined.Group, contentDescription = null) },
                                onClick = {
                                    showWorkspaceMenu = false
                                    onNavigateToWorkspaces()
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToGoogleDrive,
                        modifier = Modifier.testTag("drive_status_button")
                    ) {
                        val isConnected = driveConn?.isConnected == true
                        Box {
                            Icon(
                                Icons.Outlined.CloudQueue,
                                contentDescription = "Google Drive",
                                tint = if (isConnected) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isConnected) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onNavigateToCompanies,
                        modifier = Modifier.testTag("switch_company_button")
                    ) {
                        Icon(Icons.Outlined.Business, contentDescription = "Companies")
                    }
                    IconButton(
                        onClick = {
                            val success = ShareHelper.shareApkViaQuickShare(context)
                            if (success) {
                                Toast.makeText(context, "Opening Quick Share for QuoteForge APK...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("home_quick_share_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Quick Share APK", tint = LuxuryGold)
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToNewQuotation(false) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Quotation", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("new_quotation_fab")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Hero Stats Card
            item {
                DashboardHeaderSection(
                    stats = stats,
                    onOpenAiAssistant = onOpenAiAssistant,
                    onNewEstimate = { onNavigateToNewQuotation(true) },
                    onNavigateToClients = onNavigateToClients,
                    onNavigateToMaterials = onNavigateToMaterials
                )
            }

            // Quick Share & What's New in v2.2.0 Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { showWhatsNewDialog = true }
                        .testTag("banner_whats_new"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                color = LuxuryGold,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "v2.2.0",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Quick Share APK & Live PDF Studio",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp
                                )
                                Text(
                                    "Distribute to site teams • Tap to view changelogs",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                val success = ShareHelper.shareApkViaQuickShare(context)
                                if (success) {
                                    Toast.makeText(context, "Opening Quick Share...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = LuxuryGold,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search & Filter Tabs
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = filterState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search by quote #, client, or project...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (filterState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_quotation_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filterTabs = listOf(
                            "ALL" to "All (${quotations.size})",
                            "Draft" to "Drafts (${stats.draftsCount})",
                            "Sent" to "Sent (${stats.sentCount})",
                            "Accepted" to "Accepted (${stats.acceptedCount})",
                            "Rejected" to "Rejected (${stats.rejectedCount})"
                        )
                        items(filterTabs) { (key, label) ->
                            FilterChip(
                                selected = filterState.statusFilter == key,
                                onClick = { viewModel.updateStatusFilter(key) },
                                label = { Text(label, fontSize = 13.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("filter_chip_$key")
                            )
                        }
                    }
                }
            }

            // Quotations List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Quotations (${filteredQuotations.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onNavigateToQuotations,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("View All", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Quotations List Items
            if (filteredQuotations.isEmpty()) {
                item {
                    EmptyStateCard(
                        onNewQuotation = { onNavigateToNewQuotation(false) }
                    )
                }
            } else {
                items(filteredQuotations, key = { it.id }) { quotation ->
                    val client = clientMap[quotation.clientId]
                    val project = quotation.projectId?.let { projectMap[it] }

                    QuotationCard(
                        quotation = quotation,
                        client = client,
                        project = project,
                        onCardClick = { onNavigateToPreviewQuotation(quotation.id) },
                        onEditClick = { onNavigateToEditQuotation(quotation.id) },
                        onDuplicateClick = {
                            viewModel.duplicateQuotation(quotation.id) { newId ->
                                onNavigateToEditQuotation(newId)
                            }
                        },
                        onReviseClick = {
                            viewModel.createRevision(quotation.id) { newId ->
                                onNavigateToEditQuotation(newId)
                            }
                        },
                        onDeleteClick = {
                            viewModel.deleteQuotation(quotation)
                        }
                    )
                }
            }
        }
    }

    if (showWhatsNewDialog) {
        WhatsNewDialog(
            onDismiss = { showWhatsNewDialog = false },
            onOpenFullScreenChangelog = onNavigateToChangelog
        )
    }
}

@Composable
fun DashboardHeaderSection(
    stats: com.example.ui.viewmodel.DashboardStats,
    onOpenAiAssistant: () -> Unit,
    onNewEstimate: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToMaterials: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "QUOTATION DASHBOARD",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Overview & Metrics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldSuccess.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${stats.conversionRate}% Won",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Key Stat Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill(
                    title = "Total Quotes",
                    value = "${stats.totalQuotations}",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    title = "Drafts",
                    value = "${stats.draftsCount}",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    title = "Sent",
                    value = "${stats.sentCount}",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    title = "Accepted",
                    value = "${stats.acceptedCount}",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenAiAssistant,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_assistant_button"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Assistant", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onNewEstimate,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cost_estimate_button"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cost Estimate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onNavigateToClients,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clients_crm_button"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clients", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onNavigateToMaterials,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("material_rate_library_button"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rates", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun StatPill(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun QuotationCard(
    quotation: QuotationEntity,
    client: ClientEntity?,
    project: ProjectEntity?,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onReviseClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateStr = remember(quotation.date) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(quotation.date))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onCardClick() }
            .testTag("quotation_card_${quotation.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = quotation.quotationNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (quotation.revision > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Rev.${quotation.revision}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    if (quotation.isEstimate) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AmberLight
                        ) {
                            Text(
                                text = "ESTIMATE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberDark
                            )
                        }
                    }
                    if (quotation.isInvoice) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldLight
                        ) {
                            Text(
                                text = "INVOICE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }

                StatusPill(status = quotation.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Client & Project Info
            Text(
                text = client?.name ?: "No Client Selected",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (project != null || quotation.notes.isNotBlank()) {
                Text(
                    text = project?.name ?: quotation.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp).testTag("edit_quotation_${quotation.id}")
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp).testTag("more_quotation_${quotation.id}")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View & Export PDF") },
                                onClick = {
                                    showMenu = false
                                    onCardClick()
                                },
                                leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                onClick = {
                                    showMenu = false
                                    onDuplicateClick()
                                },
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Create Revision") },
                                onClick = {
                                    showMenu = false
                                    onReviseClick()
                                },
                                leadingIcon = { Icon(Icons.Outlined.History, contentDescription = null) }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Delete", color = RoseError) },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = RoseError) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: String) {
    val (bgColor, textColor) = when (status.lowercase()) {
        "accepted" -> EmeraldLight to EmeraldSuccess
        "sent", "viewed" -> BlueLight to BlueAccent
        "rejected", "cancelled" -> RoseLight to RoseError
        "revised" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun EmptyStateCard(onNewQuotation: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.Description,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Quotations Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Create your first architectural or interior estimate in under 3 minutes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNewQuotation,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("empty_state_create_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Quotation")
            }
        }
    }
}
