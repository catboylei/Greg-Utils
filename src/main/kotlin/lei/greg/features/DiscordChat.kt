package lei.greg.features

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import lei.greg.GregUtils.PLAYER_UUID
import lei.greg.Utils
import lei.greg.config.ConfigManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.client.network.ClientCommandSource
import net.minecraft.command.CommandSource
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

@Serializable
data class DiscordMessage(val name: String = "", val message: String, val channel_name: String = "", val guild: String = "", val type: String, val available_channels: List<String> = emptyList())

object DiscordChat {

    private val client: HttpClient = HttpClient.newHttpClient()
    private var webSocket: WebSocket? = null
    private val url = "wss://awawa.fluffy-paws.dev"
    private var availableChannels = emptyList<String>()
    private var isCommandRegistered = false

    fun register() {
        if (!ConfigManager.getBool("fkl discord bridge") || !ConfigManager.getBool("master toggle")) return
        Utils.discordMessage("info", "Connecting...")
        connect(PLAYER_UUID) { payload ->
            if (ConfigManager.getBool("fkl discord bridge") || ConfigManager.getBool("master toggle")) {
                handlePayload(payload)
            }
        }
    }

    // connects to the hardcoded url, sends auth then listens forever calling onMessage on each
    private fun connect(uuid: String, onMessage: (String) -> Unit): CompletableFuture<WebSocket> {
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

    private fun send(msg: String) {
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

    private fun handlePayload(payload: String) {
        try {
            val msg = Json.decodeFromString<DiscordMessage>(payload)

            if (msg.available_channels.isNotEmpty()) {
                availableChannels = msg.available_channels

                if (!isCommandRegistered) {
                    registerMessageCommand()
                    isCommandRegistered = true
                }
            }

            Utils.discordMessage(msg.type, msg.message, msg.name, msg.channel_name)
        } catch (e: Exception) {
            Utils.discordMessage("info", payload) // if not serializable just print it in chat as info
        }
    }

    private fun registerMessageCommand() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommandManager.literal("d")
                    .then(ClientCommandManager.argument("channel", StringArgumentType.string())
                        .suggests { _, builder -> CommandSource.suggestMatching(availableChannels, builder) }
                        .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                            .executes { context -> sendMessageCommand(context) }
                        )
                    )
            )
        }
    }

    @Suppress("SameReturnValue")
    private fun sendMessageCommand(context: CommandContext<FabricClientCommandSource>): Int {
        val msg = StringArgumentType.getString(context, "message")
        val channel = StringArgumentType.getString(context, "channel")

        send(" {\"message\": \"$msg\", \"channel\": \"$channel\"}")
        return 1
    }
}