package lei.greg.events

import lei.greg.GregUtils
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

fun interface RaidFinishedEvent {

    fun onChatMessage()

    companion object {
        val EVENT: Event<RaidFinishedEvent> = EventFactory.createArrayBacked(RaidFinishedEvent::class.java) { listeners ->
            RaidFinishedEvent {
                GregUtils.LOGGER.info("Raid finished")
                listeners.forEach { it.onChatMessage() }
            }
        }
    }
}