package com.nexia.ai.ai

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Calls YOUR HTTPS proxy; never embed an OpenAI/Gemini API key in the APK. */
class CloudAiEngine(private val backendUrl: String) {
    fun answer(prompt: String): String {
        require(backendUrl.startsWith("https://")) { "Configura un backend HTTPS antes de usar IA en línea" }
        val connection = URL(backendUrl).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 12000
            connection.readTimeout = 45000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val payload = JSONObject().put("message", prompt).toString()
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) throw IllegalStateException("Error del servicio de IA")
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(response).getString("reply").take(20000)
        } finally { connection.disconnect() }
    }
}
