package com.nexia.ai.google

import java.net.HttpURLConnection
import java.net.URL

/** Demo read-only endpoint. Call on a worker thread, never on the UI thread. */
object GoogleApi {
    fun listDriveMetadata(accessToken: String): String {
        val url = URL("https://www.googleapis.com/drive/v3/files?pageSize=10&fields=nextPageToken,files(id,name,mimeType)")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) "Error HTTP ${connection.responseCode}" else body
        } finally { connection.disconnect() }
    }
}
