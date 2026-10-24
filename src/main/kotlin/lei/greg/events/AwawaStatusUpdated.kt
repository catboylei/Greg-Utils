package lei.greg.events

import lei.greg.GregUtils
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.text.Text

fun interface AwawaStatusUpdated {

    fun onUpdate(status: Text)

    companion object {
        val EVENT: Event<AwawaStatusUpdated> = EventFactory.createArrayBacked(AwawaStatusUpdated::class.java) { listeners ->
            AwawaStatusUpdated { message ->
                GregUtils.LOGGER.info("fired event \"AwawaStatusUpdated\" with value $message")
                listeners.forEach {
                    it.onUpdate(message)
                }
            }
        }
    }
}