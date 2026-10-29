package lei.greg.features

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lei.greg.Utils
import lei.greg.backend.Backend
import lei.greg.config.ConfigManager
import lei.greg.events.AwawaPayloadEvents
import lei.greg.subScreens.AnnieRoles

@Serializable data class SignUpOutgoingPayload(val role: String, val hasScrolls: Boolean, val canLead: Boolean, @EncodeDefault val type: String = "signUpForAnnie")
@Serializable data class RequestPartiesOutgoingPayload(@EncodeDefault val type: String = "getAnnieParties")
data class Player(val name: String = "-", val hasScrolls: Boolean = false, val canLead: Boolean = false, val role: AnnieRoles = AnnieRoles.NONE)

class Party(players: List<Player> = emptyList(), val number: Int, val active: Boolean = false) {

    // takes the first 10 players and pads with empty if <10
    val players: List<Player> = players.sortedBy { it.role }.take(10).run {
        this + List((10 - this.size).coerceAtLeast(0)) { Player() }
    }

    // true if any player has scrolls
    val hasScrolls: Boolean = this.players.any { it.hasScrolls }
    val isFull: Boolean = players.lastIndex >= 9
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
    fun requestUpdate() {
        Backend.send(Json.encodeToString(RequestPartiesOutgoingPayload.serializer(), RequestPartiesOutgoingPayload()))
    }

    // fills missing slots with default parties numbered by their position
    private fun padded(list: List<Party>): List<Party> {
        val byNumber = list.associateBy { it.number }
        val count = maxOf(4, list.maxOfOrNull { it.number } ?: 0)
        return (1..count).map { n -> byNumber[n] ?: Party(number = n) }
    }

    // values taken from the 2026/10/26 fkl annie
    fun populateWithTest() {
        populate(listOf(
            Party(
                listOf(
                    Player(name = "Crixx007", hasScrolls = true, canLead = true, role = AnnieRoles.HEAL),
                    Player(name = "Cryotheum", hasScrolls = false, canLead = false, role = AnnieRoles.HEAL),
                    Player(name = "AlfieCrafts", hasScrolls = false, canLead = true, role = AnnieRoles.DPS),
                    Player(name = "TheWalrusMaster", hasScrolls = false, canLead = false, role = AnnieRoles.DPS),
                    Player(name = "ItsFlyingCactus", hasScrolls = false, canLead = false, role = AnnieRoles.DPS),
                    Player(name = "Pyrotheum", hasScrolls = false, canLead = true, role = AnnieRoles.TANK),
                    Player(name = "TheseusTheBoss", hasScrolls = false, canLead = true, role = AnnieRoles.DPS),
                    Player(name = "Yqdy", hasScrolls = false, canLead = false, role = AnnieRoles.HEAL),
                    Player(name = "Magafaof", hasScrolls = false, canLead = false, role = AnnieRoles.DPS),
                    Player(name = "GamblingManiac", hasScrolls = false, canLead = false, role = AnnieRoles.DPS),
                ),
                1,
                true
            ),
            Party(
                listOf(
                    Player(name = "Icyglowleaf", hasScrolls = true, canLead = false, role = AnnieRoles.HEAL),
                    Player(name = "bigmoneyfiber", hasScrolls = false, canLead = false, role = AnnieRoles.DPS),
                    Player(name = "hrrw", hasScrolls = false, canLead = false, role = AnnieRoles.HEAL),
                    Player(name = "CreampaiPham", hasScrolls = false, canLead = false, role = AnnieRoles.DPS),
                    Player(name = "Ams_017", hasScrolls = false, canLead = false, role = AnnieRoles.TANK),
                    Player(name = "itsyunaya", hasScrolls = false, canLead = false, role = AnnieRoles.DPS),
                    Player(name = "Siel_oschen", hasScrolls = false, canLead = false, role = AnnieRoles.DPS),
                    Player(name = "Citr_n", hasScrolls = false, canLead = false, role = AnnieRoles.DPS),
                    Player(name = "elolunis", hasScrolls = false, canLead = false, role = AnnieRoles.DPS),
                ),
                2,
                true
            ),
        ))
    }
}