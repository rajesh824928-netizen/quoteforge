package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

// ==========================================
// SAAS CORE: USER, WORKSPACE & MEMBERSHIP
// ==========================================

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String, // Google UID or local user ID
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val authProvider: String = "google", // google, email, apple, etc.
    val defaultWorkspaceId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String, // e.g. "ws_knot_arch"
    val name: String, // e.g. "Knot Architects"
    val companyName: String,
    val businessType: String = "Architecture", // Architecture, Interior Design, Construction, Furniture, Contractor, Other
    val country: String = "India",
    val currency: String = "INR",
    val currencySymbol: String = "₹",
    val logoUri: String? = null,
    val ownerUserId: String = "",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "workspace_members",
    indices = [Index("workspaceId"), Index("userId")]
)
data class WorkspaceMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val userPhotoUrl: String? = null,
    val role: String = "OWNER", // OWNER, ADMIN, MANAGER, DESIGNER, ACCOUNTANT, VIEWER
    val customPermissionsJson: String = "", // custom override permissions
    val status: String = "ACTIVE", // ACTIVE, PENDING, SUSPENDED
    val joinedAt: Long = System.currentTimeMillis()
)

// Permission model supporting fine-grained custom toggles
data class WorkspacePermissions(
    val viewClients: Boolean = true,
    val createClients: Boolean = true,
    val editClients: Boolean = true,
    val deleteClients: Boolean = false,
    val viewProjects: Boolean = true,
    val createProjects: Boolean = true,
    val editProjects: Boolean = true,
    val deleteProjects: Boolean = false,
    val viewEstimates: Boolean = true,
    val createEstimates: Boolean = true,
    val editEstimates: Boolean = true,
    val deleteEstimates: Boolean = false,
    val viewQuotations: Boolean = true,
    val createQuotations: Boolean = true,
    val editQuotations: Boolean = true,
    val deleteQuotations: Boolean = false,
    val generatePdf: Boolean = true,
    val sharePdf: Boolean = true,
    val viewProfit: Boolean = false, // Hides profit margin and markup from UI
    val viewCosts: Boolean = false,  // Hides material cost, labour cost from UI
    val manageRates: Boolean = false,
    val manageTemplates: Boolean = false,
    val manageTeam: Boolean = false,
    val manageWorkspace: Boolean = false,
    val manageBilling: Boolean = false
) {
    companion object {
        fun forRole(role: String): WorkspacePermissions = when (role.uppercase()) {
            "OWNER" -> WorkspacePermissions(
                deleteClients = true, deleteProjects = true, deleteEstimates = true,
                deleteQuotations = true, viewProfit = true, viewCosts = true,
                manageRates = true, manageTemplates = true, manageTeam = true,
                manageWorkspace = true, manageBilling = true
            )
            "ADMIN" -> WorkspacePermissions(
                deleteClients = true, deleteProjects = true, deleteEstimates = true,
                deleteQuotations = true, viewProfit = true, viewCosts = true,
                manageRates = true, manageTemplates = true, manageTeam = true,
                manageWorkspace = false, manageBilling = false
            )
            "MANAGER" -> WorkspacePermissions(
                deleteClients = false, deleteProjects = false, deleteEstimates = false,
                deleteQuotations = false, viewProfit = true, viewCosts = true,
                manageRates = true, manageTemplates = false, manageTeam = false,
                manageWorkspace = false, manageBilling = false
            )
            "DESIGNER" -> WorkspacePermissions(
                deleteClients = false, deleteProjects = false, deleteEstimates = false,
                deleteQuotations = false, viewProfit = false, viewCosts = false,
                manageRates = false, manageTemplates = false, manageTeam = false,
                manageWorkspace = false, manageBilling = false
            )
            "ACCOUNTANT" -> WorkspacePermissions(
                createClients = false, editClients = false, deleteClients = false,
                createProjects = false, editProjects = false, deleteProjects = false,
                createEstimates = false, editEstimates = false, deleteEstimates = false,
                createQuotations = false, editQuotations = false, deleteQuotations = false,
                viewProfit = true, viewCosts = true, manageBilling = true
            )
            "VIEWER" -> WorkspacePermissions(
                createClients = false, editClients = false, deleteClients = false,
                createProjects = false, editProjects = false, deleteProjects = false,
                createEstimates = false, editEstimates = false, deleteEstimates = false,
                createQuotations = false, editQuotations = false, deleteQuotations = false,
                generatePdf = false, sharePdf = false, viewProfit = false, viewCosts = false
            )
            else -> WorkspacePermissions()
        }
    }
}

