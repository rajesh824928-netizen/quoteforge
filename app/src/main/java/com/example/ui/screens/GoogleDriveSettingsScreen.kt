package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.DriveFileEntity
import com.example.ui.viewmodel.QuotationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleDriveSettingsScreen(
    viewModel: QuotationViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val driveConn by viewModel.driveConnection.collectAsState()
    val driveFiles by viewModel.driveFiles.collectAsState()
    val syncInProgress by viewModel.syncInProgress.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    var showConnectDialog by remember { mutableStateOf(false) }
    var connectEmail by remember(currentUser) { mutableStateOf(currentUser?.email.orEmpty()) }
    var connectAccountName by remember(currentUser) {
        mutableStateOf(if (currentUser?.displayName.isNullOrBlank()) "Primary Google Drive" else "${currentUser?.displayName}'s Drive")
    }
    var connectDriveType by remember { mutableStateOf("MY_DRIVE") }
    var connectFolderName by remember { mutableStateOf("QuotationApp") }

    val isConnected = driveConn?.isConnected == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Google Drive & Cloud", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Connection Status Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConnected) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) Color(0xFFDCFCE7) else Color.LightGray.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.CloudQueue,
                                        contentDescription = null,
                                        tint = if (isConnected) Color(0xFF16A34A) else Color.DarkGray
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        if (isConnected) "Google Drive Connected" else "Google Drive Disconnected",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isConnected) Color(0xFF15803D) else Color.Black
                                    )
                                    Text(
                                        if (isConnected) driveConn?.accountEmail?.ifBlank { "Connected" } ?: "" else "Sync PDFs directly to your Google Workspace",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) Color(0xFF22C55E) else Color.Gray)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isConnected) {
                                Button(
                                    onClick = { viewModel.syncDriveNow() },
                                    enabled = !syncInProgress,
                                    modifier = Modifier.weight(1f).testTag("sync_now_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (syncInProgress) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Syncing...", fontSize = 12.sp)
                                    } else {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sync Now", fontSize = 12.sp)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { viewModel.disconnectGoogleDrive() },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Disconnect", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            } else {
                                Button(
                                    onClick = { showConnectDialog = true },
                                    modifier = Modifier.fillMaxWidth().testTag("connect_drive_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Connect Google Drive")
                                }
                            }
                        }

                        syncMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(msg, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Sync Settings & Preferences
            item {
                Text("CLOUD CONFIGURATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Save Generated PDFs", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Automatically upload every generated quotation PDF to Drive", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = driveConn?.autoSavePdf == true,
                                onCheckedChange = { viewModel.toggleDriveAutoSave(it) },
                                modifier = Modifier.testTag("auto_save_switch")
                            )
                        }

                        HorizontalDivider()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Storage Location", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(if (driveConn?.driveType == "SHARED_DRIVE") "Shared Drive (Team Drive)" else "My Drive (Individual)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            AssistChip(
                                onClick = { /* Toggle */ },
                                label = { Text(driveConn?.driveType ?: "MY_DRIVE", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Dedicated Folder Hierarchy Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dedicated Drive Folder Structure", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("• QuotationApp/", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("   ├── Quotations/       (Client proposals & revisions)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("   ├── Estimates/        (Internal cost calculations)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("   ├── Projects/         (Client drawing packs & specs)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("   └── Backups/          (Periodic workspace archives)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Workspace Backup Exporter
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Workspace Backup (ZIP)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Export full JSON database + CSV clients & materials package", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                viewModel.exportWorkspaceBackup { zipFile ->
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Workspace Backup ZIP"))
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export ZIP", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Recent Synced Documents
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RECENT DRIVE FILES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("${driveFiles.size} files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (driveFiles.isEmpty()) {
                item {
                    Text(
                        "No documents synced to Drive yet. Generate or export a quotation to sync.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(driveFiles) { file ->
                    DriveFileItem(file = file)
                }
            }
        }
    }

    if (showConnectDialog) {
        AlertDialog(
            onDismissRequest = { showConnectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Google Drive", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "QuoteForge connects via Google Drive's secure 'drive.file' scope. It ONLY creates and accesses its own quotation documents and folder backups, keeping your personal Drive files completely private.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = connectEmail,
                        onValueChange = { connectEmail = it },
                        label = { Text("Google Account Email") },
                        placeholder = { Text("e.g. yourname@gmail.com") },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("drive_connect_email")
                    )
                    OutlinedTextField(
                        value = connectAccountName,
                        onValueChange = { connectAccountName = it },
                        label = { Text("Drive Display Name") },
                        placeholder = { Text("e.g. Studio Google Drive") },
                        leadingIcon = { Icon(Icons.Outlined.FolderShared, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("drive_connect_name")
                    )
                    OutlinedTextField(
                        value = connectFolderName,
                        onValueChange = { connectFolderName = it },
                        label = { Text("Root Backup Folder Name") },
                        placeholder = { Text("QuotationApp") },
                        leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("drive_connect_folder")
                    )
                    Text("Storage Target:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = connectDriveType == "MY_DRIVE",
                            onClick = { connectDriveType = "MY_DRIVE" },
                            label = { Text("My Drive") }
                        )
                        FilterChip(
                            selected = connectDriveType == "SHARED_DRIVE",
                            onClick = { connectDriveType = "SHARED_DRIVE" },
                            label = { Text("Shared Team Drive") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val emailToConnect = connectEmail.trim()
                        if (emailToConnect.isBlank() || !emailToConnect.contains("@")) {
                            Toast.makeText(context, "Please enter a valid Google Account email", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.connectGoogleDrive(
                            email = emailToConnect,
                            accountName = connectAccountName.trim().ifBlank { "Google Drive" },
                            driveType = connectDriveType,
                            folderName = connectFolderName.trim().ifBlank { "QuotationApp" }
                        )
                        showConnectDialog = false
                        Toast.makeText(context, "Google Drive linked to $emailToConnect", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_authorize_connect")
                ) {
                    Text("Authorize & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DriveFileItem(file: DriveFileEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(file.fileName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(file.uploadedAt))
                    Text("${file.driveFolderId} • $dateStr", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (file.syncState == "SYNCED") Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
            ) {
                Text(
                    file.syncState,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (file.syncState == "SYNCED") Color(0xFF15803D) else Color(0xFFB45309),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
