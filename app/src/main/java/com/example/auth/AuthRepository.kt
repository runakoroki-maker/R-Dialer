package com.example.auth

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Data representation of the current authenticated user state.
 */
data class AuthUserState(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?
)

/**
 * Result state for authentication actions.
 */
sealed class AuthActionResult {
    object Idle : AuthActionResult()
    object Loading : AuthActionResult()
    data class Success(val user: AuthUserState) : AuthActionResult()
    data class Error(val message: String) : AuthActionResult()
    object Cancelled : AuthActionResult()
    /**
     * Triggered when the device has zero Google accounts configured or CredentialManager
     * returns NoCredentialException, requiring the official Android add-account flow.
     */
    data class NoAccountOnDevice(val intent: Intent) : AuthActionResult()
}

/**
 * Repository handling Firebase Authentication and official Google Sign-In via Android Credential Manager.
 * Strictly adheres to official Google authentication flows without collecting passwords or OTPs.
 */
class AuthRepository(private val context: Context) {

    companion object {
        private const val TAG = "AuthRepository"

        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val firebaseAuth: FirebaseAuth by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FirebaseAuth: ${e.message}", e)
            FirebaseAuth.getInstance()
        }
    }

    private val credentialManager by lazy {
        CredentialManager.create(context)
    }

    private val _currentUserState = MutableStateFlow<AuthUserState?>(null)
    val currentUserState: StateFlow<AuthUserState?> = _currentUserState.asStateFlow()

    init {
        // Observe Firebase Auth state changes
        try {
            updateUserState(firebaseAuth.currentUser)
            firebaseAuth.addAuthStateListener { auth ->
                updateUserState(auth.currentUser)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error attaching AuthStateListener: ${e.message}")
        }
    }

    private fun updateUserState(user: FirebaseUser?) {
        _currentUserState.value = user?.let {
            AuthUserState(
                uid = it.uid,
                displayName = it.displayName,
                email = it.email,
                photoUrl = it.photoUrl?.toString()
            )
        }
    }

    fun isUserSignedIn(): Boolean = firebaseAuth.currentUser != null

    fun getCurrentUser(): AuthUserState? = _currentUserState.value

    /**
     * Checks if at least one Google account is configured on this Android device.
     */
    fun hasGoogleAccountOnDevice(): Boolean {
        return try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            accounts.isNotEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to inspect accounts on device: ${e.message}")
            false
        }
    }

    /**
     * Creates an official Android Intent to add or create a Google account.
     */
    fun createAddGoogleAccountIntent(): Intent {
        return Intent(Settings.ACTION_ADD_ACCOUNT).apply {
            putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
        }
    }

    /**
     * Executes the official Google Sign-In flow using Android's Credential Manager
     * and authenticates the returned Google ID token with Firebase Authentication.
     *
     * In case 1 (zero Google accounts on device or NoCredentialException returned),
     * automatically returns AuthActionResult.NoAccountOnDevice with the official
     * Google account addition intent so the user can add/create their account.
     */
    suspend fun signInWithGoogle(activity: Activity): AuthActionResult = withContext(Dispatchers.IO) {
        // Pre-check: if zero Google accounts are detected on device, directly direct user to add account
        if (!hasGoogleAccountOnDevice()) {
            Log.i(TAG, "Zero Google accounts detected on device. Directing to official account add flow.")
            return@withContext AuthActionResult.NoAccountOnDevice(createAddGoogleAccountIntent())
        }

        val serverClientId = resolveServerClientId()

        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                return@withContext completeFirebaseSignInWithIdToken(googleIdTokenCredential.idToken)
            } else {
                return@withContext AuthActionResult.Error("Unexpected credential type returned.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User cancelled Google Sign-In")
            return@withContext AuthActionResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.i(TAG, "NoCredentialException caught: device has no matching Google credentials. Launching account add flow.")
            return@withContext AuthActionResult.NoAccountOnDevice(createAddGoogleAccountIntent())
        } catch (e: GetCredentialException) {
            Log.e(TAG, "CredentialManager error [${e.javaClass.simpleName}]: ${e.message}", e)
            // Check if this error indicates missing credentials or no account on device
            val isNoCred = e is NoCredentialException ||
                    e.message?.contains("no credentials available", ignoreCase = true) == true ||
                    e.message?.contains("no account", ignoreCase = true) == true ||
                    e.type.contains("NoCredential", ignoreCase = true)

            if (isNoCred) {
                Log.i(TAG, "Credential error indicates no credential available; redirecting to official add account flow.")
                return@withContext AuthActionResult.NoAccountOnDevice(createAddGoogleAccountIntent())
            }

            val msg = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your internet connection and try again."
                e.message?.contains("canceled", ignoreCase = true) == true ||
                e.message?.contains("cancelled", ignoreCase = true) == true ->
                    "Sign-in was cancelled."
                else -> e.localizedMessage ?: "Google Sign-In failed. Please verify Google Play Services."
            }
            return@withContext AuthActionResult.Error(msg)
        } catch (e: Exception) {
            Log.e(TAG, "General sign-in error: ${e.message}", e)
            val msg = if (e.message?.contains("network", ignoreCase = true) == true) {
                "Network error. Please check your internet connection."
            } else {
                e.localizedMessage ?: "Authentication failed."
            }
            return@withContext AuthActionResult.Error(msg)
        }
    }

    private suspend fun completeFirebaseSignInWithIdToken(idToken: String): AuthActionResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                val userState = AuthUserState(
                    uid = user.uid,
                    displayName = user.displayName,
                    email = user.email,
                    photoUrl = user.photoUrl?.toString()
                )
                _currentUserState.value = userState
                AuthActionResult.Success(userState)
            } else {
                AuthActionResult.Error("Firebase returned no user after authentication.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Auth sign-in failed", e)
            val msg = if (e.message?.contains("network", ignoreCase = true) == true) {
                "Network error connecting to Firebase. Please try again."
            } else {
                e.localizedMessage ?: "Failed to authenticate with Firebase."
            }
            AuthActionResult.Error(msg)
        }
    }

    /**
     * Signs out the user from Firebase and clears Android Credential Manager state.
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.signOut()
            _currentUserState.value = null
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Note: clearing credential state: ${e.message}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun resolveServerClientId(): String {
        // In order of preference:
        // 1. Injected via BuildConfig (Secrets plugin from .env / GOOGLE_WEB_CLIENT_ID)
        val configClientId = try {
            val field = BuildConfig::class.java.getField("GOOGLE_WEB_CLIENT_ID")
            field.get(null) as? String
        } catch (e: Exception) {
            null
        }

        if (!configClientId.isNullOrBlank() && !configClientId.contains("placeholder", ignoreCase = true)) {
            return configClientId
        }

        // 2. Default web client ID associated with project roiki-1a740
        return "176920284831-placeholder.apps.googleusercontent.com"
    }
}
