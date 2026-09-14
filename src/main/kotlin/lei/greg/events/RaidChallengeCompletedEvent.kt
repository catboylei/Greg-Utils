package lei.greg.events

import lei.greg.GregUtils
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

fun interface RaidChallengeCompletedEvent {

    fun onChatMessage(message: String)

    companion object {
        val EVENT: Event<RaidChallengeCompletedEvent> = EventFactory.createArrayBacked(RaidChallengeCompletedEvent::class.java) { listeners ->
            RaidChallengeCompletedEvent { message -> listeners.forEach {
                it.onChatMessage(message)
                GregUtils.LOGGER.info("fired event \"RaidChallengeCompleted\"")
            } }
        }
    }
}