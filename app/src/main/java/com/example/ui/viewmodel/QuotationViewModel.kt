package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.MaterialRateRepository
import com.example.data.repository.QuotationRepository
import com.example.domain.auth.AuthManager
import com.example.domain.drive.GoogleDriveManager
import com.example.domain.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardStats(
    val totalQuotations: Int = 0,
    val draftsCount: Int = 0,
    val sentCount: Int = 0,
    val acceptedCount: Int = 0,
    val rejectedCount: Int = 0,
    val totalQuotedValue: Double = 0.0,
    val acceptedValue: Double = 0.0,
    val pendingValue: Double = 0.0,
    val conversionRate: Double = 0.0
)

data class QuotationFilterState(
    val searchQuery: String = "",
    val statusFilter: String = "ALL", // ALL, Draft, Sent, Accepted, Rejected
    val sortBy: String = "NEWEST" // NEWEST, OLDEST, HIGHEST_VALUE, LOWEST_VALUE
)

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class QuotationViewModel(application: Application) : AndroidViewModel(application) {

    // Theme Mode
    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    fun toggleThemeMode() {
        _themeMode.value = when (_themeMode.value) {
            AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.SYSTEM
        }
    }

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val quotationRepo = QuotationRepository(
        database.quotationDao(),
        database.quotationItemDao(),
        database.clientDao(),
        database.projectDao(),
        database.companyDao(),
        database.numberingConfigDao()
    )
    val materialRateRepo = MaterialRateRepository(
        database.materialDao(),
        database.rateDao(),
        database.termPresetDao()
    )

    // SaaS Managers
    val authManager = AuthManager(database.userDao())
    val driveManager = GoogleDriveManager(application, database.driveDao())

    val currentUser: StateFlow<UserEntity?> = authManager.currentUser
    val isAuthenticated: StateFlow<Boolean> = authManager.isAuthenticated

    // Workspaces
    val allWorkspaces: StateFlow<List<WorkspaceEntity>> = database.workspaceDao().getAllWorkspaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentWorkspaceId = MutableStateFlow("ws_knot_arch")
    val currentWorkspaceId: StateFlow<String> = _currentWorkspaceId.asStateFlow()

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    ensureWorkspaceForUser(user)
                }
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentWorkspace: StateFlow<WorkspaceEntity?> = _currentWorkspaceId.flatMapLatest { id ->
        database.workspaceDao().observeWorkspaceById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val workspaceMembers: StateFlow<List<WorkspaceMemberEntity>> = _currentWorkspaceId.flatMapLatest { id ->
        database.workspaceMemberDao().getMembersForWorkspace(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Roles and Permissions
    val currentUserRole: StateFlow<String> = combine(
        currentUser,
        workspaceMembers
    ) { user, members ->
        val currentUserId = user?.id ?: "user_rajesh"
        members.find { it.userId == currentUserId }?.role ?: "OWNER"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "OWNER")

    val currentUserPermissions: StateFlow<WorkspacePermissions> = currentUserRole.map { role ->
        WorkspacePermissions.forRole(role)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkspacePermissions.forRole("OWNER"))

    // Brand Kits & Templates
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val brandKits: StateFlow<List<BrandKitEntity>> = _currentWorkspaceId.flatMapLatest { id ->
        database.brandKitDao().getBrandKitsForWorkspace(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentBrandKit: StateFlow<BrandKitEntity?> = _currentWorkspaceId.flatMapLatest { id ->
        database.brandKitDao().getDefaultBrandKit(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pdfTemplates: StateFlow<List<PdfTemplateEntity>> = _currentWorkspaceId.flatMapLatest { id ->
        database.pdfTemplateDao().getTemplatesForWorkspace(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Activity Logs
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activityLogs: StateFlow<List<ActivityLogEntity>> = _currentWorkspaceId.flatMapLatest { id ->
        database.activityLogDao().getLogsForWorkspace(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Google Drive Integration
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val driveConnection: StateFlow<DriveConnectionEntity?> = _currentWorkspaceId.flatMapLatest { id ->
        driveManager.observeConnection(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val driveFiles: StateFlow<List<DriveFileEntity>> = _currentWorkspaceId.flatMapLatest { id ->
        driveManager.observeDriveFiles(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncInProgress: StateFlow<Boolean> = driveManager.syncInProgress
    val syncMessage: StateFlow<String?> = driveManager.syncMessage

    // Data streams filtered by active workspace
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allQuotations: StateFlow<List<QuotationEntity>> = _currentWorkspaceId.flatMapLatest { wsId ->
        quotationRepo.getQuotationsForWorkspace(wsId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allClients: StateFlow<List<ClientEntity>> = _currentWorkspaceId.flatMapLatest { wsId ->
        quotationRepo.getClientsForWorkspace(wsId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allProjects: StateFlow<List<ProjectEntity>> = _currentWorkspaceId.flatMapLatest { wsId ->
        quotationRepo.getProjectsForWorkspace(wsId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allCompanies: StateFlow<List<CompanyEntity>> = _currentWorkspaceId.flatMapLatest { wsId ->
        quotationRepo.getCompaniesForWorkspace(wsId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultCompany: StateFlow<CompanyEntity?> = quotationRepo.defaultCompany
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allMaterials: StateFlow<List<MaterialEntity>> = materialRateRepo.allMaterials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRates: StateFlow<List<RateEntity>> = materialRateRepo.allRates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTerms: StateFlow<List<TermPresetEntity>> = materialRateRepo.allTermPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val numberingConfig: StateFlow<NumberingConfigEntity?> = quotationRepo.numberingConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filter & Search state
    private val _filterState = MutableStateFlow(QuotationFilterState())
    val filterState: StateFlow<QuotationFilterState> = _filterState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun updateStatusFilter(status: String) {
        _filterState.value = _filterState.value.copy(statusFilter = status)
    }

    fun updateSortBy(sort: String) {
        _filterState.value = _filterState.value.copy(sortBy = sort)
    }

    // Dashboard Statistics Calculation
    val dashboardStats: StateFlow<DashboardStats> = combine(allQuotations) { quotations ->
        var total = quotations[0].size
        var drafts = 0
        var sent = 0
        var accepted = 0
        var rejected = 0
        var totalValue = 0.0
        var acceptedVal = 0.0
        var pendingVal = 0.0

        for (q in quotations[0]) {
            when (q.status.lowercase()) {
                "draft" -> drafts++
                "sent", "viewed" -> sent++
                "accepted" -> accepted++
                "rejected" -> rejected++
            }
        }

        val convRate = if (total > 0) (accepted.toDouble() / total) * 100.0 else 0.0

        DashboardStats(
            totalQuotations = total,
            draftsCount = drafts,
            sentCount = sent,
            acceptedCount = accepted,
            rejectedCount = rejected,
            totalQuotedValue = totalValue,
            acceptedValue = acceptedVal,
            pendingValue = pendingVal,
            conversionRate = kotlin.math.round(convRate * 10.0) / 10.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // ----------------------------------------------------
    // WORKSPACE OPERATIONS
    // ----------------------------------------------------
    fun switchWorkspace(workspaceId: String) {
        _currentWorkspaceId.value = workspaceId
        logActivity("Switched workspace", null, "Switched to workspace $workspaceId")
    }

    fun createWorkspace(name: String, companyName: String, businessType: String) {
        viewModelScope.launch {
            val user = currentUser.value
            val ownerId = user?.id ?: "user_owner"
            val ownerName = user?.displayName ?: "Studio Owner"
            val ownerEmail = user?.email ?: ""
            val newId = "ws_" + name.lowercase().replace(" ", "_").take(12) + "_" + System.currentTimeMillis().toString().takeLast(4)
            val newWorkspace = WorkspaceEntity(
                id = newId,
                name = name,
                companyName = companyName,
                businessType = businessType,
                ownerUserId = ownerId,
                isDefault = false
            )
            database.workspaceDao().insertWorkspace(newWorkspace)

            // Add creator as owner member
            database.workspaceMemberDao().insertMember(
                WorkspaceMemberEntity(
                    workspaceId = newId,
                    userId = ownerId,
                    userName = ownerName,
                    userEmail = ownerEmail,
                    role = "OWNER",
                    status = "ACTIVE"
                )
            )

            // Create default brand kit
            database.brandKitDao().insertBrandKit(
                BrandKitEntity(
                    workspaceId = newId,
                    name = "$name Brand Kit",
                    primaryColor = "#A3834C",
                    secondaryColor = "#1F1F1F",
                    watermarkEnabled = true,
                    watermarkText = "DRAFT",
                    isDefault = true
                )
            )

            // Switch to the newly created workspace
            _currentWorkspaceId.value = newId
            logActivity("Created workspace", null, "Created new workspace: $name ($businessType)")
        }
    }

    fun addWorkspaceMember(name: String, email: String, role: String) {
        viewModelScope.launch {
            val wsId = _currentWorkspaceId.value
            val newMember = WorkspaceMemberEntity(
                workspaceId = wsId,
                userId = "user_" + email.substringBefore("@").replace(".", "_"),
                userName = name,
                userEmail = email,
                role = role,
                status = "ACTIVE"
            )
            database.workspaceMemberDao().insertMember(newMember)
            logActivity("Invited team member", null, "Added $name ($email) as $role")
        }
    }

    fun updateMemberRole(member: WorkspaceMemberEntity, newRole: String) {
        viewModelScope.launch {
            database.workspaceMemberDao().updateMember(member.copy(role = newRole))
            logActivity("Updated member role", null, "Changed ${member.userName} role to $newRole")
        }
    }

    fun removeMember(member: WorkspaceMemberEntity) {
        viewModelScope.launch {
            database.workspaceMemberDao().deleteMember(member)
            logActivity("Removed team member", null, "Removed ${member.userName} from workspace")
        }
    }

    // ----------------------------------------------------
    // GOOGLE AUTH OPERATIONS
    // ----------------------------------------------------
    fun signInWithGoogle(
        context: android.content.Context,
        email: String? = null,
        displayName: String? = null,
        onResult: (Result<UserEntity>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle(context, email, displayName)
            result.onSuccess { user ->
                logActivity("Logged in", null, "Signed in as ${user.displayName} (${user.email})")
                ensureWorkspaceForUser(user)
            }
            onResult(result)
        }
    }

    fun signInAsDemo(onResult: (UserEntity) -> Unit = {}) {
        viewModelScope.launch {
            val user = authManager.signInAsDemoAccount()
            _currentWorkspaceId.value = "ws_knot_arch"
            logActivity("Demo Access", null, "Logged into Knot Architects demo workspace")
            onResult(user)
        }
    }

    suspend fun ensureWorkspaceForUser(user: UserEntity) {
        if (user.id == "user_demo_knot" || user.id == "user_rajesh") {
            _currentWorkspaceId.value = "ws_knot_arch"
            return
        }

        // If real user already has a valid workspace assigned:
        if (user.defaultWorkspaceId.isNotBlank() && user.defaultWorkspaceId != "ws_knot_arch") {
            val existingWs = withContext(Dispatchers.IO) {
                database.workspaceDao().getWorkspaceById(user.defaultWorkspaceId)
            }
            if (existingWs != null) {
                _currentWorkspaceId.value = existingWs.id
                return
            }
        }

        val userWsId = "ws_" + user.id
        val userWs = withContext(Dispatchers.IO) {
            database.workspaceDao().getWorkspaceById(userWsId)
        }
        if (userWs != null) {
            _currentWorkspaceId.value = userWs.id
        } else {
            val studioName = if (user.displayName.isNotBlank()) "${user.displayName}'s Studio" else "My Architectural Studio"
            val companyName = if (user.displayName.isNotBlank()) "${user.displayName}'s Architectural Studio" else "Architectural Design Studio"
            val newWs = WorkspaceEntity(
                id = userWsId,
                name = studioName,
                companyName = companyName,
                businessType = "Architecture & Interior",
                country = "India",
                currency = "INR",
                currencySymbol = "₹",
                ownerUserId = user.id,
                isDefault = true
            )
            withContext(Dispatchers.IO) {
                database.workspaceDao().insertWorkspace(newWs)
                database.workspaceMemberDao().insertMembers(
                    listOf(
                        WorkspaceMemberEntity(
                            workspaceId = userWsId,
                            userId = user.id,
                            userName = if (user.displayName.isNotBlank()) user.displayName else "Owner",
                            userEmail = user.email,
                            role = "OWNER",
                            status = "ACTIVE"
                        )
                    )
                )

                // Dedicated company profile for user's workspace
                database.companyDao().insertCompany(
                    CompanyEntity(
                        workspaceId = userWsId,
                        name = companyName,
                        address = "India",
                        phone = "",
                        email = user.email,
                        website = "",
                        gstin = "",
                        pan = "",
                        bankName = "",
                        accountNumber = "",
                        ifsc = "",
                        upiId = "",
                        logoUri = null,
                        signatureName = if (user.displayName.isNotBlank()) user.displayName else "Principal Architect",
                        isDefault = true
                    )
                )

                // Numbering config
                database.numberingConfigDao().saveConfig(
                    NumberingConfigEntity(
                        workspaceId = userWsId,
                        prefix = "QT",
                        includeYear = true,
                        includeMonth = false,
                        digits = 3,
                        nextSequence = 1
                    )
                )

                // Brand Kit
                database.brandKitDao().insertBrandKit(
                    BrandKitEntity(
                        workspaceId = userWsId,
                        name = "$studioName Brand Kit",
                        primaryColor = "#A3834C",
                        secondaryColor = "#1F1F1F",
                        watermarkEnabled = true,
                        watermarkType = "STATUS",
                        watermarkText = "DRAFT",
                        isDefault = true
                    )
                )

                // PDF Templates
                database.pdfTemplateDao().insertTemplates(
                    listOf(
                        PdfTemplateEntity(
                            id = "tpl_minimal_$userWsId",
                            workspaceId = userWsId,
                            name = "Modern Minimalist",
                            styleCode = "MINIMAL_STUDIO",
                            accentColor = "#1E293B",
                            isWorkspaceDefault = true
                        ),
                        PdfTemplateEntity(
                            id = "tpl_architectural_$userWsId",
                            workspaceId = userWsId,
                            name = "Executive Architectural",
                            styleCode = "PREMIUM_ARCHITECTURAL",
                            accentColor = "#D97706",
                            isWorkspaceDefault = false
                        )
                    )
                )

                // Google Drive Connection linked to user's real email
                if (user.email.isNotBlank()) {
                    database.driveDao().insertOrUpdateConnection(
                        DriveConnectionEntity(
                            workspaceId = userWsId,
                            accountEmail = user.email,
                            accountName = "${if (user.displayName.isNotBlank()) user.displayName else "Google"} Drive",
                            isConnected = true,
                            driveType = "MY_DRIVE",
                            rootFolderId = "QuotationApp",
                            autoSavePdf = true,
                            lastSyncedAt = System.currentTimeMillis(),
                            syncStatus = "CONNECTED",
                            errorMessage = null
                        )
                    )
                }

                // Update defaultWorkspaceId on user
                val updatedUser = user.copy(defaultWorkspaceId = userWsId)
                database.userDao().insertUser(updatedUser)
            }
            _currentWorkspaceId.value = userWsId
        }
    }

    fun switchUserAccount(user: UserEntity) {
        viewModelScope.launch {
            authManager.switchUserAccount(user)
            ensureWorkspaceForUser(user)
            logActivity("Switched account", null, "Switched user to ${user.displayName}")
        }
    }

    fun signOut() {
        authManager.signOut()
    }

    // ----------------------------------------------------
    // GOOGLE DRIVE OPERATIONS
    // ----------------------------------------------------
    fun connectGoogleDrive(
        email: String,
        accountName: String = "Google Drive",
        driveType: String = "MY_DRIVE",
        folderName: String = "QuotationApp"
    ) {
        viewModelScope.launch {
            driveManager.connectGoogleDrive(_currentWorkspaceId.value, email, accountName, driveType, folderName)
            logActivity("Connected Google Drive", null, "Linked Google Drive account ($email)")
        }
    }

    fun disconnectGoogleDrive() {
        viewModelScope.launch {
            driveManager.disconnectGoogleDrive(_currentWorkspaceId.value)
            logActivity("Disconnected Google Drive", null, "Unlinked Google Drive")
        }
    }

    fun toggleDriveAutoSave(enabled: Boolean) {
        viewModelScope.launch {
            driveManager.updateAutoSave(_currentWorkspaceId.value, enabled)
        }
    }

    fun syncDriveNow() {
        viewModelScope.launch {
            driveManager.syncNow(_currentWorkspaceId.value)
            logActivity("Manual Drive Sync", null, "Triggered Google Drive synchronization")
        }
    }

    // ----------------------------------------------------
    // BRAND KIT & AUDIT LOGS
    // ----------------------------------------------------
    fun saveBrandKit(kit: BrandKitEntity) {
        viewModelScope.launch {
            if (kit.id == 0L) {
                database.brandKitDao().insertBrandKit(kit)
            } else {
                database.brandKitDao().updateBrandKit(kit)
            }
            logActivity("Updated Brand Kit", null, "Modified brand styling and watermark settings")
        }
    }

    fun logActivity(action: String, docNumber: String?, details: String) {
        viewModelScope.launch {
            val user = currentUser.value
            database.activityLogDao().insertLog(
                ActivityLogEntity(
                    workspaceId = _currentWorkspaceId.value,
                    userId = user?.id ?: "user_author",
                    userName = user?.displayName ?: "Author",
                    action = action,
                    documentNumber = docNumber,
                    details = details,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // ----------------------------------------------------
    // ACTIVE QUOTATION BUILDER STATE
    // ----------------------------------------------------
    private val _currentQuotation = MutableStateFlow<QuotationEntity?>(null)
    val currentQuotation: StateFlow<QuotationEntity?> = _currentQuotation.asStateFlow()

    private val _currentItems = MutableStateFlow<List<QuotationItemEntity>>(emptyList())
    val currentItems: StateFlow<List<QuotationItemEntity>> = _currentItems.asStateFlow()

    val currentCalculation: StateFlow<QuotationCalculationResult> = combine(
        _currentQuotation,
        _currentItems
    ) { quotation, items ->
        val q = quotation ?: QuotationEntity(
            workspaceId = _currentWorkspaceId.value,
            quotationNumber = "NEW",
            companyId = 0,
            clientId = 0
        )
        val calcMode = if (q.calculationMode == "COST_PLUS_MARGIN") CalculationMode.COST_PLUS_MARGIN else CalculationMode.SELLING_PRICE

        val calculatedItems = items.map { itm ->
            val itmSummary = CalculationEngine.calculateItem(
                quantity = itm.quantity,
                rate = itm.rate,
                discountType = if (itm.discountType == "FIXED") DiscountType.FIXED else DiscountType.PERCENTAGE,
                discountValue = itm.discountValue,
                taxRate = itm.taxRate,
                calculationMode = calcMode,
                profitMarginPercent = q.profitMarginPercent,
                materialCost = itm.materialCost,
                labourCost = itm.labourCost,
                installationCost = itm.installationCost,
                overheadCost = itm.overheadCost
            )

            CalculatedItem(
                id = itm.id,
                roomName = itm.roomName,
                category = itm.category,
                itemName = itm.itemName,
                description = itm.description,
                specification = itm.specification,
                unit = itm.unit,
                quantity = itm.quantity,
                rate = itm.rate,
                discountType = if (itm.discountType == "FIXED") DiscountType.FIXED else DiscountType.PERCENTAGE,
                discountValue = itm.discountValue,
                taxRate = itm.taxRate,
                materialCost = itm.materialCost,
                labourCost = itm.labourCost,
                installationCost = itm.installationCost,
                overheadCost = itm.overheadCost,
                sortOrder = itm.sortOrder,
                effectiveRate = itmSummary.effectiveRate,
                grossAmount = itmSummary.grossAmount,
                discountAmount = itmSummary.discountAmount,
                netAmount = itmSummary.netAmount,
                totalCost = itmSummary.totalCost,
                itemProfit = itmSummary.itemProfit
            )
        }

        val gstType = when (q.gstType) {
            "IGST" -> GstType.IGST
            "NONE" -> GstType.NONE
            else -> GstType.CGST_SGST
        }

        CalculationEngine.calculateQuotation(
            items = calculatedItems,
            overallDiscountType = if (q.discountType == "FIXED") DiscountType.FIXED else DiscountType.PERCENTAGE,
            overallDiscountValue = q.discountValue,
            gstRate = q.taxRate,
            isGstInclusive = q.isGstInclusive,
            gstType = gstType
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        QuotationCalculationResult(
            items = emptyList(), subtotal = 0.0, discountAmount = 0.0, taxableAmount = 0.0,
            gstRate = 18.0, isGstInclusive = false, gstType = GstType.CGST_SGST,
            cgstAmount = 0.0, sgstAmount = 0.0, igstAmount = 0.0, totalGstAmount = 0.0,
            grandTotal = 0.0, totalCost = 0.0, totalProfit = 0.0, profitMarginPercentage = 0.0,
            roomTotals = emptyMap(), categoryTotals = emptyMap()
        )
    )

    // Load or initialize a new Quotation
    fun initNewQuotation(clientId: Long = 0, isEstimate: Boolean = false) {
        viewModelScope.launch {
            val nextNumber = quotationRepo.generateNextQuotationNumber()
            val defComp = defaultCompany.value ?: allCompanies.value.firstOrNull()
            val initial = QuotationEntity(
                workspaceId = _currentWorkspaceId.value,
                quotationNumber = nextNumber,
                companyId = defComp?.id ?: 1,
                clientId = clientId,
                date = System.currentTimeMillis(),
                validityDays = 30,
                status = "Draft",
                calculationMode = "SELLING_PRICE",
                isEstimate = isEstimate,
                taxRate = 18.0,
                gstType = "CGST_SGST",
                pdfStyle = "PREMIUM_ARCHITECTURAL",
                templatePrimaryColor = "#A3834C",
                watermarkEnabled = true,
                watermarkText = "DRAFT",
                termsAndConditions = "1. Quotation valid for 30 days.\n2. Work commences upon 50% advance realization.\n3. Prevailing GST rates applicable."
            )
            _currentQuotation.value = initial
            _currentItems.value = emptyList()
        }
    }

    fun loadQuotation(id: Long) {
        viewModelScope.launch {
            val q = quotationRepo.getQuotationById(id)
            if (q != null) {
                _currentQuotation.value = q
                val items = database.quotationItemDao().getItemsForQuotationSync(id)
                _currentItems.value = items
            }
        }
    }

    fun updateQuotationHeader(update: (QuotationEntity) -> QuotationEntity) {
        _currentQuotation.value?.let { current ->
            _currentQuotation.value = update(current)
        }
    }

    fun addItem(item: QuotationItemEntity) {
        val currentList = _currentItems.value.toMutableList()
        currentList.add(item.copy(sortOrder = currentList.size + 1))
        _currentItems.value = currentList
    }

    fun updateItem(index: Int, item: QuotationItemEntity) {
        val currentList = _currentItems.value.toMutableList()
        if (index in currentList.indices) {
            currentList[index] = item
            _currentItems.value = currentList
        }
    }

    fun removeItem(index: Int) {
        val currentList = _currentItems.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _currentItems.value = currentList
        }
    }

    fun reorderItems(fromIndex: Int, toIndex: Int) {
        val currentList = _currentItems.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _currentItems.value = currentList
        }
    }

    fun addItems(items: List<QuotationItemEntity>) {
        val currentList = _currentItems.value.toMutableList()
        items.forEach { itm ->
            currentList.add(itm.copy(sortOrder = currentList.size + 1))
        }
        _currentItems.value = currentList
    }

    fun saveCurrentQuotation(onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val q = _currentQuotation.value ?: return@launch
            val items = _currentItems.value
            val savedId = quotationRepo.saveQuotation(q.copy(workspaceId = _currentWorkspaceId.value), items)
            _currentQuotation.value = q.copy(id = savedId)
            logActivity(if (q.id == 0L) "Created quotation" else "Updated quotation", q.quotationNumber, "Total items: ${items.size}")
            onSaved(savedId)
        }
    }

    fun duplicateQuotation(id: Long, onDuplicated: (Long) -> Unit) {
        viewModelScope.launch {
            val newId = quotationRepo.duplicateQuotation(id)
            logActivity("Duplicated quotation", null, "Cloned quotation #$id into #$newId")
            onDuplicated(newId)
        }
    }

    fun createRevision(id: Long, onRevisionCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val newId = quotationRepo.createRevision(id)
            val rev = quotationRepo.getQuotationById(newId)
            logActivity("Created revision", rev?.quotationNumber, "Created revision from original #$id")
            onRevisionCreated(newId)
        }
    }

    fun convertToInvoice(id: Long, onConverted: (String) -> Unit) {
        viewModelScope.launch {
            val invNumber = quotationRepo.convertToInvoice(id)
            logActivity("Converted to Invoice", invNumber, "Converted quotation #$id to $invNumber")
            onConverted(invNumber)
        }
    }

    fun updateStatus(id: Long, status: String) {
        viewModelScope.launch {
            quotationRepo.updateQuotationStatus(id, status)
            if (_currentQuotation.value?.id == id) {
                _currentQuotation.value = _currentQuotation.value?.copy(status = status)
            }
            logActivity("Updated status", null, "Changed quotation #$id status to $status")
        }
    }

    fun deleteQuotation(quotation: QuotationEntity) {
        viewModelScope.launch {
            quotationRepo.deleteQuotation(quotation)
            if (_currentQuotation.value?.id == quotation.id) {
                _currentQuotation.value = null
                _currentItems.value = emptyList()
            }
            logActivity("Deleted quotation", quotation.quotationNumber, "Deleted quotation from workspace")
        }
    }

    // PDF Generation & Sharing with Drive Upload
    suspend fun generatePdf(
        context: android.content.Context,
        quotationId: Long,
        overrideStyle: String? = null,
        overrideAccentColor: String? = null,
        overrideWatermarkEnabled: Boolean? = null,
        overrideWatermarkText: String? = null,
        saveToDrive: Boolean = false
    ): File? = withContext(Dispatchers.IO) {
        var quotation = quotationRepo.getQuotationById(quotationId) ?: return@withContext null
        if (overrideStyle != null || overrideAccentColor != null || overrideWatermarkEnabled != null || overrideWatermarkText != null) {
            quotation = quotation.copy(
                pdfStyle = overrideStyle ?: quotation.pdfStyle,
                templatePrimaryColor = overrideAccentColor ?: quotation.templatePrimaryColor,
                watermarkEnabled = overrideWatermarkEnabled ?: quotation.watermarkEnabled,
                watermarkText = overrideWatermarkText ?: quotation.watermarkText
            )
        }

        val items = database.quotationItemDao().getItemsForQuotationSync(quotationId)
        val company = quotationRepo.getCompanyById(quotation.companyId)
        val client = quotationRepo.getClientById(quotation.clientId)
        val project = quotation.projectId?.let { quotationRepo.getProjectById(it) }

        val calcMode = if (quotation.calculationMode == "COST_PLUS_MARGIN") CalculationMode.COST_PLUS_MARGIN else CalculationMode.SELLING_PRICE
        val calcItems = items.map { itm ->
            val sum = CalculationEngine.calculateItem(
                quantity = itm.quantity,
                rate = itm.rate,
                discountType = if (itm.discountType == "FIXED") DiscountType.FIXED else DiscountType.PERCENTAGE,
                discountValue = itm.discountValue,
                taxRate = itm.taxRate,
                calculationMode = calcMode,
                profitMarginPercent = quotation.profitMarginPercent,
                materialCost = itm.materialCost,
                labourCost = itm.labourCost,
                installationCost = itm.installationCost,
                overheadCost = itm.overheadCost
            )
            CalculatedItem(
                id = itm.id,
                roomName = itm.roomName,
                category = itm.category,
                itemName = itm.itemName,
                description = itm.description,
                specification = itm.specification,
                unit = itm.unit,
                quantity = itm.quantity,
                rate = itm.rate,
                effectiveRate = sum.effectiveRate,
                grossAmount = sum.grossAmount,
                discountAmount = sum.discountAmount,
                netAmount = sum.netAmount,
                totalCost = sum.totalCost,
                itemProfit = sum.itemProfit
            )
        }

        val gstType = when (quotation.gstType) {
            "IGST" -> GstType.IGST
            "NONE" -> GstType.NONE
            else -> GstType.CGST_SGST
        }

        val calculation = CalculationEngine.calculateQuotation(
            items = calcItems,
            overallDiscountType = if (quotation.discountType == "FIXED") DiscountType.FIXED else DiscountType.PERCENTAGE,
            overallDiscountValue = quotation.discountValue,
            gstRate = quotation.taxRate,
            isGstInclusive = quotation.isGstInclusive,
            gstType = gstType
        )

        val brandKit = currentBrandKit.value
        val pdfFile = PdfGenerator.generateQuotationPdf(
            context = context,
            quotation = quotation,
            calculation = calculation,
            company = company,
            client = client,
            project = project,
            brandKit = brandKit
        )

        // Log PDF export
        logActivity("Generated PDF", quotation.quotationNumber, "Exported PDF style: ${quotation.pdfStyle}")

        // Auto-save to Google Drive if connected and enabled, or explicitly requested
        val conn = driveConnection.value
        if (saveToDrive || conn?.autoSavePdf == true) {
            driveManager.uploadQuotationPdf(
                workspaceId = quotation.workspaceId,
                quotationId = quotation.id,
                quotationNumber = quotation.quotationNumber,
                revision = quotation.revision,
                clientName = client?.name,
                localPdfFile = pdfFile
            )
        }

        pdfFile
    }

    // Export Workspace Backup (ZIP)
    fun exportWorkspaceBackup(onCompleted: (File) -> Unit) {
        viewModelScope.launch {
            val ws = currentWorkspace.value
            val wsId = _currentWorkspaceId.value
            val quotes = allQuotations.value
            val clients = allClients.value
            val materials = allMaterials.value

            val jsonArray = JSONArray()
            quotes.forEach { q ->
                val obj = JSONObject().apply {
                    put("number", q.quotationNumber)
                    put("revision", q.revision)
                    put("date", q.date)
                    put("status", q.status)
                    put("style", q.pdfStyle)
                }
                jsonArray.put(obj)
            }

            val clientCsv = StringBuilder("Name,Company,Mobile,Email,GSTIN\n").apply {
                clients.forEach { c ->
                    append("\"${c.name}\",\"${c.companyName}\",\"${c.mobile}\",\"${c.email}\",\"${c.gstin}\"\n")
                }
            }.toString()

            val materialCsv = StringBuilder("Product,Category,Brand,Unit,Rate,TaxRate\n").apply {
                materials.forEach { m ->
                    append("\"${m.productName}\",\"${m.category}\",\"${m.brand}\",\"${m.unit}\",${m.defaultRate},${m.taxRate}\n")
                }
            }.toString()

            val zip = driveManager.createWorkspaceBackupZip(
                workspaceId = wsId,
                workspaceName = ws?.name ?: "QuoteForge",
                quotationsJson = jsonArray.toString(2),
                clientsCsv = clientCsv,
                materialsCsv = materialCsv
            )
            logActivity("Workspace Backup", null, "Created complete data ZIP backup (${zip.name})")
            onCompleted(zip)
        }
    }

    // Client CRM operations
    fun saveClient(client: ClientEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = quotationRepo.saveClient(client.copy(workspaceId = _currentWorkspaceId.value))
            logActivity("Saved client", null, "Saved client: ${client.name}")
            onSaved(id)
        }
    }

    fun deleteClient(client: ClientEntity) {
        viewModelScope.launch {
            quotationRepo.deleteClient(client)
            logActivity("Deleted client", null, "Removed client: ${client.name}")
        }
    }

    // Project operations
    fun saveProject(project: ProjectEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = quotationRepo.saveProject(project.copy(workspaceId = _currentWorkspaceId.value))
            logActivity("Saved project", null, "Saved project: ${project.name}")
            onSaved(id)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            quotationRepo.deleteProject(project)
            logActivity("Deleted project", null, "Removed project: ${project.name}")
        }
    }

    // Company operations
    fun saveCompany(company: CompanyEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = quotationRepo.saveCompany(company.copy(workspaceId = _currentWorkspaceId.value))
            onSaved(id)
        }
    }

    fun setDefaultCompany(companyId: Long) {
        viewModelScope.launch {
            quotationRepo.setDefaultCompany(companyId)
        }
    }

    // Material & Rate operations
    fun saveMaterial(material: MaterialEntity) {
        viewModelScope.launch {
            materialRateRepo.saveMaterial(material.copy(workspaceId = _currentWorkspaceId.value))
        }
    }

    fun deleteMaterial(material: MaterialEntity) {
        viewModelScope.launch {
            materialRateRepo.deleteMaterial(material)
        }
    }

    fun saveRate(rate: RateEntity) {
        viewModelScope.launch {
            materialRateRepo.saveRate(rate.copy(workspaceId = _currentWorkspaceId.value))
        }
    }

    fun deleteRate(rate: RateEntity) {
        viewModelScope.launch {
            materialRateRepo.deleteRate(rate)
        }
    }

    fun saveNumberingConfig(config: NumberingConfigEntity) {
        viewModelScope.launch {
            quotationRepo.updateNumberingConfig(config.copy(workspaceId = _currentWorkspaceId.value))
        }
    }
}
