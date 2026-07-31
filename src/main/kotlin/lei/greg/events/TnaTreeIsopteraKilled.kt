package lei.catboyaddons.client.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

fun interface TnaTreeIsopteraKilled {

    fun onChatMessage()

    companion object {
        val EVENT: Event<TnaTreeIsopteraKilled> = EventFactory.createArrayBacked(TnaTreeIsopteraKilled::class.java) { listeners ->
            TnaTreeIsopteraKilled { listeners.forEach { it.onChatMessage() } }
        }
    }
}