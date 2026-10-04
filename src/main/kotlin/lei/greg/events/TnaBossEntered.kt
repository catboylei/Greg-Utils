package lei.greg.events

import lei.greg.GregUtils
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

fun interface TnaBossEntered {

    fun onChatMessage()

    companion object {
        val EVENT: Event<TnaBossEntered> = EventFactory.createArrayBacked(TnaBossEntered::class.java) { listeners ->
            TnaBossEntered {
                listeners.forEach { it.onChatMessage() }
            }
        }
    }
}