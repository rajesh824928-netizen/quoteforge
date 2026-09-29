package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.NavyDark
import com.example.ui.theme.WarmIvory
import com.example.ui.viewmodel.QuotationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceSetupScreen(
    viewModel: QuotationViewModel,
    onWorkspaceCreated: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()

    var workspaceName by remember { mutableStateOf(currentUser?.displayName?.let { "$it Studio" } ?: "My Architecture Studio") }
    var companyName by remember { mutableStateOf(currentUser?.displayName?.let { "$it & Associates" } ?: "Design & Build Co.") }
    var selectedBusinessType by remember { mutableStateOf("Architecture") }
    var selectedCurrency by remember { mutableStateOf("INR (₹)") }
    var businessTypeExpanded by remember { mutableStateOf(false) }

    val businessTypes = listOf(
        "Architecture",
        "Interior Design",
        "Modular Furniture",
        "General Contractor",
        "Civil & Construction",
        "Freelance Estimator"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workspace Setup", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDark)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LuxuryGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Business, contentDescription = null, tint = WarmIvory, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Create Your First Workspace", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WarmIvory)
                        Text(
                            text = "Workspaces isolate team members, clients, rate cards, and quotation revisions.",
                            fontSize = 11.sp,
                            color = WarmIvory.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Input Fields
            OutlinedTextField(
                value = workspaceName,
                onValueChange = { workspaceName = it },
                label = { Text("Workspace Name") },
                placeholder = { Text("e.g. Knot Architects, Studio Metro") },
                leadingIcon = { Icon(Icons.Outlined.Workspaces, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().testTag("input_workspace_name"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company / Legal Entity Name") },
                placeholder = { Text("e.g. Knot Design Private Limited") },
                leadingIcon = { Icon(Icons.Outlined.Business, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().testTag("input_company_name"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Business Type Selector
            ExposedDropdownMenuBox(
                expanded = businessTypeExpanded,
                onExpandedChange = { businessTypeExpanded = !businessTypeExpanded }
            ) {
                OutlinedTextField(
                    value = selectedBusinessType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Primary Business Specialization") },
                    leadingIcon = { Icon(Icons.Outlined.Category, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = businessTypeExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                ExposedDropdownMenu(
                    expanded = businessTypeExpanded,
                    onDismissRequest = { businessTypeExpanded = false }
                ) {
                    businessTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedBusinessType = type
                                businessTypeExpanded = false
                            }
                        )
                    }
                }
            }

            // Default Currency
            OutlinedTextField(
                value = selectedCurrency,
                onValueChange = {},
                readOnly = true,
                label = { Text("Default Workspace Currency & Tax Jurisdiction") },
                leadingIcon = { Icon(Icons.Outlined.CurrencyRupee, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Pre-configured with Indian GST (CGST/SGST/IGST) and Lakh/Crore formatting.") },
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (workspaceName.isBlank()) {
                        Toast.makeText(context, "Please enter a workspace name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.createWorkspace(
                        name = workspaceName.trim(),
                        companyName = companyName.trim().ifBlank { workspaceName.trim() },
                        businessType = selectedBusinessType
                    )
                    Toast.makeText(context, "Workspace '${workspaceName.trim()}' created!", Toast.LENGTH_SHORT).show()
                    onWorkspaceCreated()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_create_workspace_submit"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Launch Workspace & Continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
