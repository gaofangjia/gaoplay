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
        dirPath: String = "/",
        protocol: String = "webdav"
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

        if (protocol == "http") {
            return@withContext fetchHttpAutoindex(fullUrl, baseUrl, username, password, sanitizedDir, credential)
        }

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
                if (response.code == 405 || response.code == 400 || response.code == 501) {
                    // Method Not Allowed / Bad Request, fallback to standard GET parsing for HTTP Autoindex
                    return@withContext fetchHttpAutoindex(fullUrl, baseUrl, username, password, sanitizedDir, credential)
                }
                if (!response.isSuccessful) {
                    throw IOException("HTTP error code: ${response.code}")
                }
                val bodyString = response.body?.string() ?: ""

                // If response is index.html instead of WebDAV XML, fallback to HTTP autoindex parser
                if (bodyString.trim().startsWith("<html", ignoreCase = true) || bodyString.contains("<doctype html", ignoreCase = true) || bodyString.contains("<title>", ignoreCase = true)) {
                    return@withContext parseHttpAutoindex(bodyString, baseUrl, username, password, sanitizedDir)
                }

                return@withContext parseWebDavXml(bodyString, baseUrl, username, password, sanitizedDir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // On connection/parsing failures at root "/", search common subpaths as a smart fallback
            if (dirPath == "/") {
                val discoveryPaths = listOf("/dav", "/webdav", "/sharing")
                for (sub in discoveryPaths) {
                    try {
                        val subUrl = "$baseUrl$sub"
                        val subRequestBody = propfindBody.toRequestBody("text/xml; charset=utf-8".toMediaType())
                        val subRequest = Request.Builder()
                            .url(subUrl)
                            .method("PROPFIND", subRequestBody)
                            .addHeader("Authorization", credential)
                            .addHeader("Depth", "1")
                            .build()
                        client.newCall(subRequest).execute().use { subResponse ->
                            if (subResponse.isSuccessful) {
                                val bodyString = subResponse.body?.string() ?: ""
                                return@withContext parseWebDavXml(bodyString, baseUrl, username, password, sub)
                            }
                        }
                    } catch (ignore: Exception) {}
                }
            }
            throw e
        }
    }

    private fun fetchHttpAutoindex(
        fullUrl: String,
        baseUrl: String,
        username: String,
        password: String,
        sanitizedDir: String,
        credential: String
    ): List<WebDavItem> {
        val request = Request.Builder()
            .url(fullUrl)
            .get()
            .addHeader("Authorization", credential)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP Listing failed: status ${response.code}")
            }
            val htmlString = response.body?.string() ?: ""
            return parseHttpAutoindex(htmlString, baseUrl, username, password, sanitizedDir)
        }
    }

    private fun parseHttpAutoindex(
        html: String,
        baseUrl: String,
        username: String,
        password: String,
        currentDir: String
    ): List<WebDavItem> {
        val items = mutableListOf<WebDavItem>()
        // Regular expression matching <a href="...">text</a> links
        val linkRegex = "<a\\s+[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>".toRegex(RegexOption.IGNORE_CASE)
        val matches = linkRegex.findAll(html)

        val cleanCurrentDir = currentDir.trimEnd('/')

        for (match in matches) {
            val href = match.groupValues[1].trim()
            val text = match.groupValues[2].replace(Regex("<[^>]*>"), "").trim()

            // Filter out system control anchor parameters
            if (href.startsWith("?") || href.startsWith("http://") || href.startsWith("https://") || href.startsWith("/")) {
                if (!href.startsWith(cleanCurrentDir)) continue
            }
            if (href == "../" || href == ".." || text == "Parent Directory" || text == "..") {
                continue
            }

            val decodedPathName = try {
                java.net.URLDecoder.decode(href, "UTF-8")
            } catch (e: Exception) {
                href
            }

            val isDirectory = href.endsWith("/")
            val cleanName = decodedPathName.trimEnd('/')

            if (cleanName.isEmpty() || cleanName == "." || cleanName == "..") {
                continue
            }

            val fullPath = if (currentDir == "/") "/$href" else "$cleanCurrentDir/$href"
            val sanitizedPath = if (fullPath.startsWith("/")) fullPath else "/$fullPath"

            val streamUrl = try {
                val encodedUser = java.net.URLEncoder.encode(username, "UTF-8").replace("+", "%20")
                val encodedPass = java.net.URLEncoder.encode(password, "UTF-8").replace("+", "%20")
                if (baseUrl.startsWith("https://")) {
                    val host = baseUrl.substringAfter("https://")
                    "https://$encodedUser:$encodedPass@$host$sanitizedPath"
                } else {
                    val host = baseUrl.substringAfter("http://")
                    "http://$encodedUser:$encodedPass@$host$sanitizedPath"
                }
            } catch (e: Exception) {
                if (baseUrl.startsWith("https://")) {
                    val host = baseUrl.substringAfter("https://")
                    "https://$username:$password@$host$sanitizedPath"
                } else {
                    val host = baseUrl.substringAfter("http://")
                    "http://$username:$password@$host$sanitizedPath"
                }
            }

            items.add(
                WebDavItem(
                    name = cleanName,
                    path = sanitizedPath,
                    isDirectory = isDirectory,
                    size = 0L,
                    downloadUrl = streamUrl
                )
            )
        }
        return items.sortedWith(compareByDescending<WebDavItem> { it.isDirectory }.thenBy { it.name })
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
