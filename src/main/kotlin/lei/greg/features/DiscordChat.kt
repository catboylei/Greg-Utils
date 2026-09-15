package lei.greg.features

import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lei.greg.GregUtils
import lei.greg.GregUtils.PLAYER_UUID
import lei.greg.Utils
import lei.greg.config.ConfigManager
import lei.greg.utils.Scheduler
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.command.CommandSource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

// serializable json objects
@Serializable data class OutgoingAuthPayload(val uuid: String, val password: String)
@Serializable data class OutgoingMessagePayload(val message: String, val channel: String)

@Serializable data class IncomingChatMessage(val name: String, val message: String, val channelName: String, val guild: String, val type: String)
@Serializable data class IncomingInfoPayload(val message: String, val type: String)
@Serializable data class IncomingConnectSuccessPayload(val message: String, val type: String, val availableChannels: List<String>)

@Serializable data class PayloadEnvelope(val type: String) // used to determine which data class to serialize into

object DiscordChat {

    private const val URL = "wss://awawa.fluffy-paws.dev"
    private const val RECONNECT_DELAY_TICKS = 40
    private val client: HttpClient = HttpClient.newHttpClient()

    private var webSocket: WebSocket? = null
    private var availableChannels = emptyList<String>()
    private var isCommandRegistered = false
    private var isLoggedIn = false

    fun register() {
        if (!isEnabled()) return

        Utils.discordMessage("info", "Connecting...")
        connect(PLAYER_UUID) { payload -> if (isEnabled()) { handlePayload(payload) } }
    }

    // connects to url, sends auth then listens calling onMessage on each
    private fun connect(uuid: String, onMessage: (String) -> Unit): CompletableFuture<WebSocket> {
        val messageBuilder = StringBuilder()

        val listener = object : WebSocket.Listener {

            // on connection opened send auth
            override fun onOpen(webSocket: WebSocket) {
                GregUtils.LOGGER.info("GregBridge: onOpen fired, sending auth")

                val auth = OutgoingAuthPayload(uuid, ConfigManager.getString("fkl password")!!) // NOTE: should be non-null but to be monitored
                webSocket.sendText(Json.encodeToString(OutgoingAuthPayload.serializer(), auth), true) // bypasses send wrapper (intentional)

                webSocket.request(1) // listens for sent data
            }

            // on receive text from websocket, call onMessage on it
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
                GregUtils.LOGGER.warn("GregBridge: onError fired")
                error.printStackTrace()
            }

            override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*> {
                GregUtils.LOGGER.info("GregBridge: onClose fired: $statusCode $reason")

                // auto reconnect on unexpected disconnect
                if (isEnabled() && isLoggedIn) {
                    Utils.discordMessage("info", "Disconnected unexpectedly, reconnecting soon...")
                    Scheduler.schedule(RECONNECT_DELAY_TICKS) { register() }
                } else {
                    Utils.discordMessage("info", "Disconnected")
                }

                // clean up but keep isLoggedIn to true (because auth is still correct)
                DiscordChat.webSocket = null
                return CompletableFuture.completedFuture(null)
            }
        }

        return client.newWebSocketBuilder()
            .buildAsync(URI.create(URL), listener)
            .thenApply { ws -> webSocket = ws; ws }
            .whenComplete { _, throwable ->
                // if unexpected https response (usually server offline or rebooting)
                if (throwable != null) {
                    throwable.printStackTrace()
                    GregUtils.LOGGER.error("GregBridge: connect FAILED")

                    Utils.discordMessage("info", "Connection failed, is server offline?")

                    webSocket = null
                    isLoggedIn = false
                }
            }
    }

    // wrapper on send used for outbound discord messages
    private fun send(msg: String) {
        val socket = webSocket
        when {
            !isEnabled() -> Utils.discordMessage("info", "enable the bridge feature you goober")
            !isLoggedIn || socket == null -> Utils.discordMessage("info", "Not connected, is your account linked ?")
            else -> socket.sendText(msg, true)
        }
    }

    fun close() {
        webSocket?.sendClose(WebSocket.NORMAL_CLOSURE, "bye")
        webSocket = null
    }

    private fun isEnabled(): Boolean {
        return ConfigManager.getBool("fkl discord bridge") && ConfigManager.getBool("master toggle")
    }

    private val json = Json { ignoreUnknownKeys = true } // not-strict json instance (for the envelope)

    // tries serializing payload as every serializable defined
    private fun handlePayload(payload: String) {

        try {
            // match type with its associated format (strict)
            // this could be simplified but its very verbose so that we can easily add stuff <3
            when (val type = json.decodeFromString<PayloadEnvelope>(payload).type) {
                "info" -> {
                    val msg = Json.decodeFromString<IncomingInfoPayload>(payload)

                    Utils.discordMessage("info", msg.message)
                }
                "chat" -> {
                    val msg = Json.decodeFromString<IncomingChatMessage>(payload)

                    Utils.discordMessage("chat", msg.message, msg.name, msg.channelName)
                }
                "raid" -> {
                    val msg = Json.decodeFromString<IncomingChatMessage>(payload) // same fields as chat msg

                    Utils.discordMessage("raid", msg.message, msg.name)
                }
                "connectionSuccess" -> {
                    val msg = Json.decodeFromString<IncomingConnectSuccessPayload>(payload)

                    availableChannels = msg.availableChannels
                    isLoggedIn = true

                    if (!isCommandRegistered) { registerMessageCommand() }

                    Utils.discordMessage("info", msg.message)
                }
                "connectionFail" -> {
                    val msg = Json.decodeFromString<IncomingInfoPayload>(payload) // same fields as info msg

                    isLoggedIn = false

                    Utils.discordMessage("info", msg.message)
                }
                else -> { GregUtils.LOGGER.warn("GregBridge: unknown type \"$type\" on payload \"$payload\"") }
            }
        } catch (e: Exception) { GregUtils.LOGGER.warn("Failed to serialize payload \"$payload\": $e") }
    }

    private fun registerMessageCommand() {
        isCommandRegistered = true

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommandManager.literal("d")
                    .then(ClientCommandManager.argument("channel", StringArgumentType.string())
                        .suggests { _, builder -> CommandSource.suggestMatching(availableChannels, builder) }
                        .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                            .executes { context ->
                                val msg = StringArgumentType.getString(context, "message")
                                val channel = StringArgumentType.getString(context, "channel")

                                val payload = OutgoingMessagePayload(msg, channel)
                                send(Json.encodeToString(OutgoingMessagePayload.serializer(), payload))

                                return@executes 1
                            }
                        )
                    )
            )
        }
    }
}