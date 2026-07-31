package lei.catboyaddons.client.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

fun interface TnaTreeEntered {

    fun onChatMessage(grotto: String)

    companion object {
        val EVENT: Event<TnaTreeEntered> = EventFactory.createArrayBacked(TnaTreeEntered::class.java) { listeners ->
            TnaTreeEntered {grotto -> listeners.forEach { it.onChatMessage(grotto) } }
        }
    }
}