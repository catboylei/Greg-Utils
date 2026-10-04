package lei.greg.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

fun interface TnaWatchedBeam {

    // idk how to name it but it prolly shouldnt stay named "onchatmessage"
    fun onBeam(beamNr: Int)

    companion object {
        val EVENT: Event<TnaWatchedBeam> = EventFactory.createArrayBacked(TnaWatchedBeam::class.java) { listeners ->
            TnaWatchedBeam { beamNr -> listeners.forEach {
                it.onBeam(beamNr)
            } }
        }
    }
}