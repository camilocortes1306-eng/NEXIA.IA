package com.nexia.ai.google

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity

/** Uses Google's Android authorization UI; never handles a Google password. */
class GoogleAuthorization(private val activity: Activity) {
    private val client = Identity.getAuthorizationClient(activity)

    fun request(feature: GoogleFeature, onResolution: (android.app.PendingIntent) -> Unit,
                onToken: (String) -> Unit, onError: (String) -> Unit) {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(feature.asScope()))
            .build()
        client.authorize(request)
            .addOnSuccessListener { result ->
                when {
                    result.hasResolution() -> result.pendingIntent?.let(onResolution)
                        ?: onError("Google no devolvió una solicitud de autorización")
                    !result.accessToken.isNullOrBlank() -> onToken(result.accessToken!!)
                    else -> onError("No se recibió autorización")
                }
            }
            .addOnFailureListener { onError(it.localizedMessage ?: "Error de Google") }
    }

    fun resultFromIntent(data: Intent?): AuthorizationResult =
        client.getAuthorizationResultFromIntent(data)

    fun revoke(onDone: () -> Unit, onError: (String) -> Unit) {
        onError("La revocación de acceso de Google aún no está implementada")
    }
}
