package lei.greg.events.backend

import lei.greg.backend.AwawaPayload
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

object AwawaPayloadEvents { 
    fun interface Listener<T : AwawaPayload> { fun on(payload: T) }

	@Suppress("UNCHECKED_CAST")
    private fun <T : AwawaPayload> event(): Event<Listener<T>> =
        EventFactory.createArrayBacked(Listener::class.java) { listeners ->
            Listener<T> { p -> for (l in listeners) (l as Listener<T>).on(p) }
        } as Event<Listener<T>>
	
    @JvmField val INFO = event<AwawaPayload.Info>()
    @JvmField val CHAT = event<AwawaPayload.Chat>()
    @JvmField val RAID = event<AwawaPayload.Raid>()
	@JvmField val CONNECTION_SUCCESS = event<AwawaPayload.ConnectionSuccess>()
    @JvmField val CONNECTION_FAIL = event<AwawaPayload.ConnectionFail>()
    @JvmField val ERROR = event<AwawaPayload.Error>()
    @JvmField val ANNIE_STATUS = event<AwawaPayload.AnnieStatus>()
    @JvmField val ANNIE_PARTIES_UPDATE = event<AwawaPayload.AnniePartyList>()
}
