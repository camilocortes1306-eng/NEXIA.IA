package com.nexia.ai.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Call from a coroutine (not the UI thread). Web OAuth client ID is public; API keys are not. */
class GoogleBackendLogin(private val context: Context, private val webClientId: String, private val baseUrl: String) {
    suspend fun signIn(): String {
        require(baseUrl.startsWith("https://"))
        val option = GetGoogleIdOption.Builder().setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = CredentialManager.create(context).getCredential(context, request)
        val credential = result.credential as? CustomCredential ?: error("Credencial no compatible")
        require(credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
        val googleToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        return exchangeToken(googleToken)
    }
    private fun exchangeToken(idToken: String): String {
        val conn = URL(baseUrl.trimEnd('/') + "/auth/google").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 12000
            conn.readTimeout = 20000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(JSONObject().put("id_token", idToken).toString().toByteArray()) }
            if (conn.responseCode !in 200..299) error("No se pudo autenticar")
            return JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).getString("access_token")
        } finally { conn.disconnect() }
    }
}
