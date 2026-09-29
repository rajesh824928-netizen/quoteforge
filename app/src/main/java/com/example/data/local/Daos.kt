package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// SAAS DAOS
// ==========================================

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    fun getFirstUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspaces ORDER BY isDefault DESC, name ASC")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE id = :id")
    suspend fun getWorkspaceById(id: String): WorkspaceEntity?

    @Query("SELECT * FROM workspaces WHERE id = :id")
    fun observeWorkspaceById(id: String): Flow<WorkspaceEntity?>

    @Query("SELECT * FROM workspaces WHERE isDefault = 1 LIMIT 1")
    fun getDefaultWorkspace(): Flow<WorkspaceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity)

    @Update
    suspend fun updateWorkspace(workspace: WorkspaceEntity)

    @Delete
    suspend fun deleteWorkspace(workspace: WorkspaceEntity)
}

@Dao
interface WorkspaceMemberDao {
    @Query("SELECT * FROM workspace_members WHERE workspaceId = :workspaceId ORDER BY role ASC, userName ASC")
    fun getMembersForWorkspace(workspaceId: String): Flow<List<WorkspaceMemberEntity>>

    @Query("SELECT * FROM workspace_members WHERE workspaceId = :workspaceId AND userId = :userId LIMIT 1")
    suspend fun getMember(workspaceId: String, userId: String): WorkspaceMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: WorkspaceMemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<WorkspaceMemberEntity>)

    @Update
    suspend fun updateMember(member: WorkspaceMemberEntity)

    @Delete
    suspend fun deleteMember(member: WorkspaceMemberEntity)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs WHERE workspaceId = :workspaceId ORDER BY timestamp DESC LIMIT 50")
    fun getLogsForWorkspace(workspaceId: String): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity): Long
}

@Dao
interface BrandKitDao {
    @Query("SELECT * FROM brand_kits WHERE workspaceId = :workspaceId ORDER BY isDefault DESC, name ASC")
    fun getBrandKitsForWorkspace(workspaceId: String): Flow<List<BrandKitEntity>>

    @Query("SELECT * FROM brand_kits WHERE workspaceId = :workspaceId AND isDefault = 1 LIMIT 1")
    fun getDefaultBrandKit(workspaceId: String): Flow<BrandKitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrandKit(kit: BrandKitEntity): Long

    @Update
    suspend fun updateBrandKit(kit: BrandKitEntity)

    @Delete
    suspend fun deleteBrandKit(kit: BrandKitEntity)
}

@Dao
interface PdfTemplateDao {
    @Query("SELECT * FROM pdf_templates WHERE workspaceId = :workspaceId ORDER BY isWorkspaceDefault DESC, name ASC")
    fun getTemplatesForWorkspace(workspaceId: String): Flow<List<PdfTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: PdfTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<PdfTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: PdfTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: PdfTemplateEntity)
}

@Dao
interface DriveDao {
    @Query("SELECT * FROM drive_connections WHERE workspaceId = :workspaceId LIMIT 1")
    fun getDriveConnection(workspaceId: String): Flow<DriveConnectionEntity?>

    @Query("SELECT * FROM drive_connections WHERE workspaceId = :workspaceId LIMIT 1")
    suspend fun getDriveConnectionSync(workspaceId: String): DriveConnectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConnection(connection: DriveConnectionEntity)

    @Query("SELECT * FROM drive_files WHERE workspaceId = :workspaceId ORDER BY uploadedAt DESC")
    fun getDriveFiles(workspaceId: String): Flow<List<DriveFileEntity>>

    @Query("SELECT * FROM drive_files WHERE workspaceId = :workspaceId AND quotationId = :quotationId AND revision = :revision LIMIT 1")
    suspend fun getDriveFileForQuotation(workspaceId: String, quotationId: Long, revision: Int): DriveFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriveFile(file: DriveFileEntity): Long

    @Update
    suspend fun updateDriveFile(file: DriveFileEntity)
}

// ==========================================
// QUOTATION, CLIENT & PROJECT DAOS
// ==========================================

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies WHERE workspaceId = :workspaceId ORDER BY isDefault DESC, name ASC")
    fun getCompaniesForWorkspace(workspaceId: String): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies ORDER BY isDefault DESC, name ASC")
    fun getAllCompanies(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies WHERE isDefault = 1 LIMIT 1")
    fun getDefaultCompany(): Flow<CompanyEntity?>

    @Query("SELECT * FROM companies WHERE id = :id")
    suspend fun getCompanyById(id: Long): CompanyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: CompanyEntity): Long

    @Update
    suspend fun updateCompany(company: CompanyEntity)

    @Delete
    suspend fun deleteCompany(company: CompanyEntity)

    @Query("UPDATE companies SET isDefault = CASE WHEN id = :companyId THEN 1 ELSE 0 END")
    suspend fun setDefaultCompany(companyId: Long)
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients WHERE workspaceId = :workspaceId ORDER BY name ASC")
    fun getClientsForWorkspace(workspaceId: String): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: Long): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity): Long

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Delete
    suspend fun deleteClient(client: ClientEntity)

    @Query("SELECT COUNT(*) FROM clients WHERE workspaceId = :workspaceId")
    suspend fun getClientCount(workspaceId: String): Int

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun getClientCount(): Int
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE workspaceId = :workspaceId ORDER BY updatedAt DESC")
    fun getProjectsForWorkspace(workspaceId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE clientId = :clientId ORDER BY updatedAt DESC")
    fun getProjectsForClient(clientId: Long): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}

