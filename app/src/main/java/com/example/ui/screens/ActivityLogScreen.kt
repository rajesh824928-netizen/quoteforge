package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityLogEntity
import com.example.ui.viewmodel.QuotationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(
    viewModel: QuotationViewModel,
    onNavigateBack: () -> Unit
) {
    val logs by viewModel.activityLogs.collectAsState()
    val currentWorkspace by viewModel.currentWorkspace.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Workspace Audit Trail", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(currentWorkspace?.name ?: "Knot Architects", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No activities recorded yet", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs) { log ->
                    ActivityLogItem(log = log)
                }
            }
        }
    }
}

@Composable
fun ActivityLogItem(log: ActivityLogEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            log.action.contains("PDF", ignoreCase = true) -> Color(0xFFFEE2E2)
                            log.action.contains("Drive", ignoreCase = true) -> Color(0xFFDCFCE7)
                            log.action.contains("revision", ignoreCase = true) -> Color(0xFFFEF3C7)
                            log.action.contains("Created", ignoreCase = true) -> Color(0xFFE0E7FF)
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when {
                        log.action.contains("PDF", ignoreCase = true) -> Icons.Outlined.PictureAsPdf
                        log.action.contains("Drive", ignoreCase = true) -> Icons.Outlined.CloudUpload
                        log.action.contains("revision", ignoreCase = true) -> Icons.Outlined.ChangeCircle
                        log.action.contains("Created", ignoreCase = true) -> Icons.Outlined.AddCircleOutline
                        else -> Icons.Outlined.EventNote
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = when {
                        log.action.contains("PDF", ignoreCase = true) -> Color(0xFFDC2626)
                        log.action.contains("Drive", ignoreCase = true) -> Color(0xFF16A34A)
                        log.action.contains("revision", ignoreCase = true) -> Color(0xFFD97706)
                        log.action.contains("Created", ignoreCase = true) -> Color(0xFF4F46E5)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(log.action, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    val dateFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
                    Text(dateFormatted, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (log.documentNumber != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            log.documentNumber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(log.details, fontSize = 11.sp, color = Color.DarkGray)
                Text("by ${log.userName}", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        }
    }
}
