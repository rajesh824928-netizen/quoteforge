package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.ShareHelper
import com.example.ui.components.QuoteForgeAppThumbnailCard
import com.example.ui.theme.AmberDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LuxuryGold
import com.example.ui.viewmodel.AppThemeMode
import com.example.ui.viewmodel.QuotationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: QuotationViewModel,
    onNavigateToWorkspaces: () -> Unit,
    onNavigateToGoogleDrive: () -> Unit,
    onNavigateToPdfStudio: () -> Unit,
    onNavigateToActivityLogs: () -> Unit,
    onNavigateToCompanies: () -> Unit,
    onNavigateToMaterials: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onNavigateToLanding: () -> Unit,
    onNavigateToChangelog: () -> Unit
) {
    val context = LocalContext.current
    val currentWorkspace by viewModel.currentWorkspace.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val userRole by viewModel.currentUserRole.collectAsState()
    val driveConn by viewModel.driveConnection.collectAsState()
    val defaultCompany by viewModel.defaultCompany.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    var showWhatsNewDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More & SaaS Platform", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Official App Thumbnail Card
            item {
                QuoteForgeAppThumbnailCard(
                    modifier = Modifier.fillMaxWidth().testTag("app_thumbnail_card"),
                    onFeatureClick = { feature ->
                        when (feature) {
                            "quotations" -> { /* Already on bottom nav */ }
                            "team" -> onNavigateToWorkspaces()
                            "drive" -> onNavigateToGoogleDrive()
                            "analytics" -> onNavigateToPdfStudio()
                        }
                    }
                )
            }

            // Material UI 3 Appearance & Theme Mode Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("appearance_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (themeMode) {
                                        AppThemeMode.LIGHT -> Icons.Outlined.LightMode
                                        AppThemeMode.DARK -> Icons.Outlined.DarkMode
                                        AppThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Display Theme", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Material 3 Light, Dark, or System Sync", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    themeMode.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = themeMode == AppThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) },
                                label = { Text("Light", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.weight(1f).testTag("theme_chip_light")
                            )

                            FilterChip(
                                selected = themeMode == AppThemeMode.DARK,
                                onClick = { viewModel.setThemeMode(AppThemeMode.DARK) },
                                label = { Text("Dark", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.weight(1f).testTag("theme_chip_dark")
                            )

                            FilterChip(
                                selected = themeMode == AppThemeMode.SYSTEM,
                                onClick = { viewModel.setThemeMode(AppThemeMode.SYSTEM) },
                                label = { Text("System", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.BrightnessAuto, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.weight(1f).testTag("theme_chip_system")
                            )
                        }
                    }
                }
            }
            // Workspace & User Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToWorkspaces() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (currentWorkspace?.name ?: "KA").take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("ACTIVE WORKSPACE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    currentWorkspace?.name ?: "Knot Architects",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "${currentWorkspace?.companyName ?: "Architecture Studio"} • Role: $userRole",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // User Account Card
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("card_user_account")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(currentUser?.displayName ?: "Architect", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    "${currentUser?.email ?: "Active User"} • ${currentUser?.authProvider?.uppercase() ?: "LOCAL"}",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.authManager.signOut()
                                onNavigateToLanding()
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Switch Account", fontSize = 11.sp)
                        }
                    }
                }
            }

            item {
                Text("SAAS WORKSPACE & CLOUD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            item {
                MoreMenuCard(
                    title = "Workspaces & Team Members",
                    subtitle = "Switch workspaces, invite team members & configure permissions",
                    icon = Icons.Outlined.Group,
                    onClick = onNavigateToWorkspaces,
                    testTag = "more_workspaces_card"
                )
            }

            item {
                MoreMenuCard(
                    title = "Google Drive Cloud Storage",
                    subtitle = if (driveConn?.isConnected == true) "Connected: ${driveConn?.accountEmail}" else "Connect Drive for automatic PDF syncing & backups",
                    icon = Icons.Outlined.CloudQueue,
                    iconTint = if (driveConn?.isConnected == true) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToGoogleDrive,
                    testTag = "more_drive_card"
                )
            }

            item {
                MoreMenuCard(
                    title = "PDF Design Studio & Brand Kit",
                    subtitle = "3 Professional styles, brand accent colors & custom watermarks",
                    icon = Icons.Outlined.Palette,
                    onClick = onNavigateToPdfStudio,
                    testTag = "more_pdf_studio_card"
                )
            }

            item {
                MoreMenuCard(
                    title = "Workspace Activity & Audit Trail",
                    subtitle = "Log of all quotation revisions, PDF generations & Drive syncs",
                    icon = Icons.Outlined.History,
                    onClick = onNavigateToActivityLogs,
                    testTag = "more_activity_logs_card"
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("BUSINESS & CONFIGURATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            item {
                MoreMenuCard(
                    title = "Company Profiles & Letterhead",
                    subtitle = "Manage branding, addresses, bank details, UPI & signatures",
                    icon = Icons.Outlined.Business,
                    onClick = onNavigateToCompanies,
                    testTag = "more_companies_card"
                )
            }

            item {
                MoreMenuCard(
                    title = "Material & Rate Library",
                    subtitle = "Manage plywood, laminates, hardware, labour rates & CSV export",
                    icon = Icons.Outlined.Inventory2,
                    onClick = onNavigateToMaterials,
                    testTag = "more_materials_card"
                )
            }

            item {
                MoreMenuCard(
                    title = "Quotation Numbering & Settings",
                    subtitle = "Customize document numbering prefixes, sequence digits & demo data",
                    icon = Icons.Outlined.Settings,
                    onClick = onNavigateToSettings,
                    testTag = "more_settings_card"
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("INTELLIGENCE & TOOLS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            item {
                MoreMenuCard(
                    title = "AI Quotation Assistant",
                    subtitle = "Draft instant estimates from plain text architectural descriptions",
                    icon = Icons.Default.AutoAwesome,
                    iconTint = AmberDark,
                    onClick = onOpenAiAssistant,
                    testTag = "more_ai_assistant_card"
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("APP DISTRIBUTION & RELEASE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            // Quick Share APK Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val success = ShareHelper.shareApkViaQuickShare(context)
                            if (success) {
                                Toast.makeText(context, "Opening Quick Share for QuoteForge APK...", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .testTag("more_quick_share_card"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LuxuryGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, tint = LuxuryGold)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Share APK via Quick Share", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = EmeraldSuccess.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("OFFLINE", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                                Text("Direct peer-to-peer APK sharing for on-site team & contractors", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Version Changelogs Card with explicit 'View Changelog' button
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("more_changelog_card"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.HistoryEdu,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Version Changelogs & Releases", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = LuxuryGold,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "v2.2.0",
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    "Feature history, release timeline & offline APK specs",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onNavigateToChangelog,
                                modifier = Modifier.testTag("btn_full_timeline")
                            ) {
                                Text("Full Timeline", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showWhatsNewDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_view_changelog")
                            ) {
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View Changelog", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // About QuoteForge Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("QuoteForge SaaS v2.2.0", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = LuxuryGold,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("BUILD 22", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            "Multi-tenant quotation & estimation platform for architecture, interior design and construction businesses.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showWhatsNewDialog = true },
                            modifier = Modifier.testTag("btn_about_view_changelog")
                        ) {
                            Icon(Icons.Outlined.HistoryEdu, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Version History & Features", fontSize = 11.5.sp)
                        }
                        Text(
                            "Developer: knotarchitects@gmail.com",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
fun MoreMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
