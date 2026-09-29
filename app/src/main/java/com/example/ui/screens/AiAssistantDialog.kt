package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.QuotationItemEntity
import com.example.domain.engine.AiAssistantEngine
import com.example.domain.engine.CurrencyFormatter
import com.example.ui.theme.AmberDark
import com.example.ui.theme.EmeraldSuccess
import kotlinx.coroutines.launch

@Composable
fun AiAssistantDialog(
    onDismiss: () -> Unit,
    onAddItemsToQuotation: (List<QuotationItemEntity>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var userPrompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedItems by remember { mutableStateOf<List<QuotationItemEntity>>(emptyList()) }

    val samplePrompts = listOf(
        "10 ft modular kitchen with BWP marine plywood & acrylic finish shutters",
        "12 ft wide × 8 ft high sliding wardrobe in master bedroom",
        "Gypsum false ceiling with perimeter cove lighting for 450 sq.ft living room",
        "Full 3BHK turnkey interior woodwork and painting package"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 380.dp, max = 580.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("AI Quotation Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Describe items in plain words to generate estimates", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    placeholder = { Text("e.g. 10 ft kitchen with BWP plywood, acrylic finish and soft-close hardware...") },
                    modifier = Modifier.fillMaxWidth().height(90.dp).testTag("ai_prompt_input")
                )

                Text("Try quick prompt:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(samplePrompts) { prompt ->
                        SuggestionChip(
                            onClick = { userPrompt = prompt },
                            label = { Text(prompt.take(30) + "...", fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Close") }

                    Button(
                        onClick = {
                            if (userPrompt.isNotBlank()) {
                                isGenerating = true
                                coroutineScope.launch {
                                    val items = AiAssistantEngine.generateItemsFromPrompt(userPrompt)
                                    generatedItems = items
                                    isGenerating = false
                                }
                            }
                        },
                        enabled = !isGenerating && userPrompt.isNotBlank(),
                        modifier = Modifier.testTag("ai_generate_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Estimating...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Items")
                        }
                    }
                }

                if (generatedItems.isNotEmpty()) {
                    Divider()
                    Text("Generated Estimate Draft (${generatedItems.size} items):", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(generatedItems) { itm ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(itm.itemName, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        Text(
                                            CurrencyFormatter.formatInr(itm.quantity * itm.rate, false),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        "${itm.quantity} ${itm.unit} @ ${CurrencyFormatter.formatInr(itm.rate, false)}  •  ${itm.specification}",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onAddItemsToQuotation(generatedItems)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("ai_add_to_quote_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add ${generatedItems.size} Items to Current Quotation")
                    }
                }
            }
        }
    }
}
