package com.example.miles.wear

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * GMS-free peer transport over the local network (no Google Play Services).
 *
 * How it works:
 *  1. Both apps broadcast a tiny UDP beacon (identity + real battery/status)
 *     on the local WiFi, plus loopback so both apps can pair when they run on
 *     the same device.
 *  2. On seeing a beacon, each app opens a TCP channel to the peer's advertised
 *     port and sends /miles/hello (mutual introduction, so discovery works even
 *     when one app had to take a fallback port).
 *  3. Everything after that is newline-delimited JSON frames — the exact same
 *     paths the old Play Services messages used.
 */
class LanTransport(
    private val role: String,
    private val selfId: String,
    private val selfName: String,
    private val selfModel: String,
    private val appVersion: String,
    private val scope: CoroutineScope,
    private val batteryProvider: () -> Int = { -1 },
    private val statusProvider: () -> String = { "idle" },
    private val onMessage: (path: String, payload: JSONObject) -> Unit = { _, _ -> },
    private val onPeersChanged: (peers: List<LanProtocol.Beacon>) -> Unit = {}
) {

    data class Peer(
        val beacon: LanProtocol.Beacon,
        val host: String,
        @Volatile var lastSeenMs: Long
    )

    private val peers = ConcurrentHashMap<String, Peer>()
    private val outbound = ConcurrentHashMap<String, Socket>()
    private val inboundSockets = CopyOnWriteArrayList<Socket>()

    @Volatile private var tcpPort: Int = 0
    @Volatile private var running = false
    private var jobs = mutableListOf<Job>()

    fun start() {
        if (running) return
        running = true
        // TCP server on an ephemeral port (avoids clashes; port is advertised)
        runCatching {
            ServerSocket(0).also { srv ->
                tcpPort = srv.localPort
                jobs += scope.launch(Dispatchers.IO) { acceptLoop(srv) }
            }
        }.onFailure { Log.e(TAG, "TCP server failed: ${it.message}") }

        // UDP discovery
        runCatching {
            val discovery = DatagramSocket()
            discovery.broadcast = true
            discovery.soTimeout = 800
            jobs += scope.launch(Dispatchers.IO) { discoveryLoop(discovery) }
        }.onFailure { Log.e(TAG, "Discovery socket failed: ${it.message}") }

        jobs += scope.launch(Dispatchers.IO) { beaconLoop() }
        Log.i(TAG, "started role=$role id=$selfId tcp=$tcpPort")
    }

    fun stop() {
        running = false
        jobs.forEach { it.cancel() }
        jobs.clear()
        inboundSockets.forEach { runCatching { it.close() } }
        inboundSockets.clear()
        outbound.values.forEach { runCatching { it.close() } }
        outbound.clear()
    }

    private fun selfBeacon(): LanProtocol.Beacon = LanProtocol.Beacon(
        role = role,
        id = selfId,
        name = selfName,
        model = selfModel,
        tcpPort = tcpPort,
        appVersion = appVersion,
        battery = runCatching { batteryProvider() }.getOrDefault(-1),
        status = runCatching { statusProvider() }.getOrDefault("idle")
    )

    // ---------- loops ----------

    private suspend fun acceptLoop(server: ServerSocket) {
        while (running && currentCoroutineContext().isActive && !server.isClosed) {
            val socket = runCatching { server.accept() }.getOrNull() ?: break
            inboundSockets += socket
            scope.launch(Dispatchers.IO) { readLoop(socket) }
        }
    }

    private fun readLoop(socket: Socket) {
        runCatching {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            while (running) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) continue
                val (path, payload) = LanProtocol.parseMessage(line) ?: continue
                if (path == LanProtocol.PATH_HELLO) {
                    val beacon = LanProtocol.beaconFromHello(payload) ?: return@runCatching
                    upsert(beacon, socket.inetAddress?.hostAddress ?: "127.0.0.1")
                } else {
                    onMessage(path, payload)
                }
            }
        }
        runCatching { socket.close() }
    }

    private suspend fun discoveryLoop(socket: DatagramSocket) {
        val buffer = ByteArray(2048)
        while (running && currentCoroutineContext().isActive) {
            val packet = DatagramPacket(buffer, buffer.size)
            runCatching { socket.receive(packet) }.onSuccess {
                val json = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val beacon = LanProtocol.parseBeacon(json) ?: return@onSuccess
                if (beacon.id == selfId || beacon.role == role) return@onSuccess
                val host = packet.address?.hostAddress ?: return@onSuccess
                upsert(beacon, host)
                // Open the TCP channel + introduce ourselves (works both ways)
                scope.launch(Dispatchers.IO) { connectAndHello(beacon.id, host, beacon.tcpPort) }
            }
            // purge stale peers
            val now = System.currentTimeMillis()
            var changed = false
            peers.entries.toList().forEach { (id, peer) ->
                if (now - peer.lastSeenMs > LanProtocol.PEER_TIMEOUT_MS) {
                    peers.remove(id)
                    runCatching { outbound.remove(id)?.close() }
                    changed = true
                }
            }
            if (changed) notifyPeers()
        }
    }

    private suspend fun beaconLoop() {
        val beaconJson = LanProtocol.encodeBeacon(selfBeacon())
        val data = beaconJson.toByteArray(Charsets.UTF_8)
        val targets: List<InetAddress> = listOfNotNull(
            runCatching { InetAddress.getByName("255.255.255.255") }.getOrNull(),
            runCatching { InetAddress.getByName("127.0.0.1") }.getOrNull()
        )

        var socket: DatagramSocket? = null
        while (running && currentCoroutineContext().isActive) {
            runCatching {
                socket = socket ?: DatagramSocket().apply { broadcast = true }
                targets.forEach { addr ->
                    runCatching {
                        socket?.send(DatagramPacket(data, data.size, addr, LanProtocol.DISCOVERY_PORT))
                    }
                }
            }
            delay(LanProtocol.BEACON_INTERVAL_MS)
        }
        runCatching { socket?.close() }
    }

    // ---------- peers & channels ----------

    private fun upsert(beacon: LanProtocol.Beacon, host: String) {
        val existing = peers[beacon.id]
        peers[beacon.id] = Peer(beacon, host, System.currentTimeMillis())
        if (existing == null || existing.beacon != beacon) notifyPeers()
    }

    private fun notifyPeers() {
        val now = System.currentTimeMillis()
        onPeersChanged(
            peers.values
                .filter { now - it.lastSeenMs <= LanProtocol.PEER_TIMEOUT_MS }
                .map { it.beacon }
        )
    }

    private fun connectAndHello(peerId: String, host: String, port: Int): Socket? {
        outbound[peerId]?.let { if (!it.isClosed && it.isConnected) return it }
        if (port <= 0) return null
        return runCatching {
            Socket(host, port).apply {
                tcpNoDelay = true
                soTimeout = 15000
            }.also { socket ->
                outbound[peerId] = socket
                scope.launch(Dispatchers.IO) { readLoop(socket) }
                val hello = LanProtocol.helloPayload(selfBeacon()).toString()
                writeFrame(socket, LanProtocol.encodeMessage(LanProtocol.PATH_HELLO, hello))
            }
        }.getOrElse {
            outbound.remove(peerId)
            null
        }
    }

    private fun writeFrame(socket: Socket, frame: String): Boolean = runCatching {
        socket.getOutputStream().apply {
            write((frame + "\n").toByteArray(Charsets.UTF_8))
            flush()
        }
        true
    }.getOrElse { false }

    /** Sends to the freshest peer of the given role; false when nobody is reachable. */
    fun send(toRole: String, path: String, payloadJson: String): Boolean {
        val now = System.currentTimeMillis()
        val peer = peers.values
            .filter { it.beacon.role == toRole && now - it.lastSeenMs <= LanProtocol.PEER_TIMEOUT_MS }
            .maxByOrNull { it.lastSeenMs } ?: return false
        val socket = connectAndHello(peer.beacon.id, peer.host, peer.beacon.tcpPort) ?: return false
        return writeFrame(socket, LanProtocol.encodeMessage(path, payloadJson))
    }

    fun isPeerPresent(roleWanted: String): Boolean {
        val now = System.currentTimeMillis()
        return peers.values.any { it.beacon.role == roleWanted && now - it.lastSeenMs <= LanProtocol.PEER_TIMEOUT_MS }
    }

    fun peersNow(): List<LanProtocol.Beacon> {
        val now = System.currentTimeMillis()
        return peers.values.filter { now - it.lastSeenMs <= LanProtocol.PEER_TIMEOUT_MS }.map { it.beacon }
    }

    companion object {
        const val TAG = "MilesLan"
    }
}