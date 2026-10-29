package lei.greg.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import lei.greg.GregUtils
import lei.greg.GregUtils.PLAYER_UUID
import lei.greg.Utils
import lei.greg.config.ConfigManager
import lei.greg.events.AwawaPayloadEvents
import lei.greg.events.AwawaStatusUpdated
import lei.greg.utils.Scheduler
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

@Serializable data class OutgoingAuthPayload(val uuid: String, val password: String)

@Serializable
sealed class AwawaPayload {
    @Serializable @SerialName("info")
    data class Info(val message: String) : AwawaPayload()

    @Serializable @SerialName("chat")
    data class Chat(val message: String, val name: String, val channelName: String) : AwawaPayload()

    @Serializable @SerialName("raid")
    data class Raid(val message: String, val name: String) : AwawaPayload()

    @Serializable @SerialName("connectionSuccess")
    data class ConnectionSuccess(val message: String, val availableChannels: List<String>) : AwawaPayload()

    @Serializable @SerialName("connectionFail")
    data class ConnectionFail(val message: String) : AwawaPayload()

    @Serializable @SerialName("error")
    data class Error(val message: String) : AwawaPayload()

    @Serializable @SerialName("annieStatus")
    data class AnnieStatus(val isActive: Boolean, val timestamp: Int?) : AwawaPayload()
}

object Backend {
    private const val URL = "wss://awawa.fluffy-paws.dev"
    private const val UNSECURED_URL = "ws://fi15.bot-hosting.net:26529" // evil ass direct url in case cloudflare goes boom
    private const val RECONNECT_DELAY_TICKS = 40

    private val client: HttpClient = HttpClient.newHttpClient()
    private val json = Json { classDiscriminator = "type"; ignoreUnknownKeys = true }

    private var webSocket: WebSocket? = null
    @Volatile private var shouldReconnect = true
    @Volatile var status: Text = Text.literal("OFFLINE").formatted(Formatting.RED)
        private set
    @Volatile var isConnecting = false
        private set

    @Volatile
    var isLoggedIn = false
        private set
	var availableChannels = emptyList<String>()

    fun register() {
        connectionHooks()
        GregUtils.LOGGER.info("Connecting...")
        shouldReconnect = true
        connect(PLAYER_UUID)
    }

    private fun setStatus(text: Text) {
        status = text
        dispatch { AwawaStatusUpdated.EVENT.invoker().onUpdate(text) }
    }

    // manual
    fun reconnect() {
        if (isConnecting) return
        close()
        shouldReconnect = true
        connect(PLAYER_UUID)
    }

    // automatic
    private fun autoReconnect() {
        if (isConnecting || !shouldReconnect || webSocket != null) return
        GregUtils.LOGGER.info("Reconnecting...")
        connect(PLAYER_UUID)
    }

