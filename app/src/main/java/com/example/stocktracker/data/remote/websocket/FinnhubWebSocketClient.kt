package com.example.stocktracker.data.remote.websocket

import android.util.Log
import com.example.stocktracker.data.remote.dto.WebSocketMessageDto
import com.example.stocktracker.domain.model.LivePrice
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "FinnhubWS"
private const val NORMAL_CLOSURE_CODE = 1000

// Cold callbackFlow: socket opens when the first collector subscribes and is
// gracefully closed (with unsubscribe messages) when the flow is cancelled.
// OkHttp delivers callbacks on its own threads — trySend is thread-safe.
class FinnhubWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @Named("finnhub_ws_url") private val wsUrl: String,
    private val gson: Gson,
) {
    fun observePrices(symbols: List<String>): Flow<LivePrice> = callbackFlow {
        if (symbols.isEmpty()) {
            close()
            return@callbackFlow
        }

        Log.d(TAG, "Opening WebSocket for ${symbols.size} symbol(s): $symbols")
        val request = Request.Builder().url(wsUrl).build()

        val listener = object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "onOpen — subscribing to $symbols")
                symbols.forEach { webSocket.send(subscribeMessage(it)) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val message = gson.fromJson(text, WebSocketMessageDto::class.java)
                    when (message.type) {
                        "trade" -> message.data?.forEach { trade ->
                            Log.v(TAG, "trade ${trade.symbol} @ ${trade.price}")
                            trySend(LivePrice(trade.symbol, trade.price, trade.volume, trade.timestamp))
                        }
                        "ping"  -> Log.v(TAG, "ping")
                        "error" -> Log.w(TAG, "server error: $text")
                        else    -> Log.d(TAG, "unhandled type='${message.type}'")
                    }
                }.onFailure { Log.w(TAG, "parse error: $text", it) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure (HTTP ${response?.code})", t)
                close(t)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "onClosing code=$code reason=$reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "onClosed code=$code reason=$reason")
                close()
            }
        }

        val webSocket = okHttpClient.newWebSocket(request, listener)

        awaitClose {
            symbols.forEach { webSocket.send(unsubscribeMessage(it)) }
            webSocket.close(NORMAL_CLOSURE_CODE, "Flow collector cancelled")
        }
    }

    private fun subscribeMessage(symbol: String) =
        """{"type":"subscribe","symbol":"$symbol"}"""

    private fun unsubscribeMessage(symbol: String) =
        """{"type":"unsubscribe","symbol":"$symbol"}"""
}
