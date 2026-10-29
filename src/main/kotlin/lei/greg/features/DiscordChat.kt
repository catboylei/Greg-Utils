package lei.greg.features

import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lei.greg.GregUtils
import lei.greg.GregUtils.PLAYER_UUID
import lei.greg.backend.Backend
import lei.greg.events.AwawaPayloadEvents
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

@Serializable data class OutgoingMessagePayload(val message: String, val channelName: String, @EncodeDefault val type: String = "chat")

object DiscordChat {

    fun register() {
		registerMessageCommand()

		AwawaPayloadEvents.CHAT.register {
			if (isEnabled()) Utils.discordMessage("chat", it.message, it.name, it.channelName)
		} 

		AwawaPayloadEvents.RAID.register {
			if (isEnabled()) Utils.discordMessage("raid", it.message, it.name)
		}
		
    }

    private fun isEnabled(): Boolean {
        return ConfigManager.getBool("fkl discord bridge") && ConfigManager.getBool("master toggle")
    }

    private fun registerMessageCommand() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommandManager.literal("d")
                    .then(ClientCommandManager.argument("channel", StringArgumentType.string())
                        .suggests { _, builder -> CommandSource.suggestMatching(Backend.availableChannels, builder) }
                        .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                            .executes { context ->
                                val msg = StringArgumentType.getString(context, "message")
                                val channel = StringArgumentType.getString(context, "channel")

                                val payload = OutgoingMessagePayload(msg, channel)
                                Backend.send(Json.encodeToString(OutgoingMessagePayload.serializer(), payload))

                                return@executes 1
                            }
                        )
                    )
            )
        }
    }
}
