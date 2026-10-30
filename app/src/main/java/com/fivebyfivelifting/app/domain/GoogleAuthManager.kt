package com.fivebyfivelifting.app.domain

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

data class GoogleUserProfile(
    val email: String,
    val displayName: String?,
    val userId: String?,
    val idToken: String?
)

/**
 * Handles native Google Sign-In on Android via Google Play Services Auth (`com.google.android.gms.auth.api.signin`),
 * configured with the project's Google Cloud Platform OAuth 2.0 Client ID.
 */
class GoogleAuthManager(private val context: Context) {

    companion object {
        const val OAUTH_CLIENT_ID =
            "862259162609-m0kjfftsp4crj8o2kp3nrduhjjl5ripd.apps.googleusercontent.com"
    }

    private fun buildClient(includeIdToken: Boolean = true): GoogleSignInClient {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
        if (includeIdToken && OAUTH_CLIENT_ID.isNotBlank()) {
            builder.requestIdToken(OAUTH_CLIENT_ID)
        }
        return GoogleSignIn.getClient(context, builder.build())
    }

    fun buildSignInIntent(includeIdToken: Boolean = true): Intent =
        buildClient(includeIdToken).signInIntent

    /**
     * Clears any cached session first so Google Play Services always displays the interactive
     * "Sign in with Google" account picker sheet.
     */
    fun signOutAndBuildSignInIntent(
        includeIdToken: Boolean = true,
        onIntentReady: (Intent) -> Unit
    ) {
        val client = buildClient(includeIdToken)
        client.signOut().addOnCompleteListener {
            onIntentReady(client.signInIntent)
        }
    }

    fun signOut(onComplete: () -> Unit) {
        buildClient(includeIdToken = false).signOut().addOnCompleteListener {
            onComplete()
        }
    }

    fun getLastSignedInProfile(): GoogleUserProfile? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        return account.toUserProfile()
    }

    fun parseSignInResult(data: Intent?): Result<GoogleUserProfile> {
        return runCatching {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            account.toUserProfile()
                ?: error("Signed in Google account did not return an email address.")
        }
    }

    fun isDeveloperError(throwable: Throwable?): Boolean {
        return throwable is ApiException && throwable.statusCode == 10
    }

    private fun GoogleSignInAccount.toUserProfile(): GoogleUserProfile? {
        val emailAddr = email?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return GoogleUserProfile(
            email = emailAddr,
            displayName = displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: givenName?.trim()?.takeIf { it.isNotBlank() },
            userId = id,
            idToken = idToken
        )
    }
}
