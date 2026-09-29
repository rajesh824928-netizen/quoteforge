package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.domain.engine.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.QuotationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationBuilderScreen(
    viewModel: QuotationViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (Long) -> Unit,
    onOpenAiAssistant: () -> Unit
) {
    val quotation by viewModel.currentQuotation.collectAsState()
    val items by viewModel.currentItems.collectAsState()
    val calculation by viewModel.currentCalculation.collectAsState()

    val clients by viewModel.allClients.collectAsState()
    val projects by viewModel.allProjects.collectAsState()
    val companies by viewModel.allCompanies.collectAsState()
    val materials by viewModel.allMaterials.collectAsState()
    val rates by viewModel.allRates.collectAsState()
    val terms by viewModel.allTerms.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(2) } // default to Items & Estimator tab
    var selectedRoomFilter by remember { mutableStateOf("ALL") }

    // Dialog states
    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var showMaterialPicker by remember { mutableStateOf(false) }
    var showPresetPicker by remember { mutableStateOf(false) }
    var showNewClientDialog by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showAddRoomDialog by remember { mutableStateOf(false) }

    val distinctRooms = remember(items) {
        val list = items.map { if (it.roomName.isBlank()) "General" else it.roomName }.distinct().toMutableList()
        if (!list.contains("General")) list.add(0, "General")
        list
    }

    val q = quotation ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = q.quotationNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (q.revision > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Rev.${q.revision}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Text(
                            text = if (q.isEstimate) "Cost Estimation Mode" else "Client Quotation Mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("builder_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenAiAssistant,
                        modifier = Modifier.testTag("builder_ai_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", tint = AmberDark)
                    }
                    TextButton(
                        onClick = {
                            viewModel.saveCurrentQuotation { savedId ->
                                onNavigateToPreview(savedId)
                            }
                        },
                        modifier = Modifier.testTag("preview_export_button")
                    ) {
                        Text("Preview", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Live Calculation Total Bar
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Grand Total (${calculation.items.size} items)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatInr(calculation.grandTotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.saveCurrentQuotation {
                                        // Saved
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("save_draft_button")
                            ) {
                                Text("Save Draft")
                            }

                            Button(
                                onClick = {
                                    viewModel.saveCurrentQuotation { savedId ->
                                        onNavigateToPreview(savedId)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("proceed_preview_button")
                            ) {
                                Text("Generate PDF")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Client & Info", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_client_info")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Tax & Pricing", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_tax_pricing")
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Items & Rooms (${items.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_items_rooms")
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    text = { Text("Scope & Terms", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_scope_terms")
                )
            }

            when (selectedTabIndex) {
                0 -> ClientAndInfoTab(
                    quotation = q,
                    clients = clients,
                    projects = projects,
                    companies = companies,
                    onUpdateQuotation = { viewModel.updateQuotationHeader(it) },
                    onOpenNewClient = { showNewClientDialog = true },
                    onOpenNewProject = { showNewProjectDialog = true }
                )
                1 -> TaxAndPricingTab(
                    quotation = q,
                    calculation = calculation,
                    onUpdateQuotation = { viewModel.updateQuotationHeader(it) }
                )
                2 -> ItemsAndRoomsTab(
                    items = items,
                    calculation = calculation,
                    selectedRoomFilter = selectedRoomFilter,
                    rooms = distinctRooms,
                    isCostPlusMode = q.calculationMode == "COST_PLUS_MARGIN",
                    onSelectRoomFilter = { selectedRoomFilter = it },
                    onAddItemClick = {
                        editingItemIndex = null
                        showAddItemDialog = true
                    },
                    onOpenMaterialPicker = { showMaterialPicker = true },
                    onOpenPresetPicker = { showPresetPicker = true },
                    onOpenAiAssistant = onOpenAiAssistant,
                    onOpenAddRoom = { showAddRoomDialog = true },
                    onEditItem = { idx ->
                        editingItemIndex = idx
                        showAddItemDialog = true
                    },
                    onDuplicateItem = { item ->
                        viewModel.addItem(item.copy(id = 0, itemName = "${item.itemName} (Copy)"))
                    },
                    onDeleteItem = { idx ->
                        viewModel.removeItem(idx)
                    }
                )
                3 -> ScopeAndTermsTab(
                    quotation = q,
                    calculation = calculation,
                    termsPresets = terms,
                    onUpdateQuotation = { viewModel.updateQuotationHeader(it) }
                )
            }
        }
    }

    // Add / Edit Item Dialog
    if (showAddItemDialog) {
        val initialItem = if (editingItemIndex != null && editingItemIndex!! in items.indices) {
            items[editingItemIndex!!]
        } else {
            QuotationItemEntity(
                quotationId = q.id,
                roomName = if (selectedRoomFilter != "ALL") selectedRoomFilter else "General",
                itemName = "",
                unit = "sq.ft",
                quantity = 1.0,
                rate = 1000.0
            )
        }

        ItemEditorDialog(
            initialItem = initialItem,
            isCostPlusMode = q.calculationMode == "COST_PLUS_MARGIN",
            profitMargin = q.profitMarginPercent,
            existingRooms = distinctRooms,
            onDismiss = { showAddItemDialog = false },
            onSave = { savedItem ->
                if (editingItemIndex != null) {
                    viewModel.updateItem(editingItemIndex!!, savedItem)
                } else {
                    viewModel.addItem(savedItem)
                }
                showAddItemDialog = false
            }
        )
    }

    // Material Library Picker Dialog
    if (showMaterialPicker) {
        MaterialPickerDialog(
            materials = materials,
            onDismiss = { showMaterialPicker = false },
            onSelect = { material ->
                viewModel.addItem(
                    QuotationItemEntity(
                        quotationId = q.id,
                        roomName = if (selectedRoomFilter != "ALL") selectedRoomFilter else "General",
                        category = material.category,
                        itemName = material.productName,
                        specification = material.specification,
                        unit = material.unit,
                        quantity = 1.0,
                        rate = material.defaultRate
                    )
                )
                showMaterialPicker = false
            }
        )
    }

    // Industry Presets Picker Dialog
    if (showPresetPicker) {
        IndustryPresetsPickerDialog(
            onDismiss = { showPresetPicker = false },
            onSelect = { preset ->
                viewModel.addItem(
                    QuotationItemEntity(
                        quotationId = q.id,
                        roomName = if (selectedRoomFilter != "ALL") selectedRoomFilter else "General",
                        category = preset.category,
                        itemName = preset.name,
                        specification = preset.defaultSpec,
                        unit = preset.defaultUnit,
                        quantity = 1.0,
                        rate = preset.defaultRate
                    )
                )
                showPresetPicker = false
            }
        )
    }

    // New Client Dialog
    if (showNewClientDialog) {
        NewClientDialog(
            onDismiss = { showNewClientDialog = false },
            onSave = { newClient ->
                viewModel.saveClient(newClient) { savedClientId ->
                    viewModel.updateQuotationHeader { it.copy(clientId = savedClientId) }
                    showNewClientDialog = false
                }
            }
        )
    }

    // New Project Dialog
    if (showNewProjectDialog) {
        NewProjectDialog(
            clientId = q.clientId,
            onDismiss = { showNewProjectDialog = false },
            onSave = { newProject ->
                viewModel.saveProject(newProject) { savedProjectId ->
                    viewModel.updateQuotationHeader { it.copy(projectId = savedProjectId) }
                    showNewProjectDialog = false
                }
            }
        )
    }

    // Add Custom Room Dialog
    if (showAddRoomDialog) {
        AddRoomDialog(
            onDismiss = { showAddRoomDialog = false },
            onAdd = { newRoom ->
                selectedRoomFilter = newRoom
                showAddRoomDialog = false
            }
        )
    }
}

// ----------------------------------------------------
// TAB 1: CLIENT & PROJECT INFO
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientAndInfoTab(
    quotation: QuotationEntity,
    clients: List<ClientEntity>,
    projects: List<ProjectEntity>,
    companies: List<CompanyEntity>,
    onUpdateQuotation: ((QuotationEntity) -> QuotationEntity) -> Unit,
    onOpenNewClient: () -> Unit,
    onOpenNewProject: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DOCUMENT METADATA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = quotation.quotationNumber,
                        onValueChange = { num -> onUpdateQuotation { it.copy(quotationNumber = num) } },
                        label = { Text("Quotation Number") },
                        modifier = Modifier.fillMaxWidth().testTag("input_quotation_number")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quotation.validityDays.toString(),
                            onValueChange = { v ->
                                val days = v.toIntOrNull() ?: 30
                                onUpdateQuotation { it.copy(validityDays = days) }
                            },
                            label = { Text("Validity (Days)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_validity_days")
                        )

                        // Status Selector
                        var statusExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = quotation.status,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Status") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                listOf("Draft", "Sent", "Accepted", "Rejected", "Revised").forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st) },
                                        onClick = {
                                            onUpdateQuotation { it.copy(status = st) }
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Client Selector Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CLIENT SELECTION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = onOpenNewClient,
                            modifier = Modifier.testTag("add_new_client_inline_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Client", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val selectedClient = clients.firstOrNull { it.id == quotation.clientId }

                    var clientDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = clientDropdownExpanded,
                        onExpandedChange = { clientDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedClient?.name ?: "Select Client...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Client") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor().testTag("client_dropdown_trigger")
                        )
                        ExposedDropdownMenu(
                            expanded = clientDropdownExpanded,
                            onDismissRequest = { clientDropdownExpanded = false }
                        ) {
                            clients.forEach { c ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(c.name, fontWeight = FontWeight.Bold)
                                            if (c.companyName.isNotBlank()) {
                                                Text(c.companyName, fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                    },
                                    onClick = {
                                        onUpdateQuotation { it.copy(clientId = c.id) }
                                        clientDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedClient != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                Text("Contact: ${selectedClient.mobile} | ${selectedClient.email}", fontSize = 12.sp)
                                if (selectedClient.address.isNotBlank()) {
                                    Text("Address: ${selectedClient.address}", fontSize = 12.sp)
                                }
                                if (selectedClient.gstin.isNotBlank()) {
                                    Text("GSTIN: ${selectedClient.gstin}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Project Selector Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROJECT DETAILS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = onOpenNewProject,
                            modifier = Modifier.testTag("add_new_project_inline_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Project", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val selectedProject = projects.firstOrNull { it.id == quotation.projectId }

                    var projectDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = projectDropdownExpanded,
                        onExpandedChange = { projectDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedProject?.name ?: "Select or Link Project (Optional)...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Project") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor().testTag("project_dropdown_trigger")
                        )
                        ExposedDropdownMenu(
                            expanded = projectDropdownExpanded,
                            onDismissRequest = { projectDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (Independent Quotation)") },
                                onClick = {
                                    onUpdateQuotation { it.copy(projectId = null) }
                                    projectDropdownExpanded = false
                                }
                            )
                            projects.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(p.name, fontWeight = FontWeight.Bold)
                                            Text("${p.projectType} • ${p.area}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        onUpdateQuotation { it.copy(projectId = p.id) }
                                        projectDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 2: TAX & PRICING
// ----------------------------------------------------
@Composable
fun TaxAndPricingTab(
    quotation: QuotationEntity,
    calculation: QuotationCalculationResult,
    onUpdateQuotation: ((QuotationEntity) -> QuotationEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Calculation Mode
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CALCULATION MODE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = quotation.calculationMode == "SELLING_PRICE",
                            onClick = { onUpdateQuotation { it.copy(calculationMode = "SELLING_PRICE") } },
                            label = { Text("Direct Selling Rate") },
                            modifier = Modifier.weight(1f).testTag("mode_selling_rate")
                        )
                        FilterChip(
                            selected = quotation.calculationMode == "COST_PLUS_MARGIN",
                            onClick = { onUpdateQuotation { it.copy(calculationMode = "COST_PLUS_MARGIN") } },
                            label = { Text("Cost + Margin %") },
                            modifier = Modifier.weight(1f).testTag("mode_cost_plus_margin")
                        )
                    }

                    if (quotation.calculationMode == "COST_PLUS_MARGIN") {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = quotation.profitMarginPercent.toString(),
                            onValueChange = {
                                val margin = it.toDoubleOrNull() ?: 20.0
                                onUpdateQuotation { q -> q.copy(profitMarginPercent = margin) }
                            },
                            label = { Text("Profit Margin %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("input_profit_margin")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Auto-calculates item rate as: (Material + Labour + Installation + Overhead) × (1 + Margin%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // GST & Tax Configuration
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "INDIAN GST TAXATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // GST Type
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = quotation.gstType == "CGST_SGST",
                            onClick = { onUpdateQuotation { it.copy(gstType = "CGST_SGST") } },
                            label = { Text("CGST + SGST (Intra-state)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("gst_type_cgst_sgst")
                        )
                        FilterChip(
                            selected = quotation.gstType == "IGST",
                            onClick = { onUpdateQuotation { it.copy(gstType = "IGST") } },
                            label = { Text("IGST (Inter-state)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("gst_type_igst")
                        )
                        FilterChip(
                            selected = quotation.gstType == "NONE",
                            onClick = { onUpdateQuotation { it.copy(gstType = "NONE") } },
                            label = { Text("No Tax", fontSize = 11.sp) },
                            modifier = Modifier.weight(0.7f).testTag("gst_type_none")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // GST Rate Chips (0%, 5%, 12%, 18%, 28%)
                    Text("GST Rate:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.0, 5.0, 12.0, 18.0, 28.0).forEach { rate ->
                            FilterChip(
                                selected = quotation.taxRate == rate,
                                onClick = { onUpdateQuotation { it.copy(taxRate = rate) } },
                                label = { Text("${rate.toInt()}%") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Inclusive vs Exclusive Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Prices Include GST", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                text = if (quotation.isGstInclusive) "Taxable amount back-calculated from rates" else "GST added on top of taxable amount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = quotation.isGstInclusive,
                            onCheckedChange = { inc -> onUpdateQuotation { it.copy(isGstInclusive = inc) } },
                            modifier = Modifier.testTag("switch_gst_inclusive")
                        )
                    }
                }
            }
        }

        // Overall Discount Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "OVERALL QUOTATION DISCOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = quotation.discountType == "PERCENTAGE",
                            onClick = { onUpdateQuotation { it.copy(discountType = "PERCENTAGE") } },
                            label = { Text("Percentage (%)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = quotation.discountType == "FIXED",
                            onClick = { onUpdateQuotation { it.copy(discountType = "FIXED") } },
                            label = { Text("Flat Amount (₹)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = quotation.discountValue.toString(),
                        onValueChange = {
                            val disc = it.toDoubleOrNull() ?: 0.0
                            onUpdateQuotation { q -> q.copy(discountValue = disc) }
                        },
                        label = { Text(if (quotation.discountType == "PERCENTAGE") "Discount %" else "Discount Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("input_overall_discount")
                    )
                }
            }
        }

        // Live Breakdown Preview Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "FINANCIAL BREAKDOWN SUMMARY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    FinancialRow("Items Subtotal:", CurrencyFormatter.formatInr(calculation.subtotal))
                    if (calculation.discountAmount > 0) {
                        FinancialRow("Overall Discount:", "-${CurrencyFormatter.formatInr(calculation.discountAmount)}", color = RoseError)
                    }
                    FinancialRow("Taxable Base Amount:", CurrencyFormatter.formatInr(calculation.taxableAmount))
                    if (calculation.cgstAmount > 0) {
                        FinancialRow("CGST (${calculation.gstRate / 2}%):", CurrencyFormatter.formatInr(calculation.cgstAmount))
                        FinancialRow("SGST (${calculation.gstRate / 2}%):", CurrencyFormatter.formatInr(calculation.sgstAmount))
                    } else if (calculation.igstAmount > 0) {
                        FinancialRow("IGST (${calculation.gstRate}%):", CurrencyFormatter.formatInr(calculation.igstAmount))
                    }
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    FinancialRow("Net Grand Total:", CurrencyFormatter.formatInr(calculation.grandTotal), isBold = true)
                }
            }
        }
    }
}

@Composable
fun FinancialRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    color: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
        )
    }
}

// ----------------------------------------------------
// TAB 3: ITEMS & ROOM ESTIMATOR
// ----------------------------------------------------
@Composable
fun ItemsAndRoomsTab(
    items: List<QuotationItemEntity>,
    calculation: QuotationCalculationResult,
    selectedRoomFilter: String,
    rooms: List<String>,
    isCostPlusMode: Boolean,
    onSelectRoomFilter: (String) -> Unit,
    onAddItemClick: () -> Unit,
    onOpenMaterialPicker: () -> Unit,
    onOpenPresetPicker: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenAddRoom: () -> Unit,
    onEditItem: (Int) -> Unit,
    onDuplicateItem: (QuotationItemEntity) -> Unit,
    onDeleteItem: (Int) -> Unit
) {
    val filteredIndices = remember(items, selectedRoomFilter) {
        items.indices.filter { idx ->
            val itm = items[idx]
            val itmRoom = if (itm.roomName.isBlank()) "General" else itm.roomName
            selectedRoomFilter == "ALL" || itmRoom.equals(selectedRoomFilter, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Quick Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddItemClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("btn_add_item_custom")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenPresetPicker,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("btn_from_presets")
                ) {
                    Icon(Icons.Default.LibraryBooks, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Presets", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenMaterialPicker,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("btn_from_materials")
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Materials", fontSize = 12.sp)
                }
            }
        }

        // Room Filter Tabs
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = selectedRoomFilter == "ALL",
                        onClick = { onSelectRoomFilter("ALL") },
                        label = { Text("All Rooms (${items.size})") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("room_chip_ALL")
                    )
                }
                items(rooms) { room ->
                    val roomTotal = calculation.roomTotals[room] ?: 0.0
                    FilterChip(
                        selected = selectedRoomFilter.equals(room, ignoreCase = true),
                        onClick = { onSelectRoomFilter(room) },
                        label = { Text("$room (${CurrencyFormatter.formatInr(roomTotal, false)})") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("room_chip_$room")
                    )
                }
                item {
                    IconButton(
                        onClick = onOpenAddRoom,
                        modifier = Modifier.size(36.dp).testTag("btn_add_custom_room")
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Room", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (filteredIndices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Layers, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Items in this Room", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Add custom line items or pick from standard modular furniture & civil presets.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        } else {
            items(filteredIndices) { itemIndex ->
                val item = items[itemIndex]
                val calcItem = calculation.items.getOrNull(itemIndex)

                ItemCardView(
                    item = item,
                    calcItem = calcItem,
                    isCostPlusMode = isCostPlusMode,
                    onEdit = { onEditItem(itemIndex) },
                    onDuplicate = { onDuplicateItem(item) },
                    onDelete = { onDeleteItem(itemIndex) }
                )
            }
        }
    }
}

@Composable
fun ItemCardView(
    item: QuotationItemEntity,
    calcItem: CalculatedItem?,
    isCostPlusMode: Boolean,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.roomName.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = item.roomName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (item.category.isNotBlank()) {
                            Text(
                                text = item.category,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.itemName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (item.description.isNotBlank()) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatInr(calcItem?.netAmount ?: (item.quantity * item.rate)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${item.quantity} ${item.unit} @ ${CurrencyFormatter.formatInr(calcItem?.effectiveRate ?: item.rate, false)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.specification.isNotBlank()) {
                    Text(
                        text = "Spec: ${item.specification}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Row {
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = RoseError, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 4: SCOPE & TERMS
// ----------------------------------------------------
@Composable
fun ScopeAndTermsTab(
    quotation: QuotationEntity,
    calculation: QuotationCalculationResult,
    termsPresets: List<TermPresetEntity>,
    onUpdateQuotation: ((QuotationEntity) -> QuotationEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Milestone Presets
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PAYMENT MILESTONES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Pick Milestone Structure:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                val milestones = PaymentMilestoneEngine.calculateMilestones(
                                    PaymentMilestoneEngine.PRESET_50_40_10,
                                    calculation.grandTotal
                                )
                                val termsText = milestones.joinToString("\n") { "• ${it.percentage.toInt()}% (${CurrencyFormatter.formatInr(it.amount)}): ${it.stageName}" }
                                onUpdateQuotation { it.copy(termsAndConditions = "${it.termsAndConditions}\n\nPayment Terms:\n$termsText".trim()) }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("50-40-10", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val milestones = PaymentMilestoneEngine.calculateMilestones(
                                    PaymentMilestoneEngine.PRESET_40_40_20,
                                    calculation.grandTotal
                                )
                                val termsText = milestones.joinToString("\n") { "• ${it.percentage.toInt()}% (${CurrencyFormatter.formatInr(it.amount)}): ${it.stageName}" }
                                onUpdateQuotation { it.copy(termsAndConditions = "${it.termsAndConditions}\n\nPayment Terms:\n$termsText".trim()) }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("40-40-20", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val milestones = PaymentMilestoneEngine.calculateMilestones(
                                    PaymentMilestoneEngine.PRESET_FULL_ADVANCE,
                                    calculation.grandTotal
                                )
                                val termsText = milestones.joinToString("\n") { "• 100% (${CurrencyFormatter.formatInr(it.amount)}): ${it.stageName}" }
                                onUpdateQuotation { it.copy(termsAndConditions = "${it.termsAndConditions}\n\nPayment Terms:\n$termsText".trim()) }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("100% Adv", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Scope Included / Excluded
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SCOPE OF WORK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = quotation.scopeIncluded,
                        onValueChange = { inc -> onUpdateQuotation { it.copy(scopeIncluded = inc) } },
                        label = { Text("Scope Included") },
                        placeholder = { Text("e.g. All woodwork manufacturing, transport, GI false ceiling framework, 2-coat painting...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = quotation.scopeExcluded,
                        onValueChange = { exc -> onUpdateQuotation { it.copy(scopeExcluded = exc) } },
                        label = { Text("Scope Excluded") },
                        placeholder = { Text("e.g. Chimney, hob, civil wall structural modifications, loose electrical appliances...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                }
            }
        }

        // Warranties & Timeline
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WARRANTY & TIMELINE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = quotation.timeline,
                        onValueChange = { tl -> onUpdateQuotation { it.copy(timeline = tl) } },
                        label = { Text("Project Timeline") },
                        placeholder = { Text("e.g. 45 business days from 50% mobilization advance") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = quotation.warrantyTerms,
                        onValueChange = { wr -> onUpdateQuotation { it.copy(warrantyTerms = wr) } },
                        label = { Text("Warranty Terms") },
                        placeholder = { Text("e.g. 10 Years delamination on BWP plywood; 5 Years on mechanical hardware") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Terms & Conditions Field
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TERMS & CONDITIONS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = quotation.termsAndConditions,
                        onValueChange = { tc -> onUpdateQuotation { it.copy(termsAndConditions = tc) } },
                        label = { Text("Terms & Conditions") },
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// DIALOGS: ITEM EDITOR, MATERIAL PICKER, PRESETS, CLIENT
// ----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorDialog(
    initialItem: QuotationItemEntity,
    isCostPlusMode: Boolean,
    profitMargin: Double,
    existingRooms: List<String>,
    onDismiss: () -> Unit,
    onSave: (QuotationItemEntity) -> Unit
) {
    var roomName by remember { mutableStateOf(initialItem.roomName) }
    var category by remember { mutableStateOf(initialItem.category) }
    var itemName by remember { mutableStateOf(initialItem.itemName) }
    var description by remember { mutableStateOf(initialItem.description) }
    var specification by remember { mutableStateOf(initialItem.specification) }
    var unit by remember { mutableStateOf(initialItem.unit) }
    var quantityStr by remember { mutableStateOf(initialItem.quantity.toString()) }
    var rateStr by remember { mutableStateOf(initialItem.rate.toString()) }

    // Cost Breakdown fields
    var materialCostStr by remember { mutableStateOf(initialItem.materialCost.toString()) }
    var labourCostStr by remember { mutableStateOf(initialItem.labourCost.toString()) }
    var installationCostStr by remember { mutableStateOf(initialItem.installationCost.toString()) }
    var overheadCostStr by remember { mutableStateOf(initialItem.overheadCost.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = if (initialItem.id == 0L) "Add Quotation Item" else "Edit Item",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Item Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_item_name")
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = roomName,
                            onValueChange = { roomName = it },
                            label = { Text("Room / Area") },
                            modifier = Modifier.weight(1f).testTag("dialog_room_name")
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f).testTag("dialog_category")
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("dialog_quantity")
                        )

                        var unitExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = unitExpanded,
                            onExpandedChange = { unitExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = unit,
                                onValueChange = { unit = it },
                                label = { Text("Unit") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                                modifier = Modifier.menuAnchor().testTag("dialog_unit")
                            )
                            ExposedDropdownMenu(
                                expanded = unitExpanded,
                                onDismissRequest = { unitExpanded = false }
                            ) {
                                IndustryPresets.UNITS.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u) },
                                        onClick = {
                                            unit = u
                                            unitExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isCostPlusMode) {
                    item {
                        OutlinedTextField(
                            value = rateStr,
                            onValueChange = { rateStr = it },
                            label = { Text("Rate (₹ per $unit) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("dialog_rate")
                        )
                    }
                } else {
                    item {
                        Text("Cost Breakdown (Cost + $profitMargin% Margin):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = materialCostStr,
                                onValueChange = { materialCostStr = it },
                                label = { Text("Material") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = labourCostStr,
                                onValueChange = { labourCostStr = it },
                                label = { Text("Labour") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = installationCostStr,
                                onValueChange = { installationCostStr = it },
                                label = { Text("Install") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = overheadCostStr,
                                onValueChange = { overheadCostStr = it },
                                label = { Text("Overhead") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = specification,
                        onValueChange = { specification = it },
                        label = { Text("Specification") },
                        placeholder = { Text("e.g. 18mm BWP marine plywood, Hafele soft-close") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (itemName.isNotBlank()) {
                                    val qVal = quantityStr.toDoubleOrNull() ?: 1.0
                                    val rVal = rateStr.toDoubleOrNull() ?: 0.0
                                    onSave(
                                        initialItem.copy(
                                            roomName = roomName.trim(),
                                            category = category.trim(),
                                            itemName = itemName.trim(),
                                            description = description.trim(),
                                            specification = specification.trim(),
                                            unit = unit.trim(),
                                            quantity = qVal,
                                            rate = rVal,
                                            materialCost = materialCostStr.toDoubleOrNull() ?: 0.0,
                                            labourCost = labourCostStr.toDoubleOrNull() ?: 0.0,
                                            installationCost = installationCostStr.toDoubleOrNull() ?: 0.0,
                                            overheadCost = overheadCostStr.toDoubleOrNull() ?: 0.0
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.testTag("dialog_item_save_button")
                        ) {
                            Text("Save Item")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialPickerDialog(
    materials: List<MaterialEntity>,
    onDismiss: () -> Unit,
    onSelect: (MaterialEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(materials, searchQuery) {
        materials.filter {
            searchQuery.isBlank() || it.productName.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().height(480.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select from Material Library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search materials...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { mat ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(mat) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mat.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${mat.category} • ${mat.specification}", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                }
                                Text(
                                    CurrencyFormatter.formatInr(mat.defaultRate, false) + " / " + mat.unit,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun IndustryPresetsPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (PresetItem) -> Unit
) {
    var selectedCat by remember { mutableStateOf("ALL") }
    val categories = remember {
        listOf("ALL") + IndustryPresets.PRESET_ITEMS.map { it.category }.distinct()
    }
    val filtered = remember(selectedCat) {
        if (selectedCat == "ALL") IndustryPresets.PRESET_ITEMS else IndustryPresets.PRESET_ITEMS.filter { it.category == selectedCat }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().height(520.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Architectural & Interior Presets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCat == cat,
                            onClick = { selectedCat = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { p ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(p) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${p.category} • ${p.defaultSpec}", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                }
                                Text(
                                    CurrencyFormatter.formatInr(p.defaultRate, false) + " / " + p.defaultUnit,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun NewClientDialog(
    onDismiss: () -> Unit,
    onSave: (ClientEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick Add Client", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Client Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("new_client_name")
                )

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = gstin,
                    onValueChange = { gstin = it },
                    label = { Text("GSTIN") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Site / Billing Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    ClientEntity(
                                        name = name.trim(),
                                        companyName = company.trim(),
                                        mobile = mobile.trim(),
                                        email = email.trim(),
                                        gstin = gstin.trim(),
                                        address = address.trim()
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("save_new_client_button")
                    ) {
                        Text("Save Client")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectDialog(
    clientId: Long,
    onDismiss: () -> Unit,
    onSave: (ProjectEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var projectType by remember { mutableStateOf("Residential") }
    var area by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick Add Project", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("new_project_name")
                )

                var typeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = projectType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Project Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        IndustryPresets.PROJECT_TYPES.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    projectType = t
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Area (e.g. 2,400 sq.ft)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Site Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    ProjectEntity(
                                        clientId = clientId,
                                        name = name.trim(),
                                        projectType = projectType,
                                        area = area.trim(),
                                        address = address.trim()
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Save Project")
                    }
                }
            }
        }
    }
}

@Composable
fun AddRoomDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var roomName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Custom Room / Area", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Room Name") },
                    placeholder = { Text("e.g. Home Theatre, Terrace Garden") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Or select standard room:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(IndustryPresets.ROOMS.take(6)) { r ->
                        SuggestionChip(
                            onClick = { roomName = r },
                            label = { Text(r, fontSize = 11.sp) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (roomName.isNotBlank()) onAdd(roomName.trim())
                        }
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}
