package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CompanyEntity
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.viewmodel.QuotationViewModel
import com.example.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyProfileScreen(
    viewModel: QuotationViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val companies by viewModel.allCompanies.collectAsState()
    val brandKit by viewModel.currentBrandKit.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedCompany by remember { mutableStateOf<CompanyEntity?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Company Profiles", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("company_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedCompany = null
                    showEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_company_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Company")
            }
        },
        snackbarHost = {
            snackbarMessage?.let { msg ->
                Snackbar(
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("OK", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(msg)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Upload your company logo along with company name. The active company's logo and details are automatically rendered on PDF quotation headers and can be used as a document watermark.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(companies, key = { it.id }) { comp ->
                val logoBitmap = remember(comp.logoUri) {
                    comp.logoUri?.let { ImageUtils.loadBitmap(context, it) }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (comp.isDefault) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Logo or Monogram Avatar
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (logoBitmap != null) {
                                    Image(
                                        bitmap = logoBitmap.asImageBitmap(),
                                        contentDescription = "Logo of ${comp.name}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text(
                                        text = comp.name.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(comp.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (comp.isDefault) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = EmeraldSuccess.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "ACTIVE",
                                                color = EmeraldSuccess,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                if (comp.address.isNotBlank()) {
                                    Text(comp.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row {
                                IconButton(onClick = {
                                    selectedCompany = comp
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                                }
                                if (!comp.isDefault) {
                                    IconButton(onClick = { viewModel.setDefaultCompany(comp.id) }) {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Set Active", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (comp.gstin.isNotBlank() || comp.pan.isNotBlank()) {
                            Text("GSTIN: ${comp.gstin}  |  PAN: ${comp.pan}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (comp.bankName.isNotBlank()) {
                            Text("Bank: ${comp.bankName}  |  A/C: ${comp.accountNumber}  |  IFSC: ${comp.ifsc}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (comp.signatureName.isNotBlank()) {
                            Text("Signatory: ${comp.signatureName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        // Watermark integration shortcut if logo is uploaded
                        if (comp.logoUri != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    brandKit?.let { bk ->
                                        viewModel.saveBrandKit(
                                            bk.copy(
                                                logoUri = comp.logoUri,
                                                watermarkEnabled = true,
                                                watermarkType = "LOGO"
                                            )
                                        )
                                        snackbarMessage = "Company logo applied as PDF Watermark!"
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Outlined.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Use Logo as PDF Watermark", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        CompanyEditorDialog(
            company = selectedCompany,
            onDismiss = { showEditDialog = false },
            onSave = { c ->
                viewModel.saveCompany(c)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun CompanyEditorDialog(
    company: CompanyEntity?,
    onDismiss: () -> Unit,
    onSave: (CompanyEntity) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(company?.name ?: "") }
    var logoUri by remember { mutableStateOf(company?.logoUri) }
    var address by remember { mutableStateOf(company?.address ?: "") }
    var phone by remember { mutableStateOf(company?.phone ?: "") }
    var email by remember { mutableStateOf(company?.email ?: "") }
    var website by remember { mutableStateOf(company?.website ?: "") }
    var gstin by remember { mutableStateOf(company?.gstin ?: "") }
    var pan by remember { mutableStateOf(company?.pan ?: "") }
    var bankName by remember { mutableStateOf(company?.bankName ?: "") }
    var accountNumber by remember { mutableStateOf(company?.accountNumber ?: "") }
    var ifsc by remember { mutableStateOf(company?.ifsc ?: "") }
    var upiId by remember { mutableStateOf(company?.upiId ?: "") }
    var signatureName by remember { mutableStateOf(company?.signatureName ?: "") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val saved = ImageUtils.copyUriToInternalStorage(context, it, "company_logo")
            if (saved != null) {
                logoUri = saved
            }
        }
    }

    val previewBitmap = remember(logoUri) {
        logoUri?.let { ImageUtils.loadBitmap(context, it) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().height(620.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        if (company == null) "New Company Profile" else "Edit Company Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Company Logo Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (previewBitmap != null) {
                                    Image(
                                        bitmap = previewBitmap.asImageBitmap(),
                                        contentDescription = "Logo Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Icon(
                                        Icons.Outlined.AddPhotoAlternate,
                                        contentDescription = "Upload Logo",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Company Logo",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    if (logoUri != null) "Logo attached. Rendered on PDF letterhead & watermark." else "Upload brand logo (PNG / JPG)",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (logoUri == null) "Upload Logo" else "Change", fontSize = 11.sp)
                                    }

                                    if (logoUri != null) {
                                        TextButton(
                                            onClick = { logoUri = null },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Remove", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Company / Firm Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("company_name_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Office Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = gstin,
                            onValueChange = { gstin = it },
                            label = { Text("GSTIN") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pan,
                            onValueChange = { pan = it },
                            label = { Text("PAN") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text("Bank & Payment Details", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Bank Name") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it },
                            label = { Text("A/C Number") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ifsc,
                            onValueChange = { ifsc = it },
                            label = { Text("IFSC") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it },
                            label = { Text("UPI ID") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = signatureName,
                        onValueChange = { signatureName = it },
                        label = { Text("Authorized Signatory Text") },
                        placeholder = { Text("e.g. Ar. Rohit Varma (Principal Architect)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onSave(
                                        CompanyEntity(
                                            id = company?.id ?: 0,
                                            name = name.trim(),
                                            logoUri = logoUri,
                                            address = address.trim(),
                                            phone = phone.trim(),
                                            email = email.trim(),
                                            website = website.trim(),
                                            gstin = gstin.trim(),
                                            pan = pan.trim(),
                                            bankName = bankName.trim(),
                                            accountNumber = accountNumber.trim(),
                                            ifsc = ifsc.trim(),
                                            upiId = upiId.trim(),
                                            signatureName = signatureName.trim(),
                                            isDefault = company?.isDefault ?: false
                                        )
                                    )
                                }
                            }
                        ) {
                            Text("Save Company")
                        }
                    }
                }
            }
        }
    }
}
