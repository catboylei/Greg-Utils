package lei.greg.features

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import lei.greg.GregUtils.PLAYER_UUID
import lei.greg.Utils
import lei.greg.config.ConfigManager
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

// todo use secure socket (i cba rn)

@Serializable
data class DiscordMessage(val name: String = "", val message: String, val channel_name: String = "", val guild: String = "", val type: String)

object DiscordChat {

    private val client: HttpClient = HttpClient.newHttpClient()
    private var webSocket: WebSocket? = null
    private val url = "ws://fi15.bot-hosting.net:26529"

    fun register() {
        if (!ConfigManager.getBool("fkl discord bridge")) return
        Utils.discordMessage("info", "Connecting...")
        connect(PLAYER_UUID) { payload ->
            if (ConfigManager.getBool("fkl discord bridge")) {
                handlePayload(payload)
            }
        }
    }

    // connects to the hardcoded url, sends uuid for auth then listens forever calling onMessage on each
    fun connect(uuid: String, onMessage: (String) -> Unit): CompletableFuture<WebSocket> {
        val messageBuilder = StringBuilder()

        val listener = object : WebSocket.Listener {

            override fun onOpen(webSocket: WebSocket) {
                println("BotSocket: onOpen fired, sending auth")
                webSocket.sendText("{\"uuid\": \"$uuid\", \"password\": \"${ConfigManager.getString("fkl password")}\"}", true)
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
                Utils.discordMessage("info", "Disconnected")
                println("BotSocket: onClose fired: $statusCode $reason")
                DiscordChat.webSocket = null
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
        if (!ConfigManager.getBool("fkl discord bridge")) {
            Utils.discordMessage("info", "enable the bridge feature you goober")
        } else if (webSocket == null) {
            Utils.discordMessage("info", "Not connected, is your account linked ?")
        } else if (ConfigManager.getBool("fkl discord bridge")) {
            webSocket?.sendText(msg, true)
        }
    }

    fun close() {
        webSocket?.sendClose(WebSocket.NORMAL_CLOSURE, "bye")
        webSocket = null
    }

    fun handlePayload(payload: String) {
        try {
            val msg = Json.decodeFromString<DiscordMessage>(payload)

            Utils.discordMessage(msg.type, msg.message, msg.name, msg.channel_name)
        } catch (e: Exception) {
            Utils.discordMessage("info", payload) // if not serializable just print it in chat as info
        }
    }
}