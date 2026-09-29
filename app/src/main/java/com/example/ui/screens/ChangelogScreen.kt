package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.ChangelogRepository
import com.example.domain.engine.ReleaseVersion
import com.example.domain.engine.ShareHelper
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val releases = remember { ChangelogRepository.releases }
    var selectedVersionId by remember { mutableStateOf(releases.first().version) }
    val apkInfo = remember { ShareHelper.getApkInfo(context) }
    var showApkDetailsDialog by remember { mutableStateOf(false) }

    val currentRelease = releases.find { it.version == selectedVersionId } ?: releases.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Version Changelogs", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("QuoteForge v${apkInfo.versionName} • Release Notes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_changelog_back")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            val success = ShareHelper.shareApkViaQuickShare(context)
                            if (success) {
                                Toast.makeText(context, "Opening Quick Share for QuoteForge APK...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_top_quick_share_apk")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Quick Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero: Quick Share APK Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyDark),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_quick_share_hero")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(LuxuryGold.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Android,
                                        contentDescription = null,
                                        tint = LuxuryGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "QuoteForge APK",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = LuxuryGold,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "v${apkInfo.versionName}",
                                                color = Color.Black,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "${apkInfo.fileName} • ${apkInfo.fileSizeFormatted}",
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showApkDetailsDialog = true },
                                modifier = Modifier.testTag("btn_apk_info")
                            ) {
                                Icon(Icons.Outlined.Info, contentDescription = "APK Details", tint = Color.LightGray)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "Share QuoteForge directly with on-site architects, contractors, and site supervisors using Android Quick Share. No internet or app store required.",
                            fontSize = 12.5.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val success = ShareHelper.shareApkViaQuickShare(context)
                                    if (success) {
                                        Toast.makeText(context, "Opening Quick Share...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LuxuryGold,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_share_apk_quick_share")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Quick Share APK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    ShareHelper.shareApkGeneric(context, "Share QuoteForge APK")
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                modifier = Modifier.testTag("btn_share_apk_all_options")
                            ) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("All Options", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Version Selector Chips
            item {
                Column {
                    Text(
                        "RELEASE TIMELINE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(releases) { rel ->
                            val isSelected = rel.version == selectedVersionId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedVersionId = rel.version },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("v${rel.version}", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                        if (rel.isCurrent) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(EmeraldSuccess)
                                            )
                                        }
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("chip_version_${rel.version}")
                            )
                        }
                    }
                }
            }

            // Current Version Header Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Version ${currentRelease.version}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = if (currentRelease.isCurrent) EmeraldSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        currentRelease.highlightBadge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentRelease.isCurrent) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                currentRelease.releaseDate,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            currentRelease.headline,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            currentRelease.summary,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Categorized Features
            items(currentRelease.categories) { category ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                category.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val tagColor = when (category.tag) {
                                "NEW" -> EmeraldSuccess
                                "ENHANCEMENT" -> BlueAccent
                                else -> LuxuryGold
                            }

                            Surface(
                                color = tagColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    category.tag,
                                    color = tagColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            category.features.forEach { feature ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        feature,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Footer
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "QuoteForge SaaS Architectural Edition",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Developed for Knot Architects • knotarchitects@gmail.com",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // APK Details Dialog
    if (showApkDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showApkDetailsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Android, contentDescription = null, tint = LuxuryGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Package Information", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("File: ${apkInfo.fileName}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Package: ${apkInfo.packageId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Version: ${apkInfo.versionName} (Build ${apkInfo.versionCode})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Size: ${apkInfo.fileSizeFormatted}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Target Platform: Android 15+ (API 36)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "Quick Share allows zero-data peer-to-peer APK file transfer with nearby Samsung, Pixel, OnePlus, Xiaomi, and other Android devices.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApkDetailsDialog = false
                        ShareHelper.shareApkViaQuickShare(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = Color.Black)
                ) {
                    Text("Quick Share Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApkDetailsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
