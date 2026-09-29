package com.example.data.repository

import com.example.data.local.MaterialDao
import com.example.data.local.RateDao
import com.example.data.local.TermPresetDao
import com.example.data.model.MaterialEntity
import com.example.data.model.RateEntity
import com.example.data.model.TermPresetEntity
import kotlinx.coroutines.flow.Flow

class MaterialRateRepository(
    private val materialDao: MaterialDao,
    private val rateDao: RateDao,
    private val termPresetDao: TermPresetDao
) {
    val allMaterials: Flow<List<MaterialEntity>> = materialDao.getAllMaterials()
    val allRates: Flow<List<RateEntity>> = rateDao.getAllRates()
    val allTermPresets: Flow<List<TermPresetEntity>> = termPresetDao.getAllTerms()

    suspend fun saveMaterial(material: MaterialEntity): Long {
        return if (material.id == 0L) {
            materialDao.insertMaterial(material)
        } else {
            materialDao.updateMaterial(material)
            material.id
        }
    }

    suspend fun deleteMaterial(material: MaterialEntity) {
        materialDao.deleteMaterial(material)
    }

    suspend fun saveRate(rate: RateEntity): Long {
        return if (rate.id == 0L) {
            rateDao.insertRate(rate)
        } else {
            rateDao.updateRate(rate.copy(updatedAt = System.currentTimeMillis()))
            rate.id
        }
    }

    suspend fun deleteRate(rate: RateEntity) {
        rateDao.deleteRate(rate)
    }

    suspend fun saveTermPreset(term: TermPresetEntity): Long {
        return termPresetDao.insertTerm(term)
    }

    /**
     * Bulk import rates from CSV string: Name,Category,Unit,Rate
     */
    suspend fun importRatesFromCsv(csvContent: String): Int {
        val lines = csvContent.lines()
        var count = 0
        val rates = mutableListOf<RateEntity>()

        for (line in lines) {
            val parts = line.split(",").map { it.trim() }
            if (parts.size >= 4 && parts[0].lowercase() != "item" && parts[0].lowercase() != "name") {
                val rateVal = parts[3].replace("₹", "").toDoubleOrNull() ?: continue
                rates.add(
                    RateEntity(
                        name = parts[0],
                        category = parts[1],
                        unit = parts[2],
                        rate = rateVal
                    )
                )
                count++
            }
        }

        if (rates.isNotEmpty()) {
            rateDao.insertRates(rates)
        }
        return count
    }

    /**
     * Export rates to CSV format
     */
    fun exportRatesToCsv(rates: List<RateEntity>): String {
        val sb = StringBuilder("Item Name,Category,Unit,Rate (INR)\n")
        rates.forEach { rate ->
            sb.append("\"${rate.name}\",\"${rate.category}\",\"${rate.unit}\",${rate.rate}\n")
        }
        return sb.toString()
    }
}
