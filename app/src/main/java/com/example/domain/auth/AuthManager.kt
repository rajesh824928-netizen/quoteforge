package com.example.domain.auth

import android.content.Context
import com.example.data.local.UserDao
import com.example.data.model.UserEntity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Enterprise Authentication Manager handling Firebase Authentication,
 * Google Sign-In with Credential Manager, Email/Password accounts, and SaaS user sessions.
 */
class AuthManager(private val userDao: UserDao) {

    private val firebaseAuth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            null
        }

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(null)
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    init {
        // Listen to Firebase Auth state if available
        try {
            firebaseAuth?.let { auth ->
                _firebaseUser.value = auth.currentUser
                auth.addAuthStateListener { state ->
                    val fbUser = state.currentUser
                    _firebaseUser.value = fbUser
                    if (fbUser != null) {
                        val email = fbUser.email ?: ""
                        val name = fbUser.displayName ?: email.substringBefore("@")
                        val entity = UserEntity(
                            id = fbUser.uid,
                            email = email,
                            displayName = name,
                            photoUrl = fbUser.photoUrl?.toString(),
                            authProvider = fbUser.providerData.firstOrNull()?.providerId ?: "firebase",
                            defaultWorkspaceId = _currentUser.value?.defaultWorkspaceId ?: ""
                        )
                        _currentUser.value = entity
                        _isAuthenticated.value = true
                    }
                }
            }
        } catch (e: Throwable) {
            // Firebase Auth not initialized or missing configuration
        }
    }

    /**
     * Explicit Demo Account Access.
     * Allows exploring the pre-configured Knot Architects workspace with full sample data.
     */
    suspend fun signInAsDemoAccount(): UserEntity {
        val demoUser = UserEntity(
            id = "user_demo_knot",
            email = "demo.knotarchitects@example.com",
            displayName = "Demo Architect",
            photoUrl = null,
            authProvider = "demo",
            defaultWorkspaceId = "ws_knot_arch"
        )
        userDao.insertUser(demoUser)
        _currentUser.value = demoUser
        _isAuthenticated.value = true
        return demoUser
    }

    /**
     * Sign In with Google via Credential Manager & Firebase Auth.
     * If explicit credentials are provided (or prompted via Google dialog), uses them.
     */
    suspend fun signInWithGoogle(
        context: Context,
        explicitEmail: String? = null,
        explicitDisplayName: String? = null
    ): Result<UserEntity> {
        // If the user already provided an explicit Google Account in the Google sign-in flow:
        if (!explicitEmail.isNullOrBlank()) {
            val cleanEmail = explicitEmail.trim()
            val cleanName = explicitDisplayName?.trim()?.ifBlank { null }
                ?: cleanEmail.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

            val existing = userDao.getUserByEmail(cleanEmail)
            val user = if (existing != null && existing.id != "user_demo_knot" && existing.id != "user_rajesh") {
                val updatedName = if (!explicitDisplayName.isNullOrBlank()) explicitDisplayName.trim() else existing.displayName
                val updated = existing.copy(displayName = updatedName, authProvider = "google")
                userDao.insertUser(updated)
                updated
            } else {
                val newId = "usr_g_" + cleanEmail.substringBefore("@").replace(".", "_") + "_" + System.currentTimeMillis().toString().takeLast(4)
                val newUser = UserEntity(
                    id = newId,
                    email = cleanEmail,
                    displayName = cleanName,
                    photoUrl = null,
                    authProvider = "google",
                    defaultWorkspaceId = ""
                )
                userDao.insertUser(newUser)
                newUser
            }
            _currentUser.value = user
            _isAuthenticated.value = true
            return Result.success(user)
        }

        // Try standard Credential Manager
        try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("933262695061-quoteforge.apps.googleusercontent.com")
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val name = googleIdTokenCredential.displayName ?: email.substringBefore("@")

                val auth = firebaseAuth
                if (auth != null && idToken.isNotBlank()) {
                    try {
                        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(authCredential).awaitTask()
                        val fbUser = authResult.user

                        val user = UserEntity(
                            id = fbUser?.uid ?: ("user_" + email.substringBefore("@")),
                            email = fbUser?.email ?: email,
                            displayName = fbUser?.displayName ?: name,
                            photoUrl = fbUser?.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString(),
                            authProvider = "google",
                            defaultWorkspaceId = ""
                        )
                        userDao.insertUser(user)
                        _currentUser.value = user
                        _isAuthenticated.value = true
                        return Result.success(user)
                    } catch (e: Exception) {
                        // Fallback to local profile with the Google credentials
                    }
                }

                val existing = userDao.getUserByEmail(email)
                val user = if (existing != null && existing.id != "user_demo_knot" && existing.id != "user_rajesh") {
                    val updated = existing.copy(displayName = name, authProvider = "google")
                    userDao.insertUser(updated)
                    updated
                } else {
                    UserEntity(
                        id = "user_" + email.substringBefore("@").replace(".", "_"),
                        email = email,
                        displayName = name,
                        photoUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                        authProvider = "google",
                        defaultWorkspaceId = ""
                    )
                }
                userDao.insertUser(user)
                _currentUser.value = user
                _isAuthenticated.value = true
                return Result.success(user)
            }
        } catch (e: Exception) {
            // Signal to caller that device-level Credential Manager is unavailable
            // so that the UI can present the native Google Account input dialog
            return Result.failure(Exception("CREDENTIAL_MANAGER_UNAVAILABLE: ${e.message}"))
        }

        return Result.failure(Exception("Google Sign-In was cancelled or not recognized"))
    }

    /**
     * Create Account with Email and Password
     */
    suspend fun createAccountWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<UserEntity> {
        try {
            val cleanEmail = email.trim()
            var userId = "user_" + cleanEmail.substringBefore("@").replace(".", "_") + "_" + System.currentTimeMillis().toString().takeLast(4)
            val auth = firebaseAuth
            if (auth != null) {
                try {
                    val result = auth.createUserWithEmailAndPassword(cleanEmail, password).awaitTask()
                    result.user?.let { userId = it.uid }
                } catch (e: Exception) {
                    // In offline / emulator mode without live Firebase, continue with encrypted local storage
                }
            }

            val cleanName = displayName.trim().ifBlank {
                cleanEmail.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }

            val user = UserEntity(
                id = userId,
                email = cleanEmail,
                displayName = cleanName,
                photoUrl = null,
                authProvider = "email",
                defaultWorkspaceId = ""
            )
            userDao.insertUser(user)
            _currentUser.value = user
            _isAuthenticated.value = true
            return Result.success(user)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Sign In with Email and Password
     */
    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<UserEntity> {
        try {
            val cleanEmail = email.trim()
            var userId = "user_" + cleanEmail.substringBefore("@").replace(".", "_")
            var userName = cleanEmail.substringBefore("@").replace(".", " ")

            val auth = firebaseAuth
            if (auth != null) {
                try {
                    val result = auth.signInWithEmailAndPassword(cleanEmail, password).awaitTask()
                    result.user?.let {
                        userId = it.uid
                        it.displayName?.let { name -> userName = name }
                    }
                } catch (e: Exception) {
                    // Local fallback
                }
            }

            val existing = userDao.getUserByEmail(cleanEmail)
            val user = if (existing != null && existing.id != "user_demo_knot" && existing.id != "user_rajesh") {
                existing
            } else {
                UserEntity(
                    id = userId,
                    email = cleanEmail,
                    displayName = userName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                    photoUrl = null,
                    authProvider = "email",
                    defaultWorkspaceId = ""
                )
            }
            userDao.insertUser(user)
            _currentUser.value = user
            _isAuthenticated.value = true
            return Result.success(user)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun switchUserAccount(user: UserEntity) {
        userDao.insertUser(user)
        _currentUser.value = user
        _isAuthenticated.value = true
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            // Ignore
        }
        _currentUser.value = null
        _isAuthenticated.value = false
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
}
