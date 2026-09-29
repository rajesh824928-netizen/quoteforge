package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import com.example.domain.engine.IndustryPresets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        WorkspaceEntity::class,
        WorkspaceMemberEntity::class,
        ActivityLogEntity::class,
        BrandKitEntity::class,
        PdfTemplateEntity::class,
        DriveConnectionEntity::class,
        DriveFileEntity::class,
        CompanyEntity::class,
        ClientEntity::class,
        ProjectEntity::class,
        QuotationEntity::class,
        QuotationItemEntity::class,
        MaterialEntity::class,
        RateEntity::class,
        TermPresetEntity::class,
        NumberingConfigEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun workspaceMemberDao(): WorkspaceMemberDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun brandKitDao(): BrandKitDao
    abstract fun pdfTemplateDao(): PdfTemplateDao
    abstract fun driveDao(): DriveDao

    abstract fun companyDao(): CompanyDao
    abstract fun clientDao(): ClientDao
    abstract fun projectDao(): ProjectDao
    abstract fun quotationDao(): QuotationDao
    abstract fun quotationItemDao(): QuotationItemDao
    abstract fun materialDao(): MaterialDao
    abstract fun rateDao(): RateDao
    abstract fun termPresetDao(): TermPresetDao
    abstract fun numberingConfigDao(): NumberingConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quoteforge_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedInitialData(database)
                    }
                }
            }
        }

        suspend fun seedInitialData(database: AppDatabase) {
            val workspaceId = "ws_knot_arch"

            // 0. SaaS Seed: Demo Preview User
            database.userDao().insertUser(
                UserEntity(
                    id = "user_demo_knot",
                    email = "demo.knotarchitects@example.com",
                    displayName = "Demo Architect",
                    photoUrl = null,
                    authProvider = "demo",
                    defaultWorkspaceId = workspaceId
                )
            )

            // 1. SaaS Seed: Workspaces
            database.workspaceDao().insertWorkspace(
                WorkspaceEntity(
                    id = workspaceId,
                    name = "Knot Architects",
                    companyName = "Knot Architects & Interiors",
                    businessType = "Architecture",
                    country = "India",
                    currency = "INR",
                    currencySymbol = "₹",
                    ownerUserId = "user_demo_knot",
                    isDefault = true
                )
            )

            database.workspaceDao().insertWorkspace(
                WorkspaceEntity(
                    id = "ws_personal",
                    name = "Personal Workspace",
                    companyName = "Design Studio",
                    businessType = "Interior Design",
                    country = "India",
                    currency = "INR",
                    currencySymbol = "₹",
                    ownerUserId = "user_demo_knot",
                    isDefault = false
                )
            )

            database.workspaceDao().insertWorkspace(
                WorkspaceEntity(
                    id = "ws_demo_const",
                    name = "Demo Construction Co.",
                    companyName = "Apex Builders & Contractors",
                    businessType = "Construction",
                    country = "India",
                    currency = "INR",
                    currencySymbol = "₹",
                    ownerUserId = "user_demo_knot",
                    isDefault = false
                )
            )

            // 2. SaaS Seed: Workspace Members
            val members = listOf(
                WorkspaceMemberEntity(
                    workspaceId = workspaceId,
                    userId = "user_demo_knot",
                    userName = "Demo Architect",
                    userEmail = "demo.knotarchitects@example.com",
                    role = "OWNER",
                    status = "ACTIVE"
                ),
                WorkspaceMemberEntity(
                    workspaceId = workspaceId,
                    userId = "user_anu",
                    userName = "Ar. Anu Sharma",
                    userEmail = "anu.sharma@knotarchitects.in",
                    role = "DESIGNER",
                    status = "ACTIVE"
                ),
                WorkspaceMemberEntity(
                    workspaceId = workspaceId,
                    userId = "user_arun",
                    userName = "Arun Kumar",
                    userEmail = "arun.pm@knotarchitects.in",
                    role = "MANAGER",
                    status = "ACTIVE"
                ),
                WorkspaceMemberEntity(
                    workspaceId = workspaceId,
                    userId = "user_priya",
                    userName = "Priya Nair",
                    userEmail = "accounts@knotarchitects.in",
                    role = "ACCOUNTANT",
                    status = "ACTIVE"
                ),
                WorkspaceMemberEntity(
                    workspaceId = workspaceId,
                    userId = "user_sree",
                    userName = "Sreekanth M",
                    userEmail = "sreekanth.jnr@knotarchitects.in",
                    role = "DESIGNER",
                    status = "PENDING"
                )
            )
            database.workspaceMemberDao().insertMembers(members)

            // 3. SaaS Seed: Brand Kits
            database.brandKitDao().insertBrandKit(
                BrandKitEntity(
                    workspaceId = workspaceId,
                    name = "Knot Luxury Architectural Gold",
                    primaryColor = "#A3834C", // Luxury Gold
                    secondaryColor = "#1F1F1F", // Architectural Black
                    watermarkEnabled = true,
                    watermarkType = "STATUS",
                    watermarkText = "DRAFT",
                    isDefault = true
                )
            )

            database.brandKitDao().insertBrandKit(
                BrandKitEntity(
                    workspaceId = workspaceId,
                    name = "Corporate Slate & Navy",
                    primaryColor = "#1E3A5F", // Navy
                    secondaryColor = "#475569", // Slate
                    watermarkEnabled = false,
                    isDefault = false
                )
            )

            // 4. SaaS Seed: PDF Templates (3 distinct styles)
            val templates = listOf(
                PdfTemplateEntity(
                    id = "style_minimal",
                    workspaceId = workspaceId,
                    name = "Minimal Studio (Architectural)",
                    styleCode = "MINIMAL_STUDIO",
                    accentColor = "#1F1F1F",
                    isWorkspaceDefault = true
                ),
                PdfTemplateEntity(
                    id = "style_corporate",
                    workspaceId = workspaceId,
                    name = "Professional Corporate (Contractors)",
                    styleCode = "PROFESSIONAL_CORPORATE",
                    accentColor = "#1E3A5F",
                    isWorkspaceDefault = true
                ),
                PdfTemplateEntity(
                    id = "style_architectural",
                    workspaceId = workspaceId,
                    name = "Premium Architectural Proposal",
                    styleCode = "PREMIUM_ARCHITECTURAL",
                    accentColor = "#A3834C",
                    isWorkspaceDefault = true
                )
            )
            database.pdfTemplateDao().insertTemplates(templates)

            // 5. SaaS Seed: Google Drive Connection (starts disconnected so user can connect their own real account)
            database.driveDao().insertOrUpdateConnection(
                DriveConnectionEntity(
                    workspaceId = workspaceId,
                    accountEmail = "",
                    accountName = "",
                    isConnected = false,
                    driveType = "MY_DRIVE",
                    rootFolderId = "QuotationApp",
                    autoSavePdf = true,
                    lastSyncedAt = 0L,
                    syncStatus = "DISCONNECTED"
                )
            )

            // 6. SaaS Seed: Activity Logs
            val currentTime = System.currentTimeMillis()
            database.activityLogDao().insertLog(
                ActivityLogEntity(
                    workspaceId = workspaceId,
                    userId = "user_demo_knot",
                    userName = "Demo Architect",
                    action = "Created quotation",
                    documentNumber = "QT-2026-001",
                    details = "Initiated quotation for Suresh Babu (Luxury Villa Interior)",
                    timestamp = currentTime - 86_400_000
                )
            )
            database.activityLogDao().insertLog(
                ActivityLogEntity(
                    workspaceId = workspaceId,
                    userId = "user_anu",
                    userName = "Anu Sharma",
                    action = "Revised quotation",
                    documentNumber = "QT-2026-001",
                    details = "Updated wardrobe specifications and created Rev.01",
                    timestamp = currentTime - 36_000_000
                )
            )
            database.activityLogDao().insertLog(
                ActivityLogEntity(
                    workspaceId = workspaceId,
                    userId = "user_arun",
                    userName = "Arun Kumar",
                    action = "Generated PDF",
                    documentNumber = "QT-2026-001_Rev01",
                    details = "Exported 3-page A4 Architectural Proposal",
                    timestamp = currentTime - 7_200_000
                )
            )
            database.activityLogDao().insertLog(
                ActivityLogEntity(
                    workspaceId = workspaceId,
                    userId = "user_rajesh",
                    userName = "Rajesh",
                    action = "Drive sync",
                    documentNumber = "QT-2026-001_Rev01",
                    details = "Synced to QuotationApp/Quotations/QT-2026-001_Rev01_Suresh-Babu.pdf",
                    timestamp = currentTime - 120_000
                )
            )

            // 7. Numbering Config
            database.numberingConfigDao().saveConfig(
                NumberingConfigEntity(
                    id = 1,
                    workspaceId = workspaceId,
                    prefix = "KA",
                    includeYear = true,
                    includeMonth = false,
                    digits = 3,
                    nextSequence = 2
                )
            )

            // 8. Preload Company Profiles
            val knotArchitectsId = database.companyDao().insertCompany(
                CompanyEntity(
                    workspaceId = workspaceId,
                    name = "Knot Architects & Interiors",
                    address = "#42, 3rd Floor, Design Avenue, Indiranagar, Bangalore - 560038",
                    phone = "+91 98765 43210",
                    email = "knotarchitects@gmail.com",
                    website = "www.knotarchitects.in",
                    gstin = "29AABCK1234F1Z8",
                    pan = "AABCK1234F",
                    bankName = "HDFC Bank",
                    accountNumber = "50200012345678",
                    ifsc = "HDFC0001234",
                    upiId = "knotarchitects@hdfcbank",
                    signatureName = "Ar. Rohit Varma (Principal Architect)",
                    isDefault = true
                )
            )

            database.companyDao().insertCompany(
                CompanyEntity(
                    workspaceId = workspaceId,
                    name = "Knot Modular Living",
                    address = "Plot 8B, Industrial Suburb, Peenya, Bangalore - 560058",
                    phone = "+91 98765 12345",
                    email = "factory@knotliving.in",
                    website = "www.knotliving.in",
                    gstin = "29AABCK5678F1Z2",
                    pan = "AABCK5678F",
                    bankName = "ICICI Bank",
                    accountNumber = "000205012345",
                    ifsc = "ICIC0000002",
                    upiId = "knotmodular@icici",
                    signatureName = "Works Director",
                    isDefault = false
                )
            )

            // 9. Preload Sample Clients
            val clientSureshId = database.clientDao().insertClient(
                ClientEntity(
                    workspaceId = workspaceId,
                    name = "Mr. Suresh Babu",
                    companyName = "Suresh Tech Solutions Pvt Ltd",
                    mobile = "+91 98450 98765",
                    email = "suresh.babu@techsolutions.com",
                    address = "Plot #14, Palm Meadows, Whitefield, Bangalore",
                    gstin = "29AAECS4321P1Z5",
                    pan = "AAECS4321P",
                    state = "Karnataka",
                    city = "Bangalore",
                    notes = "Preferred style: Contemporary warm minimalist with fluted oak and quartz accents."
                )
            )

            val clientPriyaId = database.clientDao().insertClient(
                ClientEntity(
                    workspaceId = workspaceId,
                    name = "Dr. Priya Nair",
                    companyName = "Nair Medical Clinic",
                    mobile = "+91 99001 22334",
                    email = "priya.nair@apollohospitals.org",
                    address = "Apartment 1204, Sobha Forest Edge, Kanakapura Road, Bangalore",
                    gstin = "",
                    pan = "ANBPN9876K",
                    state = "Karnataka",
                    city = "Bangalore"
                )
            )

            // 10. Preload Sample Projects
            val projectVillaId = database.projectDao().insertProject(
                ProjectEntity(
                    workspaceId = workspaceId,
                    clientId = clientSureshId,
                    name = "Luxury Villa Interior",
                    projectType = "Villa",
                    address = "Plot 14, Palm Meadows, Whitefield, Bangalore",
                    area = "3,800 sq.ft",
                    description = "Complete interior design, modular woodwork, false ceiling, and decorative lighting for 4BHK luxury villa.",
                    budget = 2800000.0
                )
            )

            // 11. Preload Sample Quotations
            val sampleQuotationId = database.quotationDao().insertQuotation(
                QuotationEntity(
                    workspaceId = workspaceId,
                    quotationNumber = "KA-2026-001",
                    revision = 0,
                    companyId = knotArchitectsId,
                    clientId = clientSureshId,
                    projectId = projectVillaId,
                    date = System.currentTimeMillis() - 86400000,
                    validityDays = 30,
                    status = "Sent",
                    calculationMode = "SELLING_PRICE",
                    profitMarginPercent = 22.0,
                    isGstInclusive = false,
                    gstType = "CGST_SGST",
                    taxRate = 18.0,
                    discountType = "PERCENTAGE",
                    discountValue = 5.0,
                    templateId = "style_architectural",
                    pdfStyle = "PREMIUM_ARCHITECTURAL",
                    templatePrimaryColor = "#A3834C",
                    watermarkEnabled = true,
                    watermarkType = "STATUS",
                    watermarkText = "CONFIDENTIAL",
                    watermarkOpacity = 0.12f,
                    createdByName = "Demo Architect",
                    createdByUserId = "user_demo_knot",
                    driveSyncStatus = "SYNCED",
                    driveFileId = "drive_file_001",
                    scopeIncluded = "All modular woodwork, customized false ceiling, PU polish, and electrical lighting installation.",
                    scopeExcluded = "HVAC external compressors, movable loose furniture, and civil wall structural modifications.",
                    materialSpecs = "Century Club Prime IS:710 Marine Grade BWP Plywood, 1mm Merino high-pressure matte laminate, and Blum soft-close fittings.",
                    warrantyTerms = "10 Years comprehensive manufacturing defect warranty on cabinetry and hardware.",
                    timeline = "60 to 75 working days from site handover and approved drawing sign-off.",
                    siteConditions = "Power supply 3-phase and dedicated water connection to be provided by client at site.",
                    termsAndConditions = "• 50% mobilization advance upon contract sign-off.\n• 40% upon modular factory delivery before installation.\n• 10% on completion and client handover.\n• Taxes applicable as per GST regulations.",
                    paymentMilestonesJson = """[{"name":"Advance on Signing","percentage":50.0},{"name":"Factory Dispatch","percentage":40.0},{"name":"Handover","percentage":10.0}]""",
                    isEstimate = false,
                    notes = "Approved architectural drawing pack rev-B attached."
                )
            )

            // Sample Quotation Items
            database.quotationItemDao().insertItems(
                listOf(
                    QuotationItemEntity(
                        quotationId = sampleQuotationId,
                        roomName = "Master Bedroom",
                        category = "Modular Furniture",
                        itemName = "Floor-to-Ceiling Wardrobe with Fluted Details",
                        description = "Full height sliding wardrobe (9ft x 7ft) in Century Club Prime 710 marine ply, 1mm Merino anti-fingerprint fluted laminate, profile LED lighting, and Blum soft-close sliders.",
                        specification = "18mm BWP marine ply, Merino laminate, Blum hardware",
                        unit = "sq.ft",
                        quantity = 63.0,
                        rate = 1850.0,
                        materialCost = 1100.0,
                        labourCost = 350.0,
                        sortOrder = 1
                    ),
                    QuotationItemEntity(
                        quotationId = sampleQuotationId,
                        roomName = "Master Bedroom",
                        category = "Modular Furniture",
                        itemName = "King Size Bed with Upholstered Headboard",
                        description = "King size hydraulic storage bed with hydraulic lift mechanism (German Gas Lift 1500N), cushioned headboard in premium stain-resistant bouclé fabric.",
                        specification = "Century BWP Plywood, Ebco heavy-duty hydraulics",
                        unit = "nos",
                        quantity = 1.0,
                        rate = 68000.0,
                        materialCost = 42000.0,
                        labourCost = 12000.0,
                        sortOrder = 2
                    ),
                    QuotationItemEntity(
                        quotationId = sampleQuotationId,
                        roomName = "Living & Foyer",
                        category = "Ceiling & Partitions",
                        itemName = "Acoustic Gypsum False Ceiling with Cove Lighting",
                        description = "Saint-Gobain Gyproc 12.5mm plasterboard ceiling with GI channels, perimeter cove for indirect warm LED illumination, and finished in Royale luxury emulsion.",
                        specification = "Saint-Gobain Gyproc, Asian Paints Royale Luxury",
                        unit = "sq.ft",
                        quantity = 420.0,
                        rate = 145.0,
                        materialCost = 75.0,
                        labourCost = 45.0,
                        sortOrder = 3
                    ),
                    QuotationItemEntity(
                        quotationId = sampleQuotationId,
                        roomName = "Living & Foyer",
                        category = "Surfaces & Wall Panelling",
                        itemName = "Charcoal & Veneer TV Feature Wall",
                        description = "TV console feature wall in smoked natural oak veneer with vertical charcoal louvers, concealed cable management channels, and floating drawer console.",
                        specification = "Smoked Oak natural veneer with melamine coat",
                        unit = "sq.ft",
                        quantity = 96.0,
                        rate = 950.0,
                        materialCost = 550.0,
                        labourCost = 220.0,
                        sortOrder = 4
                    ),
                    QuotationItemEntity(
                        quotationId = sampleQuotationId,
                        roomName = "Kitchen",
                        category = "Modular Kitchen",
                        itemName = "Island Modular Kitchen with Blum Tandembox",
                        description = "Ergonomic modular kitchen with BWP grade marine plywood carcass, 2mm anti-scratch Senosan acrylic shutters, Blum Tandembox antaro soft-close drawers, and Hafele corner carousel.",
                        specification = "Century 710 marine ply, Senosan acrylic, Blum hardware",
                        unit = "sq.ft",
                        quantity = 110.0,
                        rate = 2200.0,
                        materialCost = 1350.0,
                        labourCost = 450.0,
                        sortOrder = 5
                    )
                )
            )

            // 12. Preload Materials Library
            val materials = listOf(
                MaterialEntity(workspaceId = workspaceId, productName = "18mm BWP Marine Plywood", brand = "Century Ply / Greenply", category = "Plywood & Boards", specification = "IS:710 Boiling Water Proof, 100% Calibrated", unit = "sq.ft", defaultRate = 185.0, taxRate = 18.0, supplier = "Sri Balaji Plywoods"),
                MaterialEntity(workspaceId = workspaceId, productName = "18mm MR Grade Commercial Plywood", brand = "Century / Kitply", category = "Plywood & Boards", specification = "IS:303 Moisture Resistant Interior Grade", unit = "sq.ft", defaultRate = 115.0, taxRate = 18.0, supplier = "Sri Balaji Plywoods"),
                MaterialEntity(workspaceId = workspaceId, productName = "18mm HDHMR High Density Board", brand = "Action Tesa", category = "Plywood & Boards", specification = "High Density High Moisture Resistance 850 kg/m³", unit = "sq.ft", defaultRate = 98.0, taxRate = 18.0, supplier = "Action Tesa Hub"),
                MaterialEntity(workspaceId = workspaceId, productName = "1mm Decorative Laminate", brand = "Greenlam / Merino", category = "Surfaces & Veneers", specification = "Anti-fingerprint suede and textured collection", unit = "sq.ft", defaultRate = 125.0, taxRate = 18.0, supplier = "Merino Gallery"),
                MaterialEntity(workspaceId = workspaceId, productName = "2mm Anti-Scratch Acrylic Sheet", brand = "Senosan / Europratik", category = "Surfaces & Veneers", specification = "High gloss scratch-resistant polymer sheet", unit = "sq.ft", defaultRate = 260.0, taxRate = 18.0, supplier = "Europratik"),
                MaterialEntity(workspaceId = workspaceId, productName = "Soft-Close Concealed Hinges (Pair)", brand = "Hettich / Hafele", category = "Hardware & Fittings", specification = "Sensys 110 degree clip-on with integrated soft-close", unit = "set", defaultRate = 480.0, taxRate = 18.0, supplier = "Hettich Studio"),
                MaterialEntity(workspaceId = workspaceId, productName = "Tandem Drawer Box 500mm", brand = "Hettich InnoTech", category = "Hardware & Fittings", specification = "Full extension soft-close 30kg load capacity", unit = "set", defaultRate = 3200.0, taxRate = 18.0, supplier = "Hettich Studio"),
                MaterialEntity(workspaceId = workspaceId, productName = "Gypsum False Ceiling Board", brand = "Saint-Gobain Gyproc", category = "Ceiling & Partitions", specification = "12.5mm standard tapered edge plasterboard", unit = "sq.ft", defaultRate = 42.0, taxRate = 18.0, supplier = "Gyproc Distributors"),
                MaterialEntity(workspaceId = workspaceId, productName = "Italian Botticino Marble", brand = "Imported", category = "Flooring & Tiling", specification = "18mm thick honed and polished Italian marble slabs", unit = "sq.ft", defaultRate = 450.0, taxRate = 18.0, supplier = "Royal Marbles")
            )
            database.materialDao().insertMaterials(materials)

            // 13. Preload Rates Library
            val rates = listOf(
                RateEntity(workspaceId = workspaceId, name = "18mm BWP Marine Plywood Supply", category = "Material", unit = "sq.ft", rate = 185.0),
                RateEntity(workspaceId = workspaceId, name = "1mm Laminate Pressing & Supply", category = "Material", unit = "sq.ft", rate = 125.0),
                RateEntity(workspaceId = workspaceId, name = "Carpenter Daily Rate", category = "Labour", unit = "day", rate = 1200.0),
                RateEntity(workspaceId = workspaceId, name = "Painter Daily Rate", category = "Labour", unit = "day", rate = 1000.0),
                RateEntity(workspaceId = workspaceId, name = "Electrician Daily Rate", category = "Labour", unit = "day", rate = 1100.0),
                RateEntity(workspaceId = workspaceId, name = "Modular Kitchen Factory Assembly & Installation", category = "Installation", unit = "sq.ft", rate = 350.0),
                RateEntity(workspaceId = workspaceId, name = "Wardrobe Site Fitment & Alignment", category = "Installation", unit = "sq.ft", rate = 250.0),
                RateEntity(workspaceId = workspaceId, name = "Architectural Site Supervision & Consultation", category = "Service", unit = "hour", rate = 2500.0)
            )
            database.rateDao().insertRates(rates)

            // 14. Preload Terms Presets
            IndustryPresets.PRESET_TERMS.forEach { preset ->
                database.termPresetDao().insertTerm(
                    TermPresetEntity(
                        workspaceId = workspaceId,
                        title = preset.title,
                        category = preset.category,
                        content = preset.clauses.joinToString("\n• ", prefix = "• ")
                    )
                )
            }
        }
    }
}
