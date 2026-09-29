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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrandKitEntity
import com.example.ui.viewmodel.QuotationViewModel
import com.example.util.ImageUtils

data class ColorOption(val name: String, val hex: String)

val PRESET_ACCENTS = listOf(
    ColorOption("Luxury Gold", "#A3834C"),
    ColorOption("Teal Primary", "#0D5C58"),
    ColorOption("Arch Black", "#1F1F1F"),
    ColorOption("Slate", "#475569"),
    ColorOption("Navy Blue", "#1E3A5F"),
    ColorOption("Forest Green", "#285943"),
    ColorOption("Terracotta", "#A45135"),
    ColorOption("Deep Burgundy", "#6B2635")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfDesignStudioScreen(
    viewModel: QuotationViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentBrandKit by viewModel.currentBrandKit.collectAsState()
    val defaultCompany by viewModel.defaultCompany.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedStyle by remember { mutableStateOf("PREMIUM_ARCHITECTURAL") }
    var selectedAccentHex by remember { mutableStateOf("#A3834C") }
    var customHexInput by remember { mutableStateOf("") }
    var watermarkEnabled by remember { mutableStateOf(true) }
    var watermarkType by remember { mutableStateOf("TEXT") } // "TEXT", "LOGO"
    var watermarkText by remember { mutableStateOf("DRAFT") }
    var watermarkOpacity by remember { mutableStateOf(0.12f) }
    var watermarkLogoUri by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Synchronize initial values from BrandKit
    LaunchedEffect(currentBrandKit) {
        currentBrandKit?.let { kit ->
            selectedAccentHex = kit.primaryColor
            watermarkEnabled = kit.watermarkEnabled
            watermarkType = kit.watermarkType
            watermarkText = kit.watermarkText
            watermarkOpacity = kit.watermarkOpacity
            watermarkLogoUri = kit.logoUri ?: defaultCompany?.logoUri
        }
    }

    val activeLogoUri = watermarkLogoUri ?: defaultCompany?.logoUri
    val logoBitmap = remember(activeLogoUri) {
        activeLogoUri?.let { ImageUtils.loadBitmap(context, it) }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val saved = ImageUtils.copyUriToInternalStorage(context, it, "watermark_logo")
            if (saved != null) {
                watermarkLogoUri = saved
                watermarkType = "LOGO"
            }
        }
    }

    val parsedAccentColor = remember(selectedAccentHex) {
        try {
            Color(android.graphics.Color.parseColor(selectedAccentHex))
        } catch (e: Exception) {
            Color(0xFFA3834C)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
            snackbarMessage?.let { msg ->
                Snackbar(
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("OK")
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(msg)
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("PDF Design Studio", fontWeight = FontWeight.Bold) },
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
            // Live Interactive Mockup Card
            item {
                Text("DOCUMENT PREVIEW MOCKUP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        // Watermark Rendering: Logo or Text
                        if (watermarkEnabled) {
                            if (watermarkType == "LOGO" && logoBitmap != null) {
                                Image(
                                    bitmap = logoBitmap.asImageBitmap(),
                                    contentDescription = "Watermark Logo",
                                    modifier = Modifier
                                        .size(130.dp)
                                        .align(Alignment.Center)
                                        .alpha(watermarkOpacity.coerceIn(0.04f, 0.45f)),
                                    contentScale = ContentScale.Fit
                                )
                            } else if (watermarkText.isNotBlank()) {
                                Text(
                                    text = watermarkText.uppercase(),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Gray.copy(alpha = watermarkOpacity),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .rotate(-35f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Mockup Content based on selectedStyle
                        Column(modifier = Modifier.fillMaxSize()) {
                            when (selectedStyle) {
                                "MINIMAL_STUDIO" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (logoBitmap != null) {
                                                Image(
                                                    bitmap = logoBitmap.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)),
                                                    contentScale = ContentScale.Fit
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(defaultCompany?.name ?: "KNOT ARCHITECTS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = parsedAccentColor)
                                        }
                                        Text("PROPOSAL #KA-2026-001", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(thickness = 0.8.dp, color = Color.LightGray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                "PROFESSIONAL_CORPORATE" -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .background(parsedAccentColor, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (logoBitmap != null) {
                                                Image(
                                                    bitmap = logoBitmap.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)),
                                                    contentScale = ContentScale.Fit
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(defaultCompany?.name ?: "KNOT ARCHITECTS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = parsedAccentColor)
                                        }
                                        Surface(color = parsedAccentColor, shape = RoundedCornerShape(3.dp)) {
                                            Text("QUOTATION", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                else -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(28.dp)
                                                    .background(parsedAccentColor, RoundedCornerShape(2.dp))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            if (logoBitmap != null) {
                                                Image(
                                                    bitmap = logoBitmap.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)),
                                                    contentScale = ContentScale.Fit
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Column {
                                                Text(defaultCompany?.name ?: "KNOT ARCHITECTS & INTERIORS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = parsedAccentColor)
                                                Text("ARCHITECTURAL ESTIMATION & PROPOSAL", fontSize = 8.sp, color = Color.Gray)
                                            }
                                        }
                                        Text("REF: KA-2026-001", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            // Mock Table Rows
                            Surface(
                                color = parsedAccentColor.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ITEM / DESCRIPTION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = parsedAccentColor)
                                    Text("AMOUNT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = parsedAccentColor)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("False Ceiling Works (Gypsum)", fontSize = 8.5.sp, color = Color.Black)
                                Text("₹1,24,000", fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Italian Marble Flooring (Bottochino)", fontSize = 8.5.sp, color = Color.Black)
                                Text("₹3,85,000", fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Custom Modular Wardrobe", fontSize = 8.5.sp, color = Color.Black)
                                Text("₹2,10,000", fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Mock Total Box
                            Surface(
                                color = parsedAccentColor,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    "GRAND TOTAL: ₹7,19,000",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // PDF Style Selection
            item {
                Text("DOCUMENT TEMPLATE LAYOUT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StyleCard(
                        title = "Premium Architectural",
                        description = "Vertical accent pillar, spacious grid lines, signature block for design firms.",
                        styleCode = "PREMIUM_ARCHITECTURAL",
                        isSelected = selectedStyle == "PREMIUM_ARCHITECTURAL",
                        onSelect = { selectedStyle = "PREMIUM_ARCHITECTURAL" }
                    )
                    StyleCard(
                        title = "Minimal Studio",
                        description = "Clean Scandinavian lines, understated metadata, modern studio branding.",
                        styleCode = "MINIMAL_STUDIO",
                        isSelected = selectedStyle == "MINIMAL_STUDIO",
                        onSelect = { selectedStyle = "MINIMAL_STUDIO" }
                    )
                    StyleCard(
                        title = "Professional Corporate",
                        description = "Bold header banner, clear tabular contrast, structured commercial formatting.",
                        styleCode = "PROFESSIONAL_CORPORATE",
                        isSelected = selectedStyle == "PROFESSIONAL_CORPORATE",
                        onSelect = { selectedStyle = "PROFESSIONAL_CORPORATE" }
                    )
                }
            }

            // Brand Accent Color
            item {
                Text("BRAND ACCENT COLOR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(PRESET_ACCENTS) { opt ->
                        val isPicked = selectedAccentHex.equals(opt.hex, ignoreCase = true)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedAccentHex = opt.hex }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(opt.hex)))
                                    .border(
                                        width = if (isPicked) 3.dp else 1.dp,
                                        color = if (isPicked) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPicked) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(opt.name, fontSize = 9.sp, fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            // Watermark Controls
            item {
                Text("DOCUMENT WATERMARK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enable Watermark", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Subtly rendered in background across every quotation page", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = watermarkEnabled,
                                onCheckedChange = { watermarkEnabled = it },
                                modifier = Modifier.testTag("watermark_toggle")
                            )
                        }

                        if (watermarkEnabled) {
                            Text("Watermark Source:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = watermarkType == "LOGO",
                                    onClick = { watermarkType = "LOGO" },
                                    label = { Text("Brand Logo (Image)", fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                                FilterChip(
                                    selected = watermarkType == "TEXT",
                                    onClick = { watermarkType = "TEXT" },
                                    label = { Text("Text / Status", fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }

                            if (watermarkType == "LOGO") {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.White)
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (logoBitmap != null) {
                                                Image(
                                                    bitmap = logoBitmap.asImageBitmap(),
                                                    contentDescription = "Watermark Logo",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Outlined.ImageNotSupported,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                if (logoBitmap != null) "Logo Watermark Active" else "No Logo Attached",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                "Rendered at center of every page with transparency",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        photoPickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Upload Logo", fontSize = 10.sp)
                                                }

                                                if (defaultCompany?.logoUri != null && watermarkLogoUri != defaultCompany?.logoUri) {
                                                    FilledTonalButton(
                                                        onClick = {
                                                            watermarkLogoUri = defaultCompany?.logoUri
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Text("Use Company Logo", fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("Preset Status Text:", style = MaterialTheme.typography.labelSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("DRAFT", "CONFIDENTIAL", "REVISED", "SAMPLE").forEach { preset ->
                                        FilterChip(
                                            selected = watermarkText == preset,
                                            onClick = { watermarkText = preset },
                                            label = { Text(preset, fontSize = 10.sp) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = watermarkText,
                                    onValueChange = { watermarkText = it },
                                    label = { Text("Custom Watermark Text") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Text("Watermark Opacity (${(watermarkOpacity * 100).toInt()}%):", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = watermarkOpacity,
                                onValueChange = { watermarkOpacity = it },
                                valueRange = 0.05f..0.35f
                            )
                        }
                    }
                }
            }

            // Save Action
            item {
                Button(
                    onClick = {
                        val kit = (currentBrandKit ?: BrandKitEntity(workspaceId = viewModel.currentWorkspaceId.value)).copy(
                            primaryColor = selectedAccentHex,
                            watermarkEnabled = watermarkEnabled,
                            watermarkType = watermarkType,
                            watermarkText = watermarkText,
                            watermarkOpacity = watermarkOpacity,
                            logoUri = activeLogoUri
                        )
                        viewModel.saveBrandKit(kit)
                        snackbarMessage = "Brand kit and watermark settings saved!"
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_brand_kit_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Style to Workspace")
                }
            }
        }
    }
}

@Composable
fun StyleCard(
    title: String,
    description: String,
    styleCode: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
