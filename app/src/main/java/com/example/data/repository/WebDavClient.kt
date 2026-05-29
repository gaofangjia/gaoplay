package com.example.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class WebDavItem(
    val name: String,
    val path: String, // Full path inside server
    val isDirectory: Boolean,
    val size: Long,
    val downloadUrl: String
) {
    val sizeFormatted: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }
}

class WebDavClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun listFiles(
        serverUrl: String,
        username: String,
        password: String,
        dirPath: String = "/"
    ): List<WebDavItem> = withContext(Dispatchers.IO) {
        // Sanitize base URL
        var baseUrl = serverUrl.trim()
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "http://$baseUrl"
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substringBeforeLast("/")
        }

        // Construct correct full path
        val sanitizedDir = if (dirPath.startsWith("/")) dirPath else "/$dirPath"
        val fullUrl = "$baseUrl$sanitizedDir"
        val credential = Credentials.basic(username, password)

        val propfindBody = """
            <?xml version="1.0" encoding="utf-8" ?>
            <propfind xmlns="DAV:">
               <prop>
                  <displayname/>
                  <getcontentlength/>
                  <resourcetype/>
               </prop>
            </propfind>
        """.trimIndent()

        val requestBody = propfindBody.toRequestBody("text/xml; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(fullUrl)
            .method("PROPFIND", requestBody)
            .addHeader("Authorization", credential)
            .addHeader("Depth", "1")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP error code: ${response.code}")
                }
                val bodyString = response.body?.string() ?: ""
                return@withContext parseWebDavXml(bodyString, baseUrl, username, password, sanitizedDir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun parseWebDavXml(
        xml: String, 
        baseUrl: String, 
        username: String, 
        password: String, 
        currentDir: String
    ): List<WebDavItem> {
        val items = mutableListOf<WebDavItem>()
        // Regex parsing to robustly extract <response> nodes
        val responseRegex = "<(?:[a-zA-Z0-9]+:)?response>([\\s\\S]*?)</(?:[a-zA-Z0-9]+:)?response>".toRegex(RegexOption.IGNORE_CASE)
        val matches = responseRegex.findAll(xml)

        val cleanCurrentDir = currentDir.trimEnd('/')

        for (match in matches) {
            val responseContent = match.groupValues[1]

            // Extract href
            val href = "<(?:[a-zA-Z0-9]+:)?href>([\\s\\S]*?)</(?:[a-zA-Z0-9]+:)?href>".toRegex(RegexOption.IGNORE_CASE)
                .find(responseContent)?.groupValues?.get(1)?.trim() ?: ""

            if (href.isEmpty()) continue

            // Decode percent encoded characters
            val decodedPath = try {
                java.net.URLDecoder.decode(href, "UTF-8")
            } catch (e: Exception) {
                href
            }

            val cleanDecodedPath = decodedPath.trimEnd('/')

            // Skip current directory itself
            if (cleanDecodedPath.equals(cleanCurrentDir, ignoreCase = true) || 
                cleanDecodedPath.endsWith(cleanCurrentDir, ignoreCase = true) && 
                cleanDecodedPath.length <= cleanCurrentDir.length) {
                continue
            }

            // Extract display name or infer from href
            var displayName = "<(?:[a-zA-Z0-9]+:)?displayname>([\\s\\S]*?)</(?:[a-zA-Z0-9]+:)?displayname>".toRegex(RegexOption.IGNORE_CASE)
                .find(responseContent)?.groupValues?.get(1)?.trim() ?: ""
            
            if (displayName.isEmpty()) {
                displayName = decodedPath.trimEnd('/').substringAfterLast('/')
            }

            if (displayName.isEmpty() || displayName == "/" || displayName == ".") {
                continue
            }

            // Is collection/directory?
            val isDirectory = responseContent.contains("collection", ignoreCase = true) || href.endsWith("/")

            // Extract content size length
            val contentLengthStr = "<(?:[a-zA-Z0-9]+:)?getcontentlength>([\\s\\S]*?)</(?:[a-zA-Z0-9]+:)?getcontentlength>".toRegex(RegexOption.IGNORE_CASE)
                .find(responseContent)?.groupValues?.get(1)?.trim() ?: "0"
            val size = contentLengthStr.toLongOrNull() ?: 0L

            // Construct basic authentication stream/download URL so ExoPlayer can stream natively
            // Form: http://user:pass@host/path
            val streamUrl = try {
                val encodedUser = java.net.URLEncoder.encode(username, "UTF-8").replace("+", "%20")
                val encodedPass = java.net.URLEncoder.encode(password, "UTF-8").replace("+", "%20")
                if (baseUrl.startsWith("https://")) {
                    val host = baseUrl.substringAfter("https://")
                    "https://$encodedUser:$encodedPass@$host$decodedPath"
                } else {
                    val host = baseUrl.substringAfter("http://")
                    "http://$encodedUser:$encodedPass@$host$decodedPath"
                }
            } catch (e: Exception) {
                if (baseUrl.startsWith("https://")) {
                    val host = baseUrl.substringAfter("https://")
                    "https://$username:$password@$host$decodedPath"
                } else {
                    val host = baseUrl.substringAfter("http://")
                    "http://$username:$password@$host$decodedPath"
                }
            }

            items.add(
                WebDavItem(
                    name = displayName,
                    path = decodedPath,
                    isDirectory = isDirectory,
                    size = size,
                    downloadUrl = streamUrl
                )
            )
        }
        
        // Sort: directories first, then other items by name alphabetically
        return items.sortedWith(compareByDescending<WebDavItem> { it.isDirectory }.thenBy { it.name })
    }
}
