package lei.greg.features

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lei.greg.GregUtils
import lei.greg.Utils
import lei.greg.backend.Backend
import lei.greg.backend.CitronsDefinitionOfAPlayer
import lei.greg.config.ConfigManager
import lei.greg.events.backend.AwawaPayloadEvents
import lei.greg.subScreens.AnnieRoles
import lei.greg.subScreens.AnnieSubScreen
import net.minecraft.client.MinecraftClient

@Serializable data class LeaveAnnieOutgoingPayload(@EncodeDefault val type: String = "leaveAnnie")
@Serializable data class SignUpOutgoingPayload(val role: String, val hasScrolls: Boolean, val canLead: Boolean, @EncodeDefault val type: String = "signUpForAnnie")
@Serializable data class RequestPartiesOutgoingPayload(@EncodeDefault val type: String = "getAnnieParties")
@Serializable data class RemoveAnnieOutgoingPayload(val name: String, @EncodeDefault val type: String = "removeFromAnnie")
@Serializable data class MovePlayerOutgoingPayload(val name: String, val party: Int, @EncodeDefault val type: String = "moveToParty")
@Serializable data class SwitchPlayersOutgoingPayload(val name1: String, val name2: String, @EncodeDefault val type: String = "switchParties")
@Serializable data class AddToAnnieOutgoingPayload(val name: String, val role: String, val hasScrolls: Boolean, val canLead: Boolean, @EncodeDefault val type: String = "addToAnnie")
data class Player(val name: String = "-", val hasScrolls: Boolean = false, val canLead: Boolean = false, val role: AnnieRoles = AnnieRoles.NONE) {
    constructor(dto: CitronsDefinitionOfAPlayer) : this(
        name = dto.name,
        hasScrolls = dto.hasScrolls != 0,
        canLead = dto.canLead != 0,
        role = AnnieRoles.fromBackend(dto.role),
    )
}

class Party(players: List<Player> = emptyList(), val number: Int, val active: Boolean = false) {

    private val realPlayers = players.filter { it.name != "-" }

    // takes the first 10 players and pads with empty if <10
    val players: List<Player> = players.sortedBy { it.role }.take(10).run {
        this + List((10 - this.size).coerceAtLeast(0)) { Player() }
    }

    // true if any player has scrolls
    val hasScrolls: Boolean = this.players.any { it.hasScrolls }
    val isFull: Boolean = realPlayers.size >= 10
    val isEmpty: Boolean = realPlayers.isEmpty()
}

object AnnieParties {

    fun register() {
        AwawaPayloadEvents.CONNECTION_SUCCESS.register {
            requestUpdate()
        }

        AwawaPayloadEvents.ANNIE_STATUS.register {
            isActive = it.isActive
            timestamp = it.timestamp
        }
    }

    var parties: List<Party> = padded(emptyList())
        private set
    var isActive = false
    var timestamp: Int? = null
    var playerParty: Int? = null

    fun getParty(number: Int?): Party? {
        return parties.firstOrNull { it.number == number }
    }
    fun isPlayerSignedUp(name: String): Boolean {
        return parties.any { party -> party.players.any { it.name.equals(name, ignoreCase = true) }  }
    }

    fun populate(newParties: List<Party>) { parties = padded(newParties) }
    fun joinAnnie(partyNumber: Int) {
        Utils.notifyToast("FKL Annie", "Sent a request to join party $partyNumber")

        if (ConfigManager.getString("annie role") == "NONE") {
            Utils.notifyToast("Role Missing", "You may select a role in settings!")
        }
        val payload = SignUpOutgoingPayload(
            ConfigManager.getString("annie role")!!,
            ConfigManager.getBool("have scrolls"),
            ConfigManager.getBool("can lead")
        )

        Backend.send(Json.encodeToString(SignUpOutgoingPayload.serializer(), payload))
    }
    fun leaveAnnie() {
        Utils.notifyToast("FKL Annie", "Sent a request to leave annie")

        Backend.send(Json.encodeToString(LeaveAnnieOutgoingPayload.serializer(), LeaveAnnieOutgoingPayload()))
    }
    fun requestUpdate() {
        Backend.send(Json.encodeToString(RequestPartiesOutgoingPayload.serializer(), RequestPartiesOutgoingPayload()))
    }

    fun addToAnnie(name: String, role: String, hasScrolls: Boolean, canLead: Boolean) {
        Utils.notifyToast("FKL Annie: Management", "Added $name to Annie")

        val payload = AddToAnnieOutgoingPayload(name, role, hasScrolls, canLead)
        Backend.send(Json.encodeToString(AddToAnnieOutgoingPayload.serializer(), payload))
    }
    fun removeFromAnnie(name: String) {
        Utils.notifyToast("FKL Annie: Management", "Removed $name from Annie")

        Backend.send(Json.encodeToString(RemoveAnnieOutgoingPayload.serializer(), RemoveAnnieOutgoingPayload(name)))
    }
    fun movePlayer(name: String, party: Int) {
        Utils.notifyToast("FKL Annie: Management", "Moved $name to party $party")

        Backend.send(Json.encodeToString(MovePlayerOutgoingPayload.serializer(), MovePlayerOutgoingPayload(name, party)))
    }
    fun switchPlayers(name1: String, name2: String) {
        Utils.notifyToast("FKL Annie: Management", "Switched players $name1 and $name2")

        Backend.send(Json.encodeToString(SwitchPlayersOutgoingPayload.serializer(), SwitchPlayersOutgoingPayload(name1, name2)))
    }

    // fills missing slots with default parties numbered by their position
    // also handles activating empty parties if all others are full
    private fun padded(list: List<Party>): List<Party> {
        playerParty = list.firstOrNull { party ->
            party.players.any { it.name.equals(GregUtils.PLAYER_NAME, ignoreCase = true) }
        }?.number

        val byNumber = list.associateBy { it.number }
        val count = maxOf(4, list.maxOfOrNull { it.number } ?: 0)

        val base = (1..count).map { n -> byNumber[n] ?: Party(number = n) }
        val firstEmpty = base.firstOrNull { it.isEmpty }

        val activateNumber = firstEmpty?.number?.takeIf {
            base.filter { p -> !p.isEmpty }.all { p -> p.isFull }
        }

        return base.map { p ->
            val shouldBeActive = p.active || !p.isEmpty || p.number == activateNumber
            if (shouldBeActive == p.active) p else Party(p.players, p.number, active = shouldBeActive)
        }
    }
}