package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ClientEntity
import com.example.data.model.CompanyEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.QuotationEntity
import com.example.domain.engine.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.QuotationViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationPreviewScreen(
    viewModel: QuotationViewModel,
    quotationId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var quotation by remember { mutableStateOf<QuotationEntity?>(null) }
    var company by remember { mutableStateOf<CompanyEntity?>(null) }
    var client by remember { mutableStateOf<ClientEntity?>(null) }
    var project by remember { mutableStateOf<ProjectEntity?>(null) }
    var calculationResult by remember { mutableStateOf<QuotationCalculationResult?>(null) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }

    var showWhatsAppDialog by remember { mutableStateOf(false) }

    LaunchedEffect(quotationId) {
        val q = viewModel.quotationRepo.getQuotationById(quotationId)
        quotation = q
        if (q != null) {
            company = viewModel.quotationRepo.getCompanyById(q.companyId)
            client = viewModel.quotationRepo.getClientById(q.clientId)
            project = q.projectId?.let { viewModel.quotationRepo.getProjectById(it) }

            val items = viewModel.quotationRepo.getItemsForQuotation(quotationId)
            items.collect { dbItems ->
                val calcMode = if (q.calculationMode == "COST_PLUS_MARGIN") CalculationMode.COST_PLUS_MARGIN else CalculationMode.SELLING_PRICE
                val calculated = dbItems.map { itm ->
                    val sum = CalculationEngine.calculateItem(
                        quantity = itm.quantity,
                        rate = itm.rate,
                        discountType = if (itm.discountType == "FIXED") DiscountType.FIXED else DiscountType.PERCENTAGE,
                        discountValue = itm.discountValue,
                        taxRate = itm.taxRate,
                        calculationMode = calcMode,
                        profitMarginPercent = q.profitMarginPercent,
                        materialCost = itm.materialCost,
                        labourCost = itm.labourCost,
                        installationCost = itm.installationCost,
                        overheadCost = itm.overheadCost
                    )
                    CalculatedItem(
                        id = itm.id,
                        roomName = itm.roomName,
                        category = itm.category,
                        itemName = itm.itemName,
                        description = itm.description,
                        specification = itm.specification,
                        unit = itm.unit,
                        quantity = itm.quantity,
                        rate = itm.rate,
                        effectiveRate = sum.effectiveRate,
                        grossAmount = sum.grossAmount,
                        discountAmount = sum.discountAmount,
                        netAmount = sum.netAmount,
                        totalCost = sum.totalCost,
                        itemProfit = sum.itemProfit
                    )
                }

                val gstType = when (q.gstType) {
                    "IGST" -> GstType.IGST
                    "NONE" -> GstType.NONE
                    else -> GstType.CGST_SGST
                }

                val calc = CalculationEngine.calculateQuotation(
                    items = calculated,
                    overallDiscountType = if (q.discountType == "FIXED") DiscountType.FIXED else DiscountType.PERCENTAGE,
                    overallDiscountValue = q.discountValue,
                    gstRate = q.taxRate,
                    isGstInclusive = q.isGstInclusive,
                    gstType = gstType
                )
                calculationResult = calc

                // Generate PDF in background
                generatedPdfFile = viewModel.generatePdf(context, quotationId)
            }
        }
    }

    val q = quotation ?: return
    val calc = calculationResult ?: return

    val styles = listOf(
        "MINIMAL_STUDIO" to "Minimal Studio",
        "PROFESSIONAL_CORPORATE" to "Corporate",
        "PREMIUM_ARCHITECTURAL" to "Architectural"
    )
    val accentPresets = listOf(
        "Gold" to "#A3834C",
        "Black" to "#1F1F1F",
        "Slate" to "#475569",
        "Navy" to "#1E3A5F",
        "Forest" to "#285943",
        "Terracotta" to "#A45135"
    )
    val watermarkOptions = listOf("None", "DRAFT", "CONFIDENTIAL", "REVISED")


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(q.quotationNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            client?.name ?: "Quotation Preview",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("preview_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToEdit(q.id) },
                        modifier = Modifier.testTag("preview_edit_action")
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val file = generatedPdfFile ?: viewModel.generatePdf(context, q.id)
                                if (file != null) {
                                    val msg = ShareHelper.generateWhatsAppMessage(q, calc.grandTotal, company, client, project)
                                    ShareHelper.sharePdf(context, file, msg)
                                } else {
                                    Toast.makeText(context, "Could not generate PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("btn_share_pdf")
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share PDF", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val file = viewModel.generatePdf(context, q.id, saveToDrive = true)
                                if (file != null) {
                                    Toast.makeText(context, "Saved to Google Drive: QuotationApp/Quotations/${file.name}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "PDF generation failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        modifier = Modifier.weight(1f).testTag("btn_save_drive")
                    ) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Drive", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { showWhatsAppDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        modifier = Modifier.weight(1f).testTag("btn_share_whatsapp")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 11.sp)
                    }

                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFE2E8F0)), // Neutral desk background for A4 document
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // PDF Style & Branding Chooser Card
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("PDF Design Style:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(styles) { (styleCode, styleName) ->
                                FilterChip(
                                    selected = q.pdfStyle.equals(styleCode, ignoreCase = true),
                                    onClick = {
                                        val updated = q.copy(pdfStyle = styleCode)
                                        quotation = updated
                                        viewModel.updateQuotationHeader { it.copy(pdfStyle = styleCode) }
                                        coroutineScope.launch {
                                            viewModel.quotationRepo.saveQuotation(updated, emptyList())
                                            generatedPdfFile = viewModel.generatePdf(context, q.id, overrideStyle = styleCode)
                                        }
                                    },
                                    label = { Text(styleName, fontSize = 11.sp) }
                                )
                            }
                        }

                        Text("Accent Color:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(accentPresets) { (colorName, colorHex) ->
                                val isSelected = q.templatePrimaryColor.equals(colorHex, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val updated = q.copy(templatePrimaryColor = colorHex)
                                        quotation = updated
                                        viewModel.updateQuotationHeader { it.copy(templatePrimaryColor = colorHex) }
                                        coroutineScope.launch {
                                            viewModel.quotationRepo.saveQuotation(updated, emptyList())
                                            generatedPdfFile = viewModel.generatePdf(context, q.id, overrideAccentColor = colorHex)
                                        }
                                    },
                                    label = { Text(colorName, fontSize = 10.sp) }
                                )
                            }
                        }

                        Text("Watermark:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(watermarkOptions) { wm ->
                                val isEnabled = if (wm == "None") !q.watermarkEnabled else q.watermarkEnabled && q.watermarkText == wm
                                FilterChip(
                                    selected = isEnabled,
                                    onClick = {
                                        val enabled = wm != "None"
                                        val text = if (enabled) wm else ""
                                        val updated = q.copy(watermarkEnabled = enabled, watermarkText = text)
                                        quotation = updated
                                        viewModel.updateQuotationHeader { it.copy(watermarkEnabled = enabled, watermarkText = text) }
                                        coroutineScope.launch {
                                            viewModel.quotationRepo.saveQuotation(updated, emptyList())
                                            generatedPdfFile = viewModel.generatePdf(context, q.id, overrideWatermarkEnabled = enabled, overrideWatermarkText = text)
                                        }
                                    },
                                    label = { Text(wm, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            }



            // Quick Status Actions Bar
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Status: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            StatusPill(status = q.status)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (q.status != "Accepted") {
                                OutlinedButton(
                                    onClick = { viewModel.updateStatus(q.id, "Accepted") },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Mark Accepted", fontSize = 11.sp)
                                }
                            }
                            if (!q.isInvoice) {
                                Button(
                                    onClick = {
                                        viewModel.convertToInvoice(q.id) { invNo ->
                                            Toast.makeText(context, "Converted to Invoice: $invNo", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Convert to Invoice", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // High Fidelity A4 Paper Layout Preview (Dynamically reflects selected style, accent color & watermark)
            item {
                val accentColor = remember(q.templatePrimaryColor) {
                    try {
                        Color(android.graphics.Color.parseColor(q.templatePrimaryColor))
                    } catch (e: Exception) {
                        Color(0xFFA3834C)
                    }
                }
                val isCorporate = q.pdfStyle.equals("PROFESSIONAL_CORPORATE", ignoreCase = true)
                val isArchitectural = q.pdfStyle.equals("PREMIUM_ARCHITECTURAL", ignoreCase = true)
                val isMinimal = !isCorporate && !isArchitectural

                Card(
                    shape = RoundedCornerShape(if (isMinimal) 2.dp else 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isArchitectural) 6.dp else 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // 1. Watermark Diagonal Overlay (if enabled)
                        if (q.watermarkEnabled && q.watermarkText.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(480.dp)
                                    .align(Alignment.Center)
                                    .graphicsLayer {
                                        rotationZ = -30f
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = q.watermarkText.uppercase(),
                                    fontSize = 54.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 8.sp,
                                    color = accentColor.copy(alpha = 0.14f)
                                )
                            }
                        }

                        // 2. Paper Content
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Top Banner or Accent Strip based on Style
                            if (isArchitectural) {
                                // Premium Architectural Header Block
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Architecture,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = company?.name ?: "Knot Architects",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "ARCHITECTURAL ESTIMATE & SPECIFICATION",
                                                    fontSize = 8.sp,
                                                    letterSpacing = 1.sp,
                                                    color = accentColor
                                                )
                                            }
                                        }

                                        Surface(
                                            color = accentColor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "REV ${q.revision}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            } else if (isCorporate) {
                                // Professional Corporate Solid Accent Banner
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(accentColor)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            } else {
                                // Minimal Studio Thin Hairline Accent
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(accentColor)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // Document Header: Company Info + Quotation Meta
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (!isArchitectural) {
                                        Text(
                                            text = company?.name ?: "Knot Architects",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (isCorporate) accentColor else Color(0xFF0F172A)
                                        )
                                    }
                                    Text(company?.address ?: "", fontSize = 9.5.sp, color = Color.Gray)
                                    Text("Ph: ${company?.phone ?: ""} | ${company?.email ?: ""}", fontSize = 9.5.sp, color = Color.Gray)
                                    if (company?.gstin?.isNotBlank() == true) {
                                        Text("GSTIN: ${company?.gstin}", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = if (isCorporate) accentColor else Color.DarkGray)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (q.isInvoice) "TAX INVOICE" else if (q.isEstimate) "COST ESTIMATE" else "QUOTATION",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                    Text("Doc #: ${q.quotationNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(q.date))
                                    Text("Date: $dateStr", fontSize = 10.sp, color = Color.Gray)
                                    Text("Validity: ${q.validityDays} Days", fontSize = 10.sp, color = Color.Gray)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Client & Project Information Section
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Bill To Box
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isCorporate) Color(0xFFF1F5F9) else Color(0xFFF8FAFC),
                                    border = if (isCorporate) androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("QUOTATION PREPARED FOR:", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                        Text(client?.name ?: "Client", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        if (client?.companyName?.isNotBlank() == true) {
                                            Text(client?.companyName ?: "", fontSize = 9.5.sp)
                                        }
                                        Text("Ph: ${client?.mobile ?: "N/A"}", fontSize = 9.5.sp, color = Color.Gray)
                                        if (client?.gstin?.isNotBlank() == true) {
                                            Text("GSTIN: ${client?.gstin}", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                // Project Box
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isCorporate) Color(0xFFF1F5F9) else Color(0xFFF8FAFC),
                                    border = if (isCorporate) androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("PROJECT SITE:", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                        Text(project?.name ?: "General Works", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Type: ${project?.projectType ?: "Interior"} | Area: ${project?.area ?: "N/A"}", fontSize = 9.5.sp, color = Color.Gray)
                                        if (project?.address?.isNotBlank() == true) {
                                            Text("Site: ${project?.address}", fontSize = 9.5.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Table Header Row (Adapts to selected style)
                            if (isMinimal) {
                                // Minimal Table Header: White background with clean bottom line
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                                    Text("ITEM & SPECIFICATION", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("QTY", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(42.dp))
                                    Text("RATE", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(55.dp))
                                    Text("AMOUNT", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(65.dp))
                                }
                                HorizontalDivider(thickness = 1.dp, color = accentColor)
                            } else {
                                // Corporate & Architectural Table Header: Solid accent bar
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = if (isArchitectural) Color(0xFF1E293B) else accentColor
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("#", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                                        Text("ITEM & SPECIFICATION", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text("QTY", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(42.dp))
                                        Text("RATE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(55.dp))
                                        Text("AMOUNT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(65.dp))
                                    }
                                }
                            }

                            // Items List with Room Sections
                            var lastRoom = ""
                            calc.items.forEachIndexed { idx, itm ->
                                val room = if (itm.roomName.isBlank()) "General" else itm.roomName
                                if (room != lastRoom) {
                                    lastRoom = room
                                    Surface(
                                        color = if (isArchitectural) accentColor.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "AREA / SECTION: ${room.uppercase()}",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isCorporate && idx % 2 == 1) Color(0xFFF8FAFC) else Color.Transparent)
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("${idx + 1}", fontSize = 9.sp, modifier = Modifier.width(20.dp), color = Color.Gray)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(itm.itemName, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        if (itm.specification.isNotBlank()) {
                                            Text(itm.specification, fontSize = 8.5.sp, color = Color.DarkGray)
                                        }
                                    }
                                    Text("${itm.quantity} ${itm.unit}", fontSize = 9.sp, modifier = Modifier.width(42.dp))
                                    Text(CurrencyFormatter.formatInr(itm.effectiveRate, false), fontSize = 9.sp, modifier = Modifier.width(55.dp))
                                    Text(CurrencyFormatter.formatInr(itm.netAmount, false), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(65.dp))
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0))
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Totals Summary Box (Aligned Right)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Column(
                                    modifier = Modifier
                                        .width(225.dp)
                                        .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(4.dp))
                                        .border(0.5.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .padding(10.dp)
                                ) {
                                    FinancialRow("Subtotal:", CurrencyFormatter.formatInr(calc.subtotal))
                                    if (calc.discountAmount > 0) {
                                        FinancialRow("Discount (${q.discountValue}%):", "-${CurrencyFormatter.formatInr(calc.discountAmount)}", color = RoseError)
                                    }
                                    FinancialRow("Taxable Amount:", CurrencyFormatter.formatInr(calc.taxableAmount))
                                    if (calc.cgstAmount > 0) {
                                        FinancialRow("CGST (${calc.gstRate / 2}%):", CurrencyFormatter.formatInr(calc.cgstAmount))
                                        FinancialRow("SGST (${calc.gstRate / 2}%):", CurrencyFormatter.formatInr(calc.sgstAmount))
                                    } else if (calc.igstAmount > 0) {
                                        FinancialRow("IGST (${calc.gstRate}%):", CurrencyFormatter.formatInr(calc.igstAmount))
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = accentColor)
                                    // Highlighted Grand Total Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(accentColor, shape = RoundedCornerShape(2.dp))
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("GRAND TOTAL:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        Text(CurrencyFormatter.formatInr(calc.grandTotal), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Amount in Words
                            Text("AMOUNT IN WORDS:", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            Text(CurrencyFormatter.amountToWords(calc.grandTotal), fontSize = 9.sp, color = Color.DarkGray)

                            Spacer(modifier = Modifier.height(12.dp))

                            // Bank Details
                            if (company?.bankName?.isNotBlank() == true) {
                                Text("BANK & PAYMENT DETAILS:", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                Text("Bank: ${company?.bankName} | A/C: ${company?.accountNumber} | IFSC: ${company?.ifsc} | UPI: ${company?.upiId}", fontSize = 8.5.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Signatures & Stamp Section (Architectural Style includes dual stamps)
                            if (isArchitectural) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .width(130.dp)
                                                .height(44.dp)
                                                .border(0.5.dp, Color.LightGray, RoundedCornerShape(2.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("CLIENT ACCEPTANCE", fontSize = 7.5.sp, color = Color.Gray)
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text("Authorized Signature", fontSize = 8.sp, color = Color.DarkGray)
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .width(130.dp)
                                                .height(44.dp)
                                                .border(0.5.dp, accentColor, RoundedCornerShape(2.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("PRINCIPAL ARCHITECT", fontSize = 7.5.sp, color = accentColor, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(company?.signatureName ?: "Authorized Signatory", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        HorizontalDivider(modifier = Modifier.width(150.dp), thickness = 0.5.dp, color = Color.Black)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(company?.signatureName ?: "Authorized Signatory", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                        Text(company?.name ?: "", fontSize = 8.5.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // WhatsApp Message Dialog
    if (showWhatsAppDialog) {
        val defaultMsg = remember {
            ShareHelper.generateWhatsAppMessage(q, calc.grandTotal, company, client, project)
        }
        var messageText by remember { mutableStateOf(defaultMsg) }

        Dialog(onDismissRequest = { showWhatsAppDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = EmeraldSuccess)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share via WhatsApp", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Text("Client: ${client?.name ?: "N/A"} (${client?.mobile ?: "No number"})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("Message Preview") },
                        modifier = Modifier.fillMaxWidth().height(150.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showWhatsAppDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val file = generatedPdfFile
                                ShareHelper.shareViaWhatsApp(context, file, messageText, client?.mobile)
                                showWhatsAppDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                        ) {
                            Text("Open WhatsApp")
                        }
                    }
                }
            }
        }
    }
}
