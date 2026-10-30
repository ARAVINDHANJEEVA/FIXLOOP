package com.fixloop.mobile.data.websocket

import android.content.Context
import android.util.Log
import com.fixloop.mobile.data.local.SessionManager
import com.fixloop.mobile.data.remote.NetworkClient
import kotlinx.coroutines.*
import okhttp3.*
import java.util.concurrent.atomic.AtomicBoolean

class StompWebSocketClient(
    private val context: Context,
    private val onMessageReceived: (destination: String, body: String) -> Unit
) {
    private val tag = "StompWebSocket"
    private val sessionManager = SessionManager(context)
    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val activeSubscriptions = mutableMapOf<String, String>() // subId -> destination
    private var subscriptionCounter = 0

    fun connect() {
        if (isConnected.get()) return

        val token = sessionManager.getToken()
        val userId = sessionManager.getUserId()
        if (token == null || userId <= 0) {
            Log.d(tag, "No active session, skipping WebSocket connection")
            return
        }

        val baseUrl = sessionManager.getBaseUrl()
        // Convert http://... or https://... to ws://... or wss://...
        val wsUrl = baseUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .removeSuffix("/") + "/ws"

        Log.d(tag, "Connecting to STOMP WebSocket at $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        val client = NetworkClient.getRawOkHttpClient(context)
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(tag, "WebSocket connection opened, sending STOMP CONNECT")
                sendConnectFrame(ws, token)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingStompMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closing: code=$code, reason=$reason")
                isConnected.set(false)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closed: code=$code, reason=$reason")
                isConnected.set(false)
                scheduleReconnect()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "WebSocket failure: ${t.message}")
                isConnected.set(false)
                scheduleReconnect()
            }
        })
    }

    private fun sendConnectFrame(ws: WebSocket, token: String) {
        val connectFrame = buildString {
            append("CONNECT\n")
            append("accept-version:1.1,1.2\n")
            append("heart-beat:10000,10000\n")
            append("Authorization:Bearer $token\n")
            append("\n")
            append("\u0000")
        }
        ws.send(connectFrame)
    }

    private fun handleIncomingStompMessage(rawMessage: String) {
        val lines = rawMessage.split("\n")
        if (lines.isEmpty()) return

        val command = lines[0].trim()
        when (command) {
            "CONNECTED" -> {
                Log.i(tag, "STOMP handshake successful! CONNECTED.")
                isConnected.set(true)
                reconnectJob?.cancel()

                // Auto-subscribe to student's personal topic
                val userId = sessionManager.getUserId()
                if (userId > 0) {
                    subscribe("/topic/student/$userId")
                }

                // Re-subscribe to any previously active subscriptions
                activeSubscriptions.values.toList().forEach { dest ->
                    subscribe(dest)
                }
            }
            "MESSAGE" -> {
                var destination = ""
                var bodyStartIndex = -1

                for (i in 1 until lines.size) {
                    val line = lines[i]
                    if (line.startsWith("destination:")) {
                        destination = line.substringAfter("destination:").trim()
                    } else if (line.isEmpty()) {
                        bodyStartIndex = i + 1
                        break
                    }
                }

                if (bodyStartIndex != -1 && bodyStartIndex < lines.size) {
                    val body = lines.subList(bodyStartIndex, lines.size)
                        .joinToString("\n")
                        .removeSuffix("\u0000")
                        .trim()

                    Log.d(tag, "STOMP message received on [$destination]: $body")
                    scope.launch(Dispatchers.Main) {
                        onMessageReceived(destination, body)
                    }
                }
            }
            "ERROR" -> {
                Log.e(tag, "STOMP error frame: $rawMessage")
            }
        }
    }

    fun subscribe(destination: String) {
        val ws = webSocket ?: return
        if (!isConnected.get()) return

        val subId = "sub-${subscriptionCounter++}"
        activeSubscriptions[subId] = destination

        val subscribeFrame = buildString {
            append("SUBSCRIBE\n")
            append("id:$subId\n")
            append("destination:$destination\n")
            append("ack:auto\n")
            append("\n")
            append("\u0000")
        }
        ws.send(subscribeFrame)
        Log.d(tag, "Subscribed to $destination with id=$subId")
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(5000) // retry every 5 seconds
            Log.d(tag, "Attempting STOMP reconnect...")
            connect()
        }
    }

    fun disconnect() {
        isConnected.set(false)
        reconnectJob?.cancel()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        activeSubscriptions.clear()
    }
}