// Activity Audit Log for team operations
@Entity(
    tableName = "activity_logs",
    indices = [Index("workspaceId"), Index("timestamp")]
)
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: String,
    val userId: String,
    val userName: String,
    val action: String, // "Created quotation", "Revised quotation", "Accepted quotation", "Generated PDF", etc.
    val documentNumber: String? = null, // e.g. "QT-2026-024"
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// Brand Kits for workspace identity
@Entity(
    tableName = "brand_kits",
    indices = [Index("workspaceId")]
)
data class BrandKitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: String,
    val name: String = "Knot Architects Signature",
    val logoUri: String? = null,
    val secondaryLogoUri: String? = null,
    val primaryColor: String = "#A3834C", // Luxury Gold
    val secondaryColor: String = "#1F1F1F", // Architectural Black
    val fontFamily: String = "Roboto",
    val watermarkEnabled: Boolean = false,
    val watermarkType: String = "STATUS", // STATUS, TEXT, LOGO
    val watermarkText: String = "CONFIDENTIAL",
    val watermarkOpacity: Float = 0.12f,
    val watermarkPosition: String = "DIAGONAL", // DIAGONAL, CENTER, TOP, BOTTOM
    val isDefault: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// PDF Templates (Workspace Default vs Personal)
@Entity(
    tableName = "pdf_templates",
    indices = [Index("workspaceId")]
)
data class PdfTemplateEntity(
    @PrimaryKey val id: String, // e.g. "style_minimal", "style_corporate", "style_architectural"
    val workspaceId: String,
    val name: String,
    val styleCode: String, // MINIMAL_STUDIO, PROFESSIONAL_CORPORATE, PREMIUM_ARCHITECTURAL
    val accentColor: String = "#A3834C",
    val isWorkspaceDefault: Boolean = true,
    val isPersonalTemplate: Boolean = false,
    val ownerUserId: String? = null,
    val settingsJson: String = ""
)

// Google Drive Integration & Cloud sync
@Entity(tableName = "drive_connections")
data class DriveConnectionEntity(
    @PrimaryKey val workspaceId: String,
    val accountEmail: String = "",
    val accountName: String = "",
    val isConnected: Boolean = false,
    val driveType: String = "MY_DRIVE", // MY_DRIVE, SHARED_DRIVE
    val rootFolderId: String = "QuotationApp",
    val autoSavePdf: Boolean = true,
    val lastSyncedAt: Long = 0L,
    val syncStatus: String = "DISCONNECTED", // DISCONNECTED, IDLE, SYNCING, FAILED, SUCCESS
    val errorMessage: String? = null
)

@Entity(
    tableName = "drive_files",
    indices = [Index("workspaceId"), Index("quotationId")]
)
data class DriveFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: String,
    val quotationId: Long,
    val revision: Int = 0,
    val driveFileId: String,
    val driveFolderId: String = "Quotations",
    val fileName: String,
    val syncState: String = "SYNCED", // PENDING, SYNCING, SYNCED, FAILED
    val fileSize: Long = 0L,
    val uploadedAt: Long = System.currentTimeMillis()
)

// ==========================================
// WORKSPACE DOMAIN ENTITIES
// ==========================================

