package lei.greg.features

import lei.greg.GregUtils.PLAYER_UUID
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

object DiscordChat {

    private val client: HttpClient = HttpClient.newHttpClient()
    private var webSocket: WebSocket? = null
    private val url = "ws://fi15.bot-hosting.net:26529"

    fun register() {
        connect(PLAYER_UUID) { message ->
            println("Received: $message")
        }
    }

    // connects to the hardcoded url, sends uuid for auth then listens forever calling onMessage on each
    fun connect(uuid: String, onMessage: (String) -> Unit): CompletableFuture<WebSocket> {
        val messageBuilder = StringBuilder()

        val listener = object : WebSocket.Listener {

            override fun onOpen(webSocket: WebSocket) {
                println("BotSocket: onOpen fired, sending auth")
                webSocket.sendText(uuid, true)
                webSocket.request(1)
            }

            override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*> {
                messageBuilder.append(data)
                if (last) {
                    val fullMessage = messageBuilder.toString()
                    messageBuilder.clear()
                    onMessage(fullMessage)
                }
                webSocket.request(1)
                return CompletableFuture.completedFuture(null)
            }

            override fun onError(webSocket: WebSocket, error: Throwable) {
                println("BotSocket: onError fired")
                error.printStackTrace()
            }

            override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*> {
                println("BotSocket: onClose fired: $statusCode $reason")
                return CompletableFuture.completedFuture(null)
            }
        }

        return client.newWebSocketBuilder()
            .buildAsync(URI.create(url), listener)
            .thenApply { ws -> webSocket = ws; ws }
            .exceptionally { ex ->
                println("BotSocket: connect FAILED")
                ex.printStackTrace()
                null
            }
    }

    fun send(msg: String) {
        webSocket?.sendText(msg, true)
    }

    fun close() {
        webSocket?.sendClose(WebSocket.NORMAL_CLOSURE, "bye")
        webSocket = null
    }
}