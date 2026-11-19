package lei.greg.features

import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lei.greg.GregUtils
import lei.greg.backend.Backend
import lei.greg.events.backend.AwawaPayloadEvents
import lei.greg.Utils
import lei.greg.config.ConfigManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.command.CommandSource
import net.minecraft.text.Text

@Serializable data class OutgoingMessagePayload(val message: String, val channelName: String, @EncodeDefault val type: String = "chat")

object DiscordChat {
    @Volatile var lastWasDiscord = false
        private set
    private val pill = Utils.discordPill.string

    fun register() {
        registerMessageCommand()

        AwawaPayloadEvents.CHAT.register {
            if (isEnabled()) {
                val previous = lastWasDiscord
                lastWasDiscord = true
                if (it.repliedMessage != null) {
                    // this also makes an icon-less message so it looks more natural if it resets the icon
                    Utils.discordReplyMessage(it.repliedMessage[1], it.repliedMessage[0])
                    lastWasDiscord = false
                }
                Utils.discordMessage("chat", it.message, it.name, it.channelName, previous)
            }
        }

        // this "raid" type is icon-less so resets lastWasDiscord
        AwawaPayloadEvents.RAID.register {
            if (isEnabled()) {
                Utils.discordMessage("raid", it.message, it.name)
                lastWasDiscord = false
            }
        }

        // discord messages are appended with chatHud.addMessage() and do not trigger this event
        ClientReceiveMessageEvents.GAME.register { _, overlay -> if (!overlay) lastWasDiscord = false }
        ClientReceiveMessageEvents.CHAT.register { _, _, _, _, _ -> lastWasDiscord = false }
    }


    private fun isEnabled(): Boolean {
        return ConfigManager.getBool("fkl discord bridge") && ConfigManager.getBool("master toggle")
    }

    private fun Text.isDiscord(): Boolean = string.contains(pill)

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
