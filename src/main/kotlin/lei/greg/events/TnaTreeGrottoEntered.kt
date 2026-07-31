package lei.catboyaddons.client.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

fun interface TnaTreeGrottoEntered {

    fun onChatMessage(message: String, player: String, grotto: String)

    companion object {
        val EVENT: Event<TnaTreeGrottoEntered> = EventFactory.createArrayBacked(TnaTreeGrottoEntered::class.java) { listeners ->
            TnaTreeGrottoEntered { message, player, grotto -> listeners.forEach { it.onChatMessage(message, player, grotto) } }
        }
    }
}