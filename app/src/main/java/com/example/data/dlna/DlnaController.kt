package com.example.data.dlna

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URL
import java.net.URLDecoder
import javax.xml.parsers.DocumentBuilderFactory

data class DlnaDevice(
    val id: String,
    val name: String,
    val location: String,
    val controlUri: String = "",
    val ipAddress: String = ""
)

class DlnaController(private val context: Context) {
    private val TAG = "DlnaController"
    
    private val _devices = MutableStateFlow<List<DlnaDevice>>(emptyList())
    val devices: StateFlow<List<DlnaDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var multicastLock: WifiManager.MulticastLock? = null

    /**
     * Start local UDP SSDP scan for MediaRenderer devices (DLNA-compliant playback displays)
     */
    suspend fun startScan() = withContext(Dispatchers.IO) {
        if (_isScanning.value) return@withContext
        _isScanning.value = true
        _devices.value = emptyList()

        // Acquire Multicast lock to allow UDP multicast packet collection
        acquireMulticastLock()

        val socket: DatagramSocket?
        try {
            socket = DatagramSocket()
            socket.soTimeout = 3000 // 3 seconds timeout
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create DatagramSocket", e)
            _isScanning.value = false
            releaseMulticastLock()
            return@withContext
        }

        val ssdpQuery = "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: 239.255.255.250:1900\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "MX: 3\r\n" +
                "ST: urn:schemas-upnp-org:service:AVTransport:1\r\n" +
                "\r\n"

        val queryData = ssdpQuery.toByteArray()
        val mcastAddr = InetAddress.getByName("239.255.255.250")
        val packet = DatagramPacket(queryData, queryData.size, mcastAddr, 1900)

        try {
            // Send query multiple times to prevent UDP packet drops
            socket.send(packet)
            socket.send(packet)

            val buffer = ByteArray(2048)
            val startTime = System.currentTimeMillis()

            // Collect responses for 3 seconds
            while (System.currentTimeMillis() - startTime < 3500) {
                val recvPacket = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(recvPacket)
                    val response = String(recvPacket.data, 0, recvPacket.length)
                    val location = parseHeader(response, "LOCATION")
                    if (location != null && location.isNotEmpty()) {
                        parseXmlAndAddDevice(location, recvPacket.address.hostAddress ?: "")
                    }
                } catch (e: java.io.InterruptedIOException) {
                    // socket timeout reached, exit loop
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error receiving multicast response", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SSDP scan loop interrupted", e)
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {}
            _isScanning.value = false
            releaseMulticastLock()
        }
    }

    private fun parseHeader(response: String, headerName: String): String? {
        val lines = response.split("\r\n")
        for (line in lines) {
            if (line.uppercase().startsWith(headerName.uppercase() + ":")) {
                return line.substring(headerName.length + 1).trim()
            }
        }
        return null
    }

