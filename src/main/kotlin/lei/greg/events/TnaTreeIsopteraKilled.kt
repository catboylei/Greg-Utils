package lei.greg.events

import lei.greg.GregUtils
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

fun interface TnaTreeIsopteraKilled {

    fun onChatMessage()

    companion object {
        val EVENT: Event<TnaTreeIsopteraKilled> = EventFactory.createArrayBacked(TnaTreeIsopteraKilled::class.java) { listeners ->
            TnaTreeIsopteraKilled { listeners.forEach {
                it.onChatMessage()
                GregUtils.LOGGER.info("fired event \"TnaTreeIsopteraKilled\"")
            } }
        }
    }
}