    private fun connect(uuid: String): CompletableFuture<WebSocket> {
        val messageBuilder = StringBuilder()
        isConnecting = true

        val listener = object : WebSocket.Listener {
            override fun onOpen(webSocket: WebSocket) {
                GregUtils.LOGGER.info("GregBridge: onOpen fired")
                val auth = OutgoingAuthPayload(uuid, ConfigManager.getString("fkl password")!!)
                GregUtils.LOGGER.info(json.encodeToString(OutgoingAuthPayload.serializer(), auth))
                webSocket.sendText(json.encodeToString(OutgoingAuthPayload.serializer(), auth), true)
                setStatus(Text.literal("CONNECTING...").formatted(Formatting.YELLOW))
                webSocket.request(1)
            }

            override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*> {
                messageBuilder.append(data)
                if (last) {
                    val fullMessage = messageBuilder.toString()
                    messageBuilder.clear()
                    handlePayload(fullMessage)
                }
                webSocket.request(1)
                return CompletableFuture.completedFuture(null)
            }

            override fun onError(webSocket: WebSocket, error: Throwable) {
                GregUtils.LOGGER.warn("GregBridge: onError fired")
                error.printStackTrace()
                isConnecting = false
            }

            override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*> {
                GregUtils.LOGGER.info("GregBridge: onClose fired: $statusCode $reason")
                setStatus(Text.literal("DISCONNECTED").formatted(Formatting.RED))

                if (Backend.webSocket !== webSocket) return CompletableFuture.completedFuture(null)

                isConnecting = false
                val wasLoggedIn = isLoggedIn
                Backend.webSocket = null
                isLoggedIn = false

                if (shouldReconnect && wasLoggedIn) {
                    GregUtils.LOGGER.warn("Disconnected unexpectedly, reconnecting soon...")
                    Scheduler.schedule(RECONNECT_DELAY_TICKS) { autoReconnect() }
                } else {
                    GregUtils.LOGGER.warn("Disconnected")
                }
                return CompletableFuture.completedFuture(null)
            }
        }

        val urlToUse = if (ConfigManager.getBool("use unsecured socket")) UNSECURED_URL else URL
        return client.newWebSocketBuilder()
            .buildAsync(URI.create(urlToUse), listener)
            .thenApply { ws -> webSocket = ws; ws }
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    throwable.printStackTrace()
                    GregUtils.LOGGER.error("GregBridge: connect FAILED")
                    webSocket = null
                    dispatch {
                        AwawaPayloadEvents.CONNECTION_FAIL.invoker().on(AwawaPayload.ConnectionFail("Connection failed, is server offline?"))
                    }
                    isConnecting = false
                }
            }
    }

    // returns false if not connected
    fun send(msg: String): Boolean {
        val socket = webSocket
        if (!isLoggedIn || socket == null) return false

        GregUtils.LOGGER.info("sending payload $msg")
        socket.sendText(msg, true)
        return true
    }

    fun close() {
        isConnecting = false
        shouldReconnect = false
        isLoggedIn = false
        webSocket?.sendClose(WebSocket.NORMAL_CLOSURE, "bye")
        webSocket = null
    }

    // websocket callbacks are not on the client thread
    private fun dispatch(block: () -> Unit) = MinecraftClient.getInstance().execute(block)

    private fun handlePayload(payload: String) {
        val parsed = try {
            json.decodeFromString<AwawaPayload>(payload)
        } catch (e: SerializationException) {
            GregUtils.LOGGER.warn("GregBridge: bad or unknown payload \"$payload\": $e")
            return
        }

		GregUtils.LOGGER.info("Greg: received payload $payload")

        dispatch {
            try {
                when (parsed) {
                    is AwawaPayload.Info -> AwawaPayloadEvents.INFO.invoker().on(parsed)
                    is AwawaPayload.Chat -> AwawaPayloadEvents.CHAT.invoker().on(parsed)
                    is AwawaPayload.Raid -> AwawaPayloadEvents.RAID.invoker().on(parsed)
                    is AwawaPayload.ConnectionSuccess -> AwawaPayloadEvents.CONNECTION_SUCCESS.invoker().on(parsed)
                    is AwawaPayload.ConnectionFail -> AwawaPayloadEvents.CONNECTION_FAIL.invoker().on(parsed)
                    is AwawaPayload.Error -> AwawaPayloadEvents.ERROR.invoker().on(parsed)
                    is AwawaPayload.AnnieStatus -> AwawaPayloadEvents.ANNIE_STATUS.invoker().on(parsed)
                }
            } catch (e: Exception) {
                GregUtils.LOGGER.warn("GregBridge: listener failed on \"$payload\": $e")
            }
        }
    }

    private fun connectionHooks() {

        AwawaPayloadEvents.CONNECTION_SUCCESS.register {
            isLoggedIn = true
			availableChannels = it.availableChannels
            Utils.notifyChat(it.message)
            setStatus(Text.literal("CONNECTED").formatted(Formatting.GREEN))
            isConnecting = false
        }

        AwawaPayloadEvents.CONNECTION_FAIL.register {
            isLoggedIn = false
            Utils.notifyChat(it.message)
            isConnecting = false
        }

		AwawaPayloadEvents.INFO.register {
			Utils.notifyChat(it.message)
		}
    }
}
