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
        val requestBuilder = Request.Builder()
            .url(fullUrl)
            .get()
        if (username.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", credential)
        }
        val request = requestBuilder.build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 405) {
                        // Method not allowed for GET, fallback to PROPFIND
                        val propfindBody = """
                            <?xml version="1.0" encoding="utf-8" ?>
                            <propfind xmlns="DAV:"><prop><displayname/><getcontentlength/><resourcetype/></prop></propfind>
                        """.trimIndent()
                        val reqBody = propfindBody.toRequestBody("text/xml; charset=utf-8".toMediaType())
                        val pfReq = Request.Builder().url(fullUrl).method("PROPFIND", reqBody).addHeader("Authorization", credential).addHeader("Depth", "1").build()
                        client.newCall(pfReq).execute().use { pfResp ->
                            if (pfResp.isSuccessful) {
                                val pfBody = pfResp.body?.string() ?: ""
                                return parseWebDavXml(pfBody, baseUrl, username, password, sanitizedDir)
                            }
                        }
                    }
                    throw IOException("HTTP Listing failed: status ${response.code}")
                }
                val bodyString = response.body?.string() ?: ""
                
                // If response is WebDAV XML despite HTTP GET request
                if (bodyString.contains("<multistatus", ignoreCase = true) || bodyString.contains("<response>", ignoreCase = true) || bodyString.contains(":response>", ignoreCase = true)) {
                    return parseWebDavXml(bodyString, baseUrl, username, password, sanitizedDir)
                }

                return parseHttpAutoindex(bodyString, baseUrl, username, password, sanitizedDir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
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
        val linkRegex = "<a\\s+[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>".toRegex(RegexOption.IGNORE_CASE)
        val matches = linkRegex.findAll(html)

        val cleanCurrentDir = currentDir.trimEnd('/')

        for (match in matches) {
            val href = match.groupValues[1].trim()
            val text = match.groupValues[2].replace(Regex("<[^>]*>"), "").replace("&amp;", "&").trim()

            // Skip query parameters, anchors, javascript, parent directories
            if (href.startsWith("?") || href.startsWith("#") || href.startsWith("javascript:")) {
                continue
            }
            if (href == "../" || href == ".." || href == "." || href == "./" || 
                text == "Parent Directory" || text == ".." || text == ".") {
                continue
            }

            var cleanHref = href
            if (cleanHref.startsWith("./")) {
                cleanHref = cleanHref.substring(2)
            }

            // Determine if href is an absolute HTTP/HTTPS URL
            val isFullUrl = cleanHref.startsWith("http://", ignoreCase = true) || cleanHref.startsWith("https://", ignoreCase = true)
            val isAbsolutePath = cleanHref.startsWith("/")

            val sanitizedPath = when {
                isFullUrl -> {
                    val pathPart = cleanHref.substringAfter("://").substringAfter("/", "")
                    if (pathPart.isEmpty()) "/" else "/$pathPart"
                }
                isAbsolutePath -> cleanHref
                else -> if (cleanCurrentDir.isEmpty()) "/$cleanHref" else "$cleanCurrentDir/$cleanHref"
            }.replace(Regex("/{2,}"), "/")

            // Skip current directory itself
            if (sanitizedPath == currentDir || sanitizedPath == "$cleanCurrentDir/") {
                continue
            }

            val filename = sanitizedPath.substringAfterLast('/')
            val isDirectory = href.endsWith("/") || text.endsWith("/") || (!filename.contains(".") && !isFullUrl)

            var cleanName = text.trimEnd('/')
            if (cleanName.isEmpty() || cleanName == href) {
                val decodedPathName = try {
                    java.net.URLDecoder.decode(sanitizedPath, "UTF-8")
                } catch (e: Exception) {
                    sanitizedPath
                }
                cleanName = decodedPathName.trimEnd('/').substringAfterLast('/')
            }

            if (cleanName.isEmpty() || cleanName == "." || cleanName == "..") {
                continue
            }

            val streamUrl = if (username.isNotEmpty()) {
                val encodedUser = try { java.net.URLEncoder.encode(username, "UTF-8").replace("+", "%20") } catch (e: Exception) { username }
                val encodedPass = try { java.net.URLEncoder.encode(password, "UTF-8").replace("+", "%20") } catch (e: Exception) { password }
                if (baseUrl.startsWith("https://", ignoreCase = true)) {
                    val host = baseUrl.substringAfter("https://")
                    "https://$encodedUser:$encodedPass@$host$sanitizedPath"
                } else {
                    val host = baseUrl.substringAfter("http://")
                    "http://$encodedUser:$encodedPass@$host$sanitizedPath"
                }
            } else {
                "$baseUrl$sanitizedPath"
            }.replace(Regex("(?<!:)/{2,}"), "/")

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
        return items.distinctBy { it.path }.sortedWith(compareByDescending<WebDavItem> { it.isDirectory }.thenBy { it.name })
    }

    private fun parseWebDavXml(
        xml: String, 
        baseUrl: String, 
        username: String, 
        password: String, 
        currentDir: String
    ): List<WebDavItem> {
        val items = mutableListOf<WebDavItem>()
        val responseRegex = "<(?:[a-zA-Z0-9]+:)?response>([\\s\\S]*?)</(?:[a-zA-Z0-9]+:)?response>".toRegex(RegexOption.IGNORE_CASE)
        val matches = responseRegex.findAll(xml)

        val cleanCurrentDir = currentDir.trimEnd('/')

        for (match in matches) {
            val responseContent = match.groupValues[1]

            val href = "<(?:[a-zA-Z0-9]+:)?href>([\\s\\S]*?)</(?:[a-zA-Z0-9]+:)?href>".toRegex(RegexOption.IGNORE_CASE)
                .find(responseContent)?.groupValues?.get(1)?.trim() ?: ""

            if (href.isEmpty()) continue

            val decodedPath = try {
                java.net.URLDecoder.decode(href, "UTF-8")
            } catch (e: Exception) {
                href
            }

            val cleanDecodedPath = decodedPath.trimEnd('/')

            if (cleanDecodedPath.equals(cleanCurrentDir, ignoreCase = true) || 
                cleanDecodedPath.endsWith(cleanCurrentDir, ignoreCase = true) && 
                cleanDecodedPath.length <= cleanCurrentDir.length) {
                continue
            }

            var displayName = "<(?:[a-zA-Z0-9]+:)?displayname>([\\s\\S]*?)</(?:[a-zA-Z0-9]+:)?displayname>".toRegex(RegexOption.IGNORE_CASE)
                .find(responseContent)?.groupValues?.get(1)?.trim() ?: ""
            
            if (displayName.isEmpty()) {
                displayName = decodedPath.trimEnd('/').substringAfterLast('/')
            }

            if (displayName.isEmpty() || displayName == "/" || displayName == ".") {
                continue
            }

            val isDirectory = responseContent.contains("collection", ignoreCase = true) || href.endsWith("/")

            val contentLengthStr = "<(?:[a-zA-Z0-9]+:)?getcontentlength>([\\s\\S]*?)</(?:[a-zA-Z0-9]+:)?getcontentlength>".toRegex(RegexOption.IGNORE_CASE)
                .find(responseContent)?.groupValues?.get(1)?.trim() ?: "0"
            val size = contentLengthStr.toLongOrNull() ?: 0L

            val streamUrl = if (username.isNotEmpty()) {
                val encodedUser = try { java.net.URLEncoder.encode(username, "UTF-8").replace("+", "%20") } catch (e: Exception) { username }
                val encodedPass = try { java.net.URLEncoder.encode(password, "UTF-8").replace("+", "%20") } catch (e: Exception) { password }
                if (baseUrl.startsWith("https://")) {
                    val host = baseUrl.substringAfter("https://")
                    "https://$encodedUser:$encodedPass@$host$decodedPath"
                } else {
                    val host = baseUrl.substringAfter("http://")
                    "http://$encodedUser:$encodedPass@$host$decodedPath"
                }
            } else {
                "$baseUrl$decodedPath"
            }.replace(Regex("(?<!:)/{2,}"), "/")

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
        
        return items.distinctBy { it.path }.sortedWith(compareByDescending<WebDavItem> { it.isDirectory }.thenBy { it.name })
    }
}
