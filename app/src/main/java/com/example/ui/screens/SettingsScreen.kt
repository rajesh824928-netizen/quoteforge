package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.model.NumberingConfigEntity
import com.example.domain.engine.NumberingRule
import com.example.domain.engine.QuotationNumberGenerator
import com.example.ui.theme.RoseError
import com.example.ui.viewmodel.QuotationViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: QuotationViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val config by viewModel.numberingConfig.collectAsState()

    var prefix by remember(config) { mutableStateOf(config?.prefix ?: "KA") }
    var includeYear by remember(config) { mutableStateOf(config?.includeYear ?: true) }
    var includeMonth by remember(config) { mutableStateOf(config?.includeMonth ?: false) }
    var digits by remember(config) { mutableStateOf((config?.digits ?: 3).toString()) }
    var nextSeq by remember(config) { mutableStateOf((config?.nextSequence ?: 1).toString()) }

    val sampleNumber = remember(prefix, includeYear, includeMonth, digits, nextSeq) {
        val rule = NumberingRule(
            prefix = prefix,
            includeYear = includeYear,
            includeMonth = includeMonth,
            digits = digits.toIntOrNull() ?: 3
        )
        QuotationNumberGenerator.generate(rule, nextSeq.toIntOrNull() ?: 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Numbering", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
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
            // Quotation Numbering Configuration
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "QUOTATION NUMBERING FORMAT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Preview box
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Next Generated Number:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(sampleNumber, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = prefix,
                            onValueChange = { prefix = it },
                            label = { Text("Prefix (e.g. KA, KNOT, QT)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = digits,
                                onValueChange = { digits = it },
                                label = { Text("Sequence Digits (e.g. 3)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = nextSeq,
                                onValueChange = { nextSeq = it },
                                label = { Text("Next Sequence") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Include Current Year (e.g. 2026)")
                            Switch(checked = includeYear, onCheckedChange = { includeYear = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Include Current Month (e.g. 09)")
                            Switch(checked = includeMonth, onCheckedChange = { includeMonth = it })
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.saveNumberingConfig(
                                    NumberingConfigEntity(
                                        id = 1,
                                        prefix = prefix.trim(),
                                        includeYear = includeYear,
                                        includeMonth = includeMonth,
                                        digits = digits.toIntOrNull() ?: 3,
                                        nextSequence = nextSeq.toIntOrNull() ?: 1
                                    )
                                )
                                Toast.makeText(context, "Numbering format saved", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Format")
                        }
                    }
                }
            }

            // Developer / Demo Sample Data
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DEMO & SAMPLE DATA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Reload official demo dataset: Knot Architects company profile, Mr. Suresh Babu, Villa Interior project, and standard ₹ rates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val db = AppDatabase.getDatabase(context, this)
                                    AppDatabase.seedInitialData(db)
                                    Toast.makeText(context, "Sample data reloaded successfully!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("reload_sample_data_button")
                        ) {
                            Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reload Demo Sample Data")
                        }
                    }
                }
            }

            // App Identity & Info
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("QuoteForge", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Architectural & Interior Quotation Generator", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Version 1.0 • Offline-First Native Engine", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
