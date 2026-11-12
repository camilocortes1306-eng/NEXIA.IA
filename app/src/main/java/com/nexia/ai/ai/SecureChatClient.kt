package com.nexia.ai.ai

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Send only NEXIA's short-lived session token; NEVER send provider API keys from the app. */
class SecureChatClient(private val baseUrl: String) {
    fun chat(message: String, provider: String, sessionToken: String): String {
        require(baseUrl.startsWith("https://")) { "HTTPS obligatorio" }
        require(provider == "openai" || provider == "gemini")
        val conn = URL(baseUrl.trimEnd('/') + "/chat").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 12000
            conn.readTimeout = 70000
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $sessionToken")
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val body = JSONObject().put("message", message).put("provider", provider).toString()
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) error("Error HTTP ${conn.responseCode}")
            return JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).getString("reply")
        } finally { conn.disconnect() }
    }
}
