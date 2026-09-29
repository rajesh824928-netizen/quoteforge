package com.example.domain.engine

import com.example.data.model.QuotationItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AiAssistantEngine {

    /**
     * Generates a draft list of quotation items based on natural language prompt
     * e.g. "Create a quotation for a 10 ft kitchen with BWP plywood, acrylic finish and soft-close hardware."
     */
    suspend fun generateItemsFromPrompt(prompt: String): List<QuotationItemEntity> = withContext(Dispatchers.IO) {
        val apiKey = try {
            com.example.BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val apiResult = callGeminiForItems(apiKey, prompt)
                if (apiResult.isNotEmpty()) return@withContext apiResult
            } catch (e: Exception) {
                // Fallback to local rule engine
            }
        }

        // High quality architectural domain heuristic fallback
        return@withContext generateLocalEstimateItems(prompt)
    }

    /**
     * Generates professional client-facing descriptions in 4 styles:
     * Professional, Premium, Technical, Simple
     */
    suspend fun generateDescription(
        itemName: String,
        rawSpec: String,
        style: String = "Professional"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            com.example.BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = "Generate a single-sentence client-facing quotation description in '$style' style for architectural item '$itemName' with base spec '$rawSpec'. Return only the clean description text without markdown."
                val text = callGeminiRawText(apiKey, prompt)
                if (text.isNotBlank()) return@withContext text.trim()
            } catch (e: Exception) {
                // local fallback
            }
        }

        val baseSpec = if (rawSpec.isNotBlank()) rawSpec else "18mm BWP grade marine plywood"
        return@withContext when (style.lowercase()) {
            "premium" -> "Exclusively crafted $itemName utilizing luxury $baseSpec, featuring seamless edge-binding and refined bespoke Italian hardware detailing."
            "technical" -> "Precision-engineered $itemName: manufactured with calibrated $baseSpec conforming to IS:710 standards, mounted with heavy-duty architectural fittings."
            "simple" -> "Custom-built $itemName made of durable $baseSpec with smooth hardware finish, sized to site dimensions."
            else -> "High-grade $itemName constructed with $baseSpec, premium surface treatment, and soft-close hardware fitment."
        }
    }

    /**
     * Generates terms and conditions clauses for specified scope
     */
    fun generateTerms(scopeType: String): String {
        return when (scopeType.lowercase()) {
            "payment" -> """
• 50% mobilization advance upon contract sign-off.
• 40% upon dispatch of materials from manufacturing unit.
• 10% upon final punchlist clearance and handover.
• Payment dues must be cleared within 3 business days of milestone invoice.
            """.trimIndent()
            "warranty" -> """
• 10-Year warranty against delamination and borer/termite attacks on BWP marine plywood.
• 5-Year manufacturer warranty on mechanical soft-close hinges and drawer slides.
• 1-Year complimentary service warranty covering alignment and hardware tune-ups.
• Warranty excludes damage resulting from water logging beyond specified grades or physical misuse.
            """.trimIndent()
            "exclusions" -> """
• Electrical appliances (chimney, hob, microwave, oven) unless explicitly mentioned in bill of quantities.
• Main civil masonry modifications, beam alterations, or major structural chiselling.
• Deep cleaning and debris haulage beyond building municipal dump points.
• Water and power tariff costs incurred during construction to be borne by client.
            """.trimIndent()
            else -> """
• All measurements subject to final laser measurement verification at site.
• Quotation validity: 30 days from date of generation.
• Prevailing GST rates applicable at the time of milestone billing.
            """.trimIndent()
        }
    }

    private fun generateLocalEstimateItems(prompt: String): List<QuotationItemEntity> {
        val lower = prompt.lowercase()
        val items = mutableListOf<QuotationItemEntity>()

        if (lower.contains("kitchen")) {
            val lengthFt = extractNumber(lower, "ft") ?: 10.0
            val isAcrylic = lower.contains("acrylic")
            val isPlywood = lower.contains("bwp") || lower.contains("plywood")
            val spec = if (isPlywood) "18mm BWP marine plywood carcass (IS:710)" else "18mm HDHMR carcass"
            val finish = if (isAcrylic) "2mm high-gloss anti-scratch acrylic" else "1mm textured laminate"

            items.add(
                QuotationItemEntity(
                    quotationId = 0,
                    roomName = "Modular Kitchen",
                    category = "Modular Kitchen",
                    itemName = "Base Storage Cabinets ($lengthFt ft run)",
                    description = "$spec with $finish shutters, interior off-white laminate, and soft-close tandem drawers.",
                    specification = "$spec, soft-close hardware",
                    unit = "sq.ft",
                    quantity = lengthFt * 2.8,
                    rate = if (isAcrylic) 1950.0 else 1650.0
                )
            )
            items.add(
                QuotationItemEntity(
                    quotationId = 0,
                    roomName = "Modular Kitchen",
                    category = "Modular Kitchen",
                    itemName = "Wall Overhead Cabinets ($lengthFt ft run)",
                    description = "Upper storage cabinets with lift-up gas struts and $finish facade.",
                    specification = "18mm BWP carcass, Blum Aventos lift-ups",
                    unit = "sq.ft",
                    quantity = lengthFt * 2.0,
                    rate = if (isAcrylic) 1800.0 else 1500.0
                )
            )
            items.add(
                QuotationItemEntity(
                    quotationId = 0,
                    roomName = "Modular Kitchen",
                    category = "Modular Kitchen",
                    itemName = "Countertop & Quartz Scribing",
                    description = "18mm premium quartz stone with bevelled edge moulding and undermount sink cutout.",
                    specification = "Kalinga / Caesarstone Quartz",
                    unit = "r.ft",
                    quantity = lengthFt,
                    rate = 2600.0
                )
            )
        } else if (lower.contains("wardrobe")) {
            val width = extractNumber(lower, "width") ?: extractNumber(lower, "ft") ?: 10.0
            val height = 8.0
            val sqft = width * height

            items.add(
                QuotationItemEntity(
                    quotationId = 0,
                    roomName = "Master Bedroom",
                    category = "Wardrobes",
                    itemName = "Floor-to-Ceiling Wardrobe ($width' × $height')",
                    description = "18mm BWP marine plywood carcass with 1mm laminate, soft-close hinges, internal drawers and LED hanging rod.",
                    specification = "BWP Marine Grade, Hafele hardware",
                    unit = "sq.ft",
                    quantity = sqft,
                    rate = 1450.0
                )
            )
            items.add(
                QuotationItemEntity(
                    quotationId = 0,
                    roomName = "Master Bedroom",
                    category = "Wardrobes",
                    itemName = "Overhead Loft Storage",
                    description = "Overhead storage with matching shutters and hydraulic stays.",
                    specification = "18mm carcass, 1mm laminate",
                    unit = "sq.ft",
                    quantity = width * 2.0,
                    rate = 1200.0
                )
            )
        } else if (lower.contains("ceiling")) {
            val area = extractNumber(lower, "sq") ?: 400.0
            items.add(
                QuotationItemEntity(
                    quotationId = 0,
                    roomName = "Living Room",
                    category = "False Ceiling",
                    itemName = "Gypsum Perimeter Cove False Ceiling",
                    description = "Saint-Gobain Gypboard with GI channel framework and cove lighting channel.",
                    specification = "Saint-Gobain Gyproc 12.5mm",
                    unit = "sq.ft",
                    quantity = area,
                    rate = 135.0
                )
            )
        } else {
            // General estimated item
            items.add(
                QuotationItemEntity(
                    quotationId = 0,
                    roomName = "General",
                    category = "Interior Works",
                    itemName = "Custom Architectural Fitout Item",
                    description = "Bespoke fabrication as per prompt: $prompt",
                    specification = "Standard ISI certified materials & execution",
                    unit = "lump sum",
                    quantity = 1.0,
                    rate = 45000.0
                )
            )
        }

        return items
    }

    private fun extractNumber(text: String, keyword: String): Double? {
        val regex = Regex("(\\d+(\\.\\d+)?)\\s*$keyword")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun callGeminiRawText(apiKey: String, prompt: String): String {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.doOutput = true

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            }
            put("contents", contents)
        }

        OutputStreamWriter(conn.outputStream).use { it.write(jsonBody.toString()) }

        val responseCode = conn.responseCode
        if (responseCode == 200) {
            val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val obj = JSONObject(response)
            val candidates = obj.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            return parts?.optJSONObject(0)?.optString("text", "") ?: ""
        }
        return ""
    }

    private fun callGeminiForItems(apiKey: String, prompt: String): List<QuotationItemEntity> {
        val systemPrompt = "You are an expert quantity surveyor and architectural estimator in India. Given the user request, return a JSON array of items with properties: category (string), itemName (string), description (string), specification (string), unit (string: sq.ft, r.ft, nos, sq.m), quantity (number), rate (number in INR). Do not wrap in markdown or backticks."
        val fullPrompt = "$systemPrompt\n\nUser request: $prompt"
        val response = callGeminiRawText(apiKey, fullPrompt)

        val cleanJson = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val array = JSONArray(cleanJson)
        val result = mutableListOf<QuotationItemEntity>()

        for (i in 0 until array.length()) {
            val itemObj = array.getJSONObject(i)
            result.add(
                QuotationItemEntity(
                    quotationId = 0,
                    roomName = "General",
                    category = itemObj.optString("category", "Interior"),
                    itemName = itemObj.optString("itemName", "Item"),
                    description = itemObj.optString("description", ""),
                    specification = itemObj.optString("specification", ""),
                    unit = itemObj.optString("unit", "nos"),
                    quantity = itemObj.optDouble("quantity", 1.0),
                    rate = itemObj.optDouble("rate", 1000.0)
                )
            )
        }
        return result
    }
}
