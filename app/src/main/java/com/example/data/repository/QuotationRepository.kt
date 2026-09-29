package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.example.domain.engine.CalculationEngine
import com.example.domain.engine.NumberingRule
import com.example.domain.engine.QuotationNumberGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class QuotationRepository(
    private val quotationDao: QuotationDao,
    private val quotationItemDao: QuotationItemDao,
    private val clientDao: ClientDao,
    private val projectDao: ProjectDao,
    private val companyDao: CompanyDao,
    private val numberingConfigDao: NumberingConfigDao
) {
    val allQuotations: Flow<List<QuotationEntity>> = quotationDao.getAllQuotations()
    val allCompanies: Flow<List<CompanyEntity>> = companyDao.getAllCompanies()
    val defaultCompany: Flow<CompanyEntity?> = companyDao.getDefaultCompany()
    val allClients: Flow<List<ClientEntity>> = clientDao.getAllClients()
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val numberingConfig: Flow<NumberingConfigEntity?> = numberingConfigDao.getConfig()

    fun getQuotationsForWorkspace(workspaceId: String): Flow<List<QuotationEntity>> =
        quotationDao.getQuotationsForWorkspace(workspaceId)

    fun getClientsForWorkspace(workspaceId: String): Flow<List<ClientEntity>> =
        clientDao.getClientsForWorkspace(workspaceId)

    fun getProjectsForWorkspace(workspaceId: String): Flow<List<ProjectEntity>> =
        projectDao.getProjectsForWorkspace(workspaceId)

    fun getCompaniesForWorkspace(workspaceId: String): Flow<List<CompanyEntity>> =
        companyDao.getCompaniesForWorkspace(workspaceId)


    fun getItemsForQuotation(quotationId: Long): Flow<List<QuotationItemEntity>> =
        quotationItemDao.getItemsForQuotation(quotationId)

    suspend fun getQuotationById(id: Long): QuotationEntity? =
        quotationDao.getQuotationById(id)

    fun observeQuotationById(id: Long): Flow<QuotationEntity?> =
        quotationDao.observeQuotationById(id)

    suspend fun getCompanyById(id: Long): CompanyEntity? =
        companyDao.getCompanyById(id)

    suspend fun getClientById(id: Long): ClientEntity? =
        clientDao.getClientById(id)

    suspend fun getProjectById(id: Long): ProjectEntity? =
        projectDao.getProjectById(id)

    suspend fun saveQuotation(
        quotation: QuotationEntity,
        items: List<QuotationItemEntity>
    ): Long {
        val qId = if (quotation.id == 0L) {
            quotationDao.insertQuotation(quotation)
        } else {
            quotationDao.updateQuotation(quotation.copy(updatedAt = System.currentTimeMillis()))
            quotation.id
        }

        // Delete old items and insert updated items
        quotationItemDao.deleteItemsForQuotation(qId)
        val itemsWithId = items.mapIndexed { idx, itm ->
            itm.copy(id = 0, quotationId = qId, sortOrder = idx + 1)
        }
        quotationItemDao.insertItems(itemsWithId)
        return qId
    }

    suspend fun updateQuotationStatus(id: Long, status: String) {
        quotationDao.updateStatus(id, status)
    }

    suspend fun deleteQuotation(quotation: QuotationEntity) {
        quotationItemDao.deleteItemsForQuotation(quotation.id)
        quotationDao.deleteQuotation(quotation)
    }

    /**
     * Generates the next quotation number based on numbering config and increments sequence
     */
    suspend fun generateNextQuotationNumber(): String {
        val config = numberingConfigDao.getConfigSync() ?: NumberingConfigEntity()
        val rule = NumberingRule(
            prefix = config.prefix,
            includeYear = config.includeYear,
            includeMonth = config.includeMonth,
            digits = config.digits
        )
        val number = QuotationNumberGenerator.generate(rule, config.nextSequence)
        numberingConfigDao.saveConfig(config.copy(nextSequence = config.nextSequence + 1))
        return number
    }

    /**
     * Creates a new revision (e.g., QT-2026-001 Rev.01) without modifying the original quotation
     */
    suspend fun createRevision(originalId: Long): Long {
        val original = quotationDao.getQuotationById(originalId) ?: return 0L
        val originalItems = quotationItemDao.getItemsForQuotationSync(originalId)

        // Mark original as Revised if it was sent or accepted
        quotationDao.updateStatus(originalId, "Revised")

        val rootParentId = original.parentQuotationId ?: original.id
        val nextRevisionNum = original.revision + 1
        val baseNumber = original.quotationNumber.split(" Rev.")[0]
        val revisionNumber = QuotationNumberGenerator.formatWithRevision(baseNumber, nextRevisionNum)

        val revisionQuotation = original.copy(
            id = 0,
            quotationNumber = revisionNumber,
            revision = nextRevisionNum,
            parentQuotationId = rootParentId,
            status = "Draft",
            date = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val newId = quotationDao.insertQuotation(revisionQuotation)
        val copiedItems = originalItems.map { it.copy(id = 0, quotationId = newId) }
        quotationItemDao.insertItems(copiedItems)
        return newId
    }

    /**
     * Duplicates a quotation with a brand new quotation number and today's date
     */
    suspend fun duplicateQuotation(originalId: Long, newClientId: Long? = null): Long {
        val original = quotationDao.getQuotationById(originalId) ?: return 0L
        val originalItems = quotationItemDao.getItemsForQuotationSync(originalId)

        val nextNumber = generateNextQuotationNumber()
        val duplicated = original.copy(
            id = 0,
            quotationNumber = nextNumber,
            revision = 0,
            parentQuotationId = null,
            clientId = newClientId ?: original.clientId,
            status = "Draft",
            date = System.currentTimeMillis(),
            isInvoice = false,
            invoiceNumber = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val newId = quotationDao.insertQuotation(duplicated)
        val copiedItems = originalItems.map { it.copy(id = 0, quotationId = newId) }
        quotationItemDao.insertItems(copiedItems)
        return newId
    }

    /**
     * Converts an accepted quotation into an Invoice
     */
    suspend fun convertToInvoice(quotationId: Long): String {
        val quotation = quotationDao.getQuotationById(quotationId) ?: return ""
        val invoiceNo = "INV-${quotation.quotationNumber.replace("QT-", "").replace("KA-", "")}"
        val updated = quotation.copy(
            isInvoice = true,
            invoiceNumber = invoiceNo,
            status = "Accepted",
            updatedAt = System.currentTimeMillis()
        )
        quotationDao.updateQuotation(updated)
        return invoiceNo
    }

    // Client operations
    suspend fun saveClient(client: ClientEntity): Long {
        return if (client.id == 0L) {
            clientDao.insertClient(client)
        } else {
            clientDao.updateClient(client.copy(updatedAt = System.currentTimeMillis()))
            client.id
        }
    }

    suspend fun deleteClient(client: ClientEntity) {
        clientDao.deleteClient(client)
    }

    // Project operations
    suspend fun saveProject(project: ProjectEntity): Long {
        return if (project.id == 0L) {
            projectDao.insertProject(project)
        } else {
            projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
            project.id
        }
    }

    suspend fun deleteProject(project: ProjectEntity) {
        projectDao.deleteProject(project)
    }

    // Company operations
    suspend fun saveCompany(company: CompanyEntity): Long {
        return if (company.id == 0L) {
            companyDao.insertCompany(company)
        } else {
            companyDao.updateCompany(company.copy(updatedAt = System.currentTimeMillis()))
            company.id
        }
    }

    suspend fun setDefaultCompany(companyId: Long) {
        companyDao.setDefaultCompany(companyId)
    }

    suspend fun deleteCompany(company: CompanyEntity) {
        companyDao.deleteCompany(company)
    }

    // Numbering configuration
    suspend fun updateNumberingConfig(config: NumberingConfigEntity) {
        numberingConfigDao.saveConfig(config)
    }
}