    private fun parseXmlAndAddDevice(xmlUrl: String, fallbackIp: String) {
        try {
            val url = URL(xmlUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            val stream = connection.getInputStream()

            val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = docBuilder.parse(stream)
            doc.documentElement.normalize()

            val friendlyNameNode = doc.getElementsByTagName("friendlyName").item(0)
            val friendlyName = friendlyNameNode?.textContent ?: "Smart TV Screen"

            // Look for AVTransport service controls to push urls
            val serviceList = doc.getElementsByTagName("service")
            var controlUri = ""
            for (i in 0 until serviceList.length) {
                val serviceNode = serviceList.item(i)
                val typeNode = serviceNode.childNodes
                var isAvTransport = false
                var currentControlUri = ""
                for (j in 0 until typeNode.length) {
                    val child = typeNode.item(j)
                    if (child.nodeName == "serviceType" && child.textContent.contains("AVTransport")) {
                        isAvTransport = true
                    }
                    if (child.nodeName == "controlURL") {
                        currentControlUri = child.textContent
                    }
                }
                if (isAvTransport && currentControlUri.isNotEmpty()) {
                    controlUri = currentControlUri
                    break
                }
            }

            // Normalise the control url
            val baseUri = "${url.protocol}://${url.host}:${url.port}"
            val finalControl = if (controlUri.startsWith("/")) {
                baseUri + controlUri
            } else {
                "$baseUri/$controlUri"
            }

            val deviceId = url.host + "_" + url.port
            val newDevice = DlnaDevice(
                id = deviceId,
                name = friendlyName,
                location = xmlUrl,
                controlUri = finalControl,
                ipAddress = url.host ?: fallbackIp
            )

            val currentList = _devices.value.toMutableList()
            if (!currentList.any { it.id == newDevice.id }) {
                currentList.add(newDevice)
                _devices.value = currentList
                Log.d(TAG, "Discovered DLNA TV: ${newDevice.name} at ${newDevice.ipAddress}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing DLNA device XML: $xmlUrl", e)
        }
    }

    /**
     * Cast stream URL payload using standard UPnP AVTransport SOAP API commands
     */
    suspend fun castVideo(device: DlnaDevice, videoUrl: String, title: String): Boolean = withContext(Dispatchers.IO) {
        val controlUrl = device.controlUri.ifEmpty { return@withContext false }
        
        val setUriSoap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "  <s:Body>\n" +
                "    <u:SetAVTransportURI xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\">\n" +
                "      <InstanceID>0</InstanceID>\n" +
                "      <CurrentURI>${escapeXml(videoUrl)}</CurrentURI>\n" +
                "      <CurrentURIMetaData>" +
                "&lt;DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\"&gt;" +
                "&lt;item id=\"0\" parentID=\"0\" restricted=\"1\"&gt;" +
                "&lt;dc:title&gt;${escapeXml(title)}&lt;/dc:title&gt;" +
                "&lt;upnp:class&gt;object.item.videoItem&lt;/upnp:class&gt;" +
                "&lt;res protocolInfo=\"http-get:*:video/*:*\"&gt;${escapeXml(videoUrl)}&lt;/res&gt;" +
                "&lt;/item&gt;" +
                "&lt;/DIDL-Lite&gt;" +
                "</CurrentURIMetaData>\n" +
                "    </u:SetAVTransportURI>\n" +
                "  </s:Body>\n" +
                "</s:Envelope>"

        val playSoap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "  <s:Body>\n" +
                "    <u:Play xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\">\n" +
                "      <InstanceID>0</InstanceID>\n" +
                "      <Speed>1</Speed>\n" +
                "    </u:Play>\n" +
                "  </s:Body>\n" +
                "</s:Envelope>"

        try {
            // Step 1: Set URI
            val setResponse = postSoapAction(controlUrl, "urn:schemas-upnp-org:service:AVTransport:1#SetAVTransportURI", setUriSoap)
            if (!setResponse) return@withContext false

            // Step 2: Trigger Play command
            return@withContext postSoapAction(controlUrl, "urn:schemas-upnp-org:service:AVTransport:1#Play", playSoap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed casting to DLNA device", e)
            return@withContext false
        }
    }

    private fun postSoapAction(urlStr: String, action: String, xmlPayload: String): Boolean {
        var connection: java.net.HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
            connection.setRequestProperty("SOAPACTION", "\"$action\"")

            val os = connection.outputStream
            os.write(xmlPayload.toByteArray())
            os.flush()
            os.close()

            val responseCode = connection.responseCode
            Log.d(TAG, "SOAP Action ($action) returned response code path: $responseCode")
            return responseCode == 200
        } catch (e: Exception) {
            Log.e(TAG, "Soap action error $action: $e")
            return false
        } finally {
            connection?.disconnect()
        }
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun acquireMulticastLock() {
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            multicastLock = wifi.createMulticastLock("DlnaSsdpLock").apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring MulticastLock", e)
        }
    }

    private fun releaseMulticastLock() {
        try {
            multicastLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MulticastLock", e)
        }
    }
}
