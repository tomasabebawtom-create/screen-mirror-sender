package com.example.screenmirror

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class SignalingClient(
    private val serverUrl: String,
    private val listener: Listener
) {
    interface Listener {
        fun onOpen()
        fun onClosed()
        fun onViewerJoined()
        fun onRemoteAnswer(sdp: SessionDescription)
        fun onRemoteIceCandidate(candidate: IceCandidate)
        fun onTouchEvent(json: JSONObject)
    }

    private var ws: WebSocket? = null
    private val client = OkHttpClient()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var reconnectAttempt = 0
    private var manuallyClosed = false

    fun connect() {
        manuallyClosed = false
        val request = Request.Builder().url(serverUrl).build()
        ws = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempt = 0
                val register = JSONObject().apply {
                    put("type", "register")
                    put("role", "sender")
                }
                webSocket.send(register.toString())
                listener.onOpen()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = JSONObject(text)
                when (msg.optString("type")) {
                    "viewer-joined" -> listener.onViewerJoined()
                    "answer" -> listener.onRemoteAnswer(
                        SessionDescription(SessionDescription.Type.ANSWER, msg.getString("sdp"))
                    )
                    "ice-candidate" -> {
                        val c = msg.getJSONObject("candidate")
                        listener.onRemoteIceCandidate(
                            IceCandidate(
                                c.optString("sdpMid"),
                                c.optInt("sdpMLineIndex"),
                                c.getString("candidate")
                            )
                        )
                    }
                    "touch" -> listener.onTouchEvent(msg)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener.onClosed()
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onClosed()
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (manuallyClosed) return
        reconnectAttempt++
        // Backoff: 2s, 4s, 8s ... capped at 30s - covers "data turned back on" cases
        // without hammering the server while offline.
        val delayMs = minOf(2000L * (1 shl minOf(reconnectAttempt, 4)), 30000L)
        mainHandler.postDelayed({ if (!manuallyClosed) connect() }, delayMs)
    }

    fun sendOffer(sdp: SessionDescription) {
        ws?.send(JSONObject().apply {
            put("type", "offer")
            put("sdp", sdp.description)
        }.toString())
    }

    fun sendIceCandidate(candidate: IceCandidate) {
        val c = JSONObject().apply {
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
            put("candidate", candidate.sdp)
        }
        ws?.send(JSONObject().apply {
            put("type", "ice-candidate")
            put("candidate", c)
        }.toString())
    }

    fun close() {
        manuallyClosed = true
        mainHandler.removeCallbacksAndMessages(null)
        ws?.close(1000, "bye")
    }
}
