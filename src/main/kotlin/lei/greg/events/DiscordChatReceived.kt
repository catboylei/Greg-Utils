package lei.greg.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

data class DiscordMessage(
	val author: String,
	val content: String,
)

fun interface DiscordChatReceived {

    fun onMessage(message: DiscordMessage)

    companion object {
        val EVENT: Event<DiscordChatReceived> = EventFactory.createArrayBacked(DiscordChatReceived::class.java) { listeners ->
            DiscordChatReceived {message -> listeners.forEach { it.onMessage(message) } }
        }
    }
}