@Dao
interface QuotationDao {
    @Query("SELECT * FROM quotations WHERE workspaceId = :workspaceId ORDER BY date DESC, id DESC")
    fun getQuotationsForWorkspace(workspaceId: String): Flow<List<QuotationEntity>>

    @Query("SELECT * FROM quotations ORDER BY date DESC, id DESC")
    fun getAllQuotations(): Flow<List<QuotationEntity>>

    @Query("SELECT * FROM quotations WHERE clientId = :clientId ORDER BY date DESC")
    fun getQuotationsForClient(clientId: Long): Flow<List<QuotationEntity>>

    @Query("SELECT * FROM quotations WHERE projectId = :projectId ORDER BY date DESC")
    fun getQuotationsForProject(projectId: Long): Flow<List<QuotationEntity>>

    @Query("SELECT * FROM quotations WHERE id = :id")
    suspend fun getQuotationById(id: Long): QuotationEntity?

    @Query("SELECT * FROM quotations WHERE id = :id")
    fun observeQuotationById(id: Long): Flow<QuotationEntity?>

    @Query("SELECT * FROM quotations WHERE quotationNumber = :number")
    suspend fun getQuotationsByNumber(number: String): List<QuotationEntity>

    @Query("SELECT * FROM quotations WHERE parentQuotationId = :parentId OR id = :parentId ORDER BY revision ASC")
    fun getRevisionsForQuotation(parentId: Long): Flow<List<QuotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotation(quotation: QuotationEntity): Long

    @Update
    suspend fun updateQuotation(quotation: QuotationEntity)

    @Query("UPDATE quotations SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE quotations SET driveSyncStatus = :status, driveFileId = :driveFileId WHERE id = :id")
    suspend fun updateDriveSyncStatus(id: Long, status: String, driveFileId: String?)

    @Delete
    suspend fun deleteQuotation(quotation: QuotationEntity)

    @Query("SELECT COUNT(*) FROM quotations WHERE workspaceId = :workspaceId")
    suspend fun getQuotationCount(workspaceId: String): Int

    @Query("SELECT COUNT(*) FROM quotations")
    suspend fun getQuotationCount(): Int
}

@Dao
interface QuotationItemDao {
    @Query("SELECT * FROM quotation_items WHERE quotationId = :quotationId ORDER BY sortOrder ASC, id ASC")
    fun getItemsForQuotation(quotationId: Long): Flow<List<QuotationItemEntity>>

    @Query("SELECT * FROM quotation_items WHERE quotationId = :quotationId ORDER BY sortOrder ASC, id ASC")
    suspend fun getItemsForQuotationSync(quotationId: Long): List<QuotationItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: QuotationItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<QuotationItemEntity>)

    @Update
    suspend fun updateItem(item: QuotationItemEntity)

    @Delete
    suspend fun deleteItem(item: QuotationItemEntity)

    @Query("DELETE FROM quotation_items WHERE quotationId = :quotationId")
    suspend fun deleteItemsForQuotation(quotationId: Long)
}

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials WHERE workspaceId = :workspaceId ORDER BY productName ASC")
    fun getMaterialsForWorkspace(workspaceId: String): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials ORDER BY productName ASC")
    fun getAllMaterials(): Flow<List<MaterialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: MaterialEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(materials: List<MaterialEntity>)

    @Update
    suspend fun updateMaterial(material: MaterialEntity)

    @Delete
    suspend fun deleteMaterial(material: MaterialEntity)

    @Query("SELECT COUNT(*) FROM materials")
    suspend fun getMaterialCount(): Int
}

@Dao
interface RateDao {
    @Query("SELECT * FROM rates WHERE workspaceId = :workspaceId ORDER BY name ASC")
    fun getRatesForWorkspace(workspaceId: String): Flow<List<RateEntity>>

    @Query("SELECT * FROM rates ORDER BY name ASC")
    fun getAllRates(): Flow<List<RateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: RateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<RateEntity>)

    @Update
    suspend fun updateRate(rate: RateEntity)

    @Delete
    suspend fun deleteRate(rate: RateEntity)

    @Query("SELECT COUNT(*) FROM rates")
    suspend fun getRateCount(): Int
}

@Dao
interface TermPresetDao {
    @Query("SELECT * FROM term_presets WHERE workspaceId = :workspaceId ORDER BY title ASC")
    fun getTermsForWorkspace(workspaceId: String): Flow<List<TermPresetEntity>>

    @Query("SELECT * FROM term_presets ORDER BY title ASC")
    fun getAllTerms(): Flow<List<TermPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerm(term: TermPresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerms(terms: List<TermPresetEntity>)

    @Query("SELECT COUNT(*) FROM term_presets")
    suspend fun getTermsCount(): Int
}

@Dao
interface NumberingConfigDao {
    @Query("SELECT * FROM numbering_config WHERE workspaceId = :workspaceId LIMIT 1")
    fun getConfigForWorkspace(workspaceId: String): Flow<NumberingConfigEntity?>

    @Query("SELECT * FROM numbering_config WHERE id = 1")
    fun getConfig(): Flow<NumberingConfigEntity?>

    @Query("SELECT * FROM numbering_config WHERE id = 1")
    suspend fun getConfigSync(): NumberingConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: NumberingConfigEntity)
}
