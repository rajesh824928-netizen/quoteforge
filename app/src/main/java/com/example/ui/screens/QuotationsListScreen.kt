package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuotationEntity
import com.example.domain.engine.CalculationEngine
import com.example.domain.engine.CurrencyFormatter
import com.example.domain.engine.DiscountType
import com.example.domain.engine.GstType
import com.example.ui.theme.RoseError
import com.example.ui.viewmodel.QuotationViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationsListScreen(
    viewModel: QuotationViewModel,
    onNavigateToNewQuotation: (isEstimate: Boolean) -> Unit,
    onNavigateToEditQuotation: (Long) -> Unit,
    onNavigateToPreviewQuotation: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val quotations by viewModel.allQuotations.collectAsState()
    val clients by viewModel.allClients.collectAsState()
    val projects by viewModel.allProjects.collectAsState()

    val clientMap = remember(clients) { clients.associateBy { it.id } }
    val projectMap = remember(projects) { projects.associateBy { it.id } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, DRAFT, SENT, ACCEPTED, REVISED, INVOICES, ESTIMATES
    var sortOrder by remember { mutableStateOf("NEWEST") } // NEWEST, OLDEST, HIGHEST, LOWEST
    var showSortMenu by remember { mutableStateOf(false) }

    val filterOptions = listOf(
        "ALL" to "All",
        "Draft" to "Drafts",
        "Sent" to "Sent",
        "Accepted" to "Accepted",
        "Revised" to "Revised",
        "INVOICES" to "Invoices",
        "ESTIMATES" to "Estimates"
    )

    val filteredList = remember(quotations, searchQuery, selectedFilter, sortOrder, clientMap) {
        var list = quotations.filter { q ->
            val client = clientMap[q.clientId]
            val project = q.projectId?.let { projectMap[it] }

            val matchesSearch = searchQuery.isBlank() ||
                    q.quotationNumber.contains(searchQuery, ignoreCase = true) ||
                    (client?.name?.contains(searchQuery, ignoreCase = true) == true) ||
                    (project?.name?.contains(searchQuery, ignoreCase = true) == true)

            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "INVOICES" -> q.isInvoice
                "ESTIMATES" -> q.isEstimate
                else -> q.status.equals(selectedFilter, ignoreCase = true)
            }

            matchesSearch && matchesFilter
        }

        list = when (sortOrder) {
            "OLDEST" -> list.sortedBy { it.date }
            "HIGHEST" -> list.sortedByDescending { it.id } // Proxy or sort by date/id
            "LOWEST" -> list.sortedBy { it.id }
            else -> list.sortedByDescending { it.date }
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Quotations & Estimates", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${filteredList.size} documents found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("quotations_sort_button")
                        ) {
                            Icon(Icons.Outlined.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest Date First") },
                                onClick = {
                                    sortOrder = "NEWEST"
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOrder == "NEWEST") Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest Date First") },
                                onClick = {
                                    sortOrder = "OLDEST"
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOrder == "OLDEST") Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToNewQuotation(false) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("quotations_create_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Quotation")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by quote #, client, or project...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("quotations_search_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(filterOptions) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("filter_chip_$key")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching quotations" else "No quotations in this category",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button to create a new quotation or estimate.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredList, key = { it.id }) { quote ->
                        val client = clientMap[quote.clientId]
                        val project = quote.projectId?.let { projectMap[it] }

                        QuotationRowCard(
                            quotation = quote,
                            clientName = client?.name ?: "Unknown Client",
                            projectName = project?.name,
                            onPreview = { onNavigateToPreviewQuotation(quote.id) },
                            onEdit = { onNavigateToEditQuotation(quote.id) },
                            onDuplicate = {
                                viewModel.duplicateQuotation(quote.id) { newId ->
                                    Toast.makeText(context, "Quotation duplicated", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRevise = {
                                viewModel.createRevision(quote.id) { newId ->
                                    Toast.makeText(context, "New revision created", Toast.LENGTH_SHORT).show()
                                    onNavigateToEditQuotation(newId)
                                }
                            },
                            onDelete = {
                                viewModel.deleteQuotation(quote)
                                Toast.makeText(context, "Quotation deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuotationRowCard(
    quotation: QuotationEntity,
    clientName: String,
    projectName: String?,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onRevise: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateStr = remember(quotation.date) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(quotation.date))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreview() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = quotation.quotationNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
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
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (quotation.isInvoice) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "INVOICE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else if (quotation.isEstimate) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "ESTIMATE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = clientName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    if (!projectName.isNullOrBlank()) {
                        Text(
                            text = "Project: $projectName",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    StatusPill(status = quotation.status)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Validity: ${quotation.validityDays} days",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPreview, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Visibility, contentDescription = "Preview", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options", modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                onClick = {
                                    onDuplicate()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Create Revision") },
                                onClick = {
                                    onRevise()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Outlined.Difference, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Delete", color = RoseError) },
                                onClick = {
                                    onDelete()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = RoseError, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
