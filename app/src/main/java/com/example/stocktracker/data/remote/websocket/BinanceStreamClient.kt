package com.example.stocktracker.data.remote.websocket

import android.util.Log
import com.example.stocktracker.data.remote.dto.CombinedStreamEnvelopeDto
import com.example.stocktracker.domain.model.LivePrice
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.retryWhen
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.min

private const val TAG = "BinanceWS"
private const val NORMAL_CLOSURE_CODE = 1000

private const val BASE_BACKOFF_MS = 1_000L
private const val MAX_BACKOFF_MS = 30_000L

class BinanceStreamClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @Named("binance_stream_url") private val streamBaseUrl: String,
    private val gson: Gson,
) {
    fun observeTickers(symbols: List<String>): Flow<LivePrice> {
        if (symbols.isEmpty()) return emptyFlow()
        return openConnection(symbols).retryWhen { cause, attempt ->
            if (cause !is StreamDroppedException) return@retryWhen false
            val backoff = min(MAX_BACKOFF_MS, BASE_BACKOFF_MS shl attempt.toInt().coerceAtMost(5))
            Log.w(TAG, "reconnecting in ${backoff}ms (attempt ${attempt + 1}): ${cause.message}")
            delay(backoff)
            true
        }
    }

    private fun openConnection(symbols: List<String>): Flow<LivePrice> = callbackFlow {
        val streamNames = symbols.joinToString("/") { "${it.lowercase()}@ticker" }
        val url = "$streamBaseUrl?streams=$streamNames"
        Log.d(TAG, "open: ${symbols.size} symbol(s) ${symbols.take(5)}${if (symbols.size > 5) "…" else ""}")
        val request = Request.Builder().url(url).build()

        val listener = object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "onOpen ${response.code}")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val envelope = gson.fromJson(text, CombinedStreamEnvelopeDto::class.java)
                    val payload = envelope?.data ?: return@runCatching
                    val sym = payload.symbol ?: return@runCatching
                    val price = payload.lastPrice?.toDoubleOrNull() ?: return@runCatching
                    val changePct = payload.priceChangePercent?.toDoubleOrNull()
                    trySend(LivePrice(id = sym, price = price, changePercent24Hr = changePct))
                }.onFailure { Log.w(TAG, "parse error: $text", it) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "onFailure (HTTP ${response?.code})", t)
                close(StreamDroppedException("failure: ${t.message}", t))
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "onClosing $code $reason")
                if (code == NORMAL_CLOSURE_CODE) close()
                else close(StreamDroppedException("closing $code: $reason"))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "onClosed $code $reason")
                if (code == NORMAL_CLOSURE_CODE) close()
                else close(StreamDroppedException("closed $code: $reason"))
            }
        }

        val webSocket = okHttpClient.newWebSocket(request, listener)
        awaitClose { webSocket.close(NORMAL_CLOSURE_CODE, "collector cancelled") }
    }
}

private class StreamDroppedException(message: String, cause: Throwable? = null) :
    IOException(message, cause)