@Entity(
    tableName = "companies",
    indices = [Index("workspaceId")]
)
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: String = "ws_knot_arch",
    val name: String,
    val logoUri: String? = null,
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val gstin: String = "",
    val pan: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifsc: String = "",
    val upiId: String = "",
    val signatureName: String = "",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "clients",
    indices = [Index("workspaceId")]
)
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: String = "ws_knot_arch",
    val name: String,
    val companyName: String = "",
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    val gstin: String = "",
    val pan: String = "",
    val state: String = "",
    val city: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clientId"), Index("workspaceId")]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: String = "ws_knot_arch",
    val clientId: Long,
    val name: String,
    val projectType: String = "Residential",
    val address: String = "",
    val area: String = "",
    val description: String = "",
    val startDate: Long? = null,
    val expectedCompletionDate: Long? = null,
    val budget: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "quotations",
    indices = [Index("quotationNumber"), Index("clientId"), Index("projectId"), Index("workspaceId")]
)
data class QuotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: String = "ws_knot_arch",
    val quotationNumber: String,
    val revision: Int = 0, // 0 = original, 1 = Rev.01, etc.
    val parentQuotationId: Long? = null,
    val companyId: Long,
    val clientId: Long,
    val projectId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val validityDays: Int = 30,
    val status: String = "Draft", // Draft, Sent, Viewed, Accepted, Rejected, Expired, Revised, Cancelled
    val calculationMode: String = "SELLING_PRICE", // SELLING_PRICE, COST_PLUS_MARGIN
    val profitMarginPercent: Double = 20.0,
    val isGstInclusive: Boolean = false,
    val gstType: String = "CGST_SGST", // CGST_SGST, IGST, NONE
    val taxRate: Double = 18.0,
    val discountType: String = "PERCENTAGE", // PERCENTAGE, FIXED
    val discountValue: Double = 0.0,
    val templateId: String = "style_minimal",
    val pdfStyle: String = "MINIMAL_STUDIO", // MINIMAL_STUDIO, PROFESSIONAL_CORPORATE, PREMIUM_ARCHITECTURAL
    val templatePrimaryColor: String = "#A3834C", // Luxury Gold
    val watermarkEnabled: Boolean = false,
    val watermarkType: String = "STATUS", // STATUS, TEXT, LOGO
    val watermarkText: String = "DRAFT",
    val watermarkOpacity: Float = 0.12f,
    val createdByName: String = "Author",
    val createdByUserId: String = "",
    val driveSyncStatus: String = "NOT_SYNCED", // NOT_SYNCED, SYNCED, PENDING, FAILED
    val driveFileId: String? = null,
    val scopeIncluded: String = "",
    val scopeExcluded: String = "",
    val materialSpecs: String = "",
    val warrantyTerms: String = "",
    val timeline: String = "",
    val siteConditions: String = "",
    val termsAndConditions: String = "",
    val paymentMilestonesJson: String = "", // serialized JSON for milestones
    val isEstimate: Boolean = false, // false = Client quotation, true = Internal cost estimate
    val isInvoice: Boolean = false,
    val invoiceNumber: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "quotation_items",
    foreignKeys = [
        ForeignKey(
            entity = QuotationEntity::class,
            parentColumns = ["id"],
            childColumns = ["quotationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("quotationId")]
)
data class QuotationItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quotationId: Long,
    val roomName: String = "",
    val category: String = "",
    val itemName: String,
    val description: String = "",
    val specification: String = "",
    val unit: String = "nos",
    val quantity: Double = 1.0,
    val rate: Double = 0.0,
    val discountType: String = "PERCENTAGE",
    val discountValue: Double = 0.0,
    val taxRate: Double = 0.0,
    val materialCost: Double = 0.0,
    val labourCost: Double = 0.0,
    val installationCost: Double = 0.0,
    val overheadCost: Double = 0.0,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "materials",
    indices = [Index("workspaceId")]
)
data class MaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: String = "ws_knot_arch",
    val productName: String,
    val brand: String = "",
    val category: String = "",
    val specification: String = "",
    val unit: String = "sq.ft",
    val defaultRate: Double = 0.0,
    val taxRate: Double = 18.0,
    val supplier: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "rates",
    indices = [Index("workspaceId")]
)
data class RateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: String = "ws_knot_arch",
    val name: String,
    val category: String = "Material", // Material, Labour, Installation, Product, Service
    val unit: String = "sq.ft",
    val rate: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "term_presets",
    indices = [Index("workspaceId")]
)
data class TermPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: String = "ws_knot_arch",
    val title: String,
    val category: String = "Interior",
    val content: String = ""
)

@Entity(
    tableName = "numbering_config",
    indices = [Index("workspaceId")]
)
data class NumberingConfigEntity(
    @PrimaryKey val id: Int = 1,
    val workspaceId: String = "ws_knot_arch",
    val prefix: String = "KA",
    val includeYear: Boolean = true,
    val includeMonth: Boolean = false,
    val digits: Int = 3,
    val nextSequence: Int = 1
)
