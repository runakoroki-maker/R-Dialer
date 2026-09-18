package com.example.auth

import android.app.Activity
import android.content.Context
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
     * Executes the official Google Sign-In flow using Android's Credential Manager
     * and authenticates the returned Google ID token with Firebase Authentication.
     *
     * Launches the official Google account chooser bottom sheet provided by Google Play Services.
     * Never redirects to Android system settings or launches generic account-management pages.
     */
    suspend fun signInWithGoogle(activity: Activity): AuthActionResult = withContext(Dispatchers.IO) {
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
                return@withContext AuthActionResult.Error("Unexpected credential type returned from Google.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User cancelled Google Sign-In")
            return@withContext AuthActionResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "NoCredentialException from CredentialManager: ${e.message}")
            return@withContext AuthActionResult.Error(
                "No Google accounts found. Please ensure your Google account is configured on this device."
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, "CredentialManager error [${e.javaClass.simpleName}]: ${e.message}", e)
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

    fun resolveServerClientId(): String {
        // In order of preference:
        // 1. Injected via BuildConfig (Secrets plugin from .env / GOOGLE_WEB_CLIENT_ID)
        val configClientId = try {
            val field = BuildConfig::class.java.getField("GOOGLE_WEB_CLIENT_ID")
            val value = field.get(null) as? String
            if (!value.isNullOrBlank() && !value.contains("placeholder", ignoreCase = true) && !value.contains("YOUR_WEB_CLIENT_ID", ignoreCase = true)) {
                value
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

        if (configClientId != null) {
            return configClientId
        }

        // 2. Resource string: default_web_client_id (from google-services or strings.xml)
        val defaultResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (defaultResId != 0) {
            val resVal = context.getString(defaultResId)
            if (resVal.isNotBlank() && !resVal.contains("placeholder", ignoreCase = true)) {
                return resVal
            }
        }

        // 3. Resource string: google_web_client_id
        val customResId = context.resources.getIdentifier("google_web_client_id", "string", context.packageName)
        if (customResId != 0) {
            val resVal = context.getString(customResId)
            if (resVal.isNotBlank() && !resVal.contains("placeholder", ignoreCase = true)) {
                return resVal
            }
        }

        // 4. Fallback associated with project roiki-1a740
        return "176920284831-placeholder.apps.googleusercontent.com"
    }
}
