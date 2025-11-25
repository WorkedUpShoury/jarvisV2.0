package com.example.jarvisv2.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

@Serializable
data class JarvisCommandRequest(val command: String, val token: String)

@Serializable
data class JarvisCommandResponse(val ok: Boolean, val request_id: String)

// OLD Event classes (Can keep for legacy/other events if needed)
@Serializable
data class JarvisEvent(val id: Int, val ts: Double, val type: String, val text: String)
@Serializable
data class JarvisEventResponse(val events: List<JarvisEvent>, val last_id: Int)

// NEW History Classes
@Serializable
data class HistoryItem(
    val type: String,
    val role: String,
    val parts: HistoryParts
)

@Serializable
data class HistoryParts(
    val text: String
)

@Serializable
data class SystemLevelsResponse(val volume: Int, val brightness: Int)

@Serializable
data class MediaStateResponse(
    val title: String,
    val artist: String,
    val is_playing: Boolean,
    val thumbnail: String?
)

class JarvisApiClient(private val context: Context) {

    private val client = HttpClient(Android) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true // Important for complex JSON
            })
        }
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger { override fun log(message: String) { Log.d("KtorClient", message) } }
        }
    }

    private val apiToken = "jarvisrunning"
    private val udpPort = 8766

    suspend fun sendUdpCommand(host: String, message: String) {
        withContext(Dispatchers.IO) {
            try {
                val socket = DatagramSocket()
                val address = InetAddress.getByName(host)
                val buffer = message.toByteArray()
                val packet = DatagramPacket(buffer, buffer.size, address, udpPort)
                socket.send(packet)
                socket.close()
            } catch (e: Exception) { Log.e("JarvisUDP", "Send failed: ${e.message}") }
        }
    }

    suspend fun getSystemLevels(serverUrl: String): Result<SystemLevelsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response: SystemLevelsResponse = client.get("$serverUrl/api/levels") {
                    url { parameters.append("token", apiToken) }
                }.body()
                Result.success(response)
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun getMediaState(serverUrl: String): Result<MediaStateResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response: MediaStateResponse = client.get("$serverUrl/api/media") {
                    url { parameters.append("token", apiToken) }
                }.body()
                Result.success(response)
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    // --- NEW: Get Full Chat History ---
    suspend fun getChatHistory(serverUrl: String): Result<List<HistoryItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val response: List<HistoryItem> = client.get("$serverUrl/api/history") {
                    url { parameters.append("token", apiToken) }
                }.body()
                Result.success(response)
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    fun discoverJarvisService(): Flow<String> = callbackFlow {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifiManager.createMulticastLock("JarvisAppLock")
        var jmdns: JmDNS? = null
        var serviceListener: ServiceListener? = null

        try {
            lock.setReferenceCounted(true)
            lock.acquire()
            val hostAddress = getLocalIpAddress(wifiManager)
            if (hostAddress == null) { close(IllegalStateException("Wifi is off.")); return@callbackFlow }

            Log.d("JarvisApiClient", "Starting jmDNS on $hostAddress")
            val inetAddress = InetAddress.getByName(hostAddress)
            jmdns = JmDNS.create(inetAddress, "JarvisClient")

            serviceListener = object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) { jmdns?.requestServiceInfo(event.type, event.name, 1) }
                override fun serviceRemoved(event: ServiceEvent) {}
                override fun serviceResolved(event: ServiceEvent) {
                    val info = event.info
                    val port = info.port
                    val host = info.inetAddresses.firstOrNull()?.hostAddress
                    if (host != null && port != 0) trySend("http://$host:$port")
                }
            }
            jmdns.addServiceListener("_jarvis._tcp.local.", serviceListener)
        } catch (e: Exception) { close(e) }

        awaitClose {
            if (jmdns != null && serviceListener != null) jmdns.removeServiceListener("_jarvis._tcp.local.", serviceListener)
            jmdns?.close()
            if (lock.isHeld) lock.release()
        }
    }.flowOn(Dispatchers.IO)

    @Suppress("DEPRECATION")
    private fun getLocalIpAddress(wifiManager: WifiManager): String? {
        val dhcpInfo = wifiManager.dhcpInfo ?: return null
        val ipInt = dhcpInfo.ipAddress
        if (ipInt == 0) return null
        return String.format(java.util.Locale.US, "%d.%d.%d.%d", ipInt and 0xFF, ipInt shr 8 and 0xFF, ipInt shr 16 and 0xFF, ipInt shr 24 and 0xFF)
    }

    suspend fun sendCommand(serverUrl: String, command: String): Result<JarvisCommandResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = JarvisCommandRequest(command = command, token = apiToken)
                val response: JarvisCommandResponse = client.post("$serverUrl/command") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }.body()
                Result.success(response)
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun getEvents(serverUrl: String, since: Int): Result<JarvisEventResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response: JarvisEventResponse = client.get("$serverUrl/events") {
                    url { parameters.append("since", since.toString()) }
                }.body()
                Result.success(response)
            } catch (e: Exception) { Result.failure(e) }
        }
    }
}