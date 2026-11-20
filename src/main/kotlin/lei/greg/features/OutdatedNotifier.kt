package lei.greg.features

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lei.greg.GregUtils
import lei.greg.Utils
import lei.greg.backend.Backend
import lei.greg.events.backend.AwawaPayloadEvents
import lei.greg.utils.NerdFonts
import net.minecraft.client.gui.Click
import net.minecraft.text.ClickEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import java.net.URI

@Serializable data class OutgoingVersionsPayload(@EncodeDefault val type: String = "getVersions")

object OutdatedNotifier {
    var latestVer = ""
    var latestVerDownload = ""

    var latestNightly = ""
    var latestNightlyDownload = ""

    val notifyMessage: MutableText by lazy {
        Text.empty().apply {
            // TODO: fix wrapping on this
            append(Text.literal("Update Found! ").styled { it.withItalic(true).withUnderline(true).withClickEvent(
                ClickEvent.OpenUrl(URI.create(latestVerDownload)) // TODO: replace this with opening the real gui
            )}.withColor(0xAAAAAA))
            append(Text.literal(GregUtils.version + " ").withColor(0xFF5555))
            append(NerdFonts.icon(0xf432).withColor(0xAAAAAA))
            append(Text.literal(" " + if (GregUtils.isNightly) latestNightly else latestVer).withColor(0x55FF55))
        }
    }

    fun register() {
        AwawaPayloadEvents.VERSIONS_UPDATE.register {
            print("updating versions")

            latestVer = it.stable
            latestVerDownload = it.stable_link
            latestNightly = it.nightly
            latestNightlyDownload = it.nightly_link

            if (GregUtils.version != latestVer && GregUtils.version != latestNightly) {
                Utils.notifyChat(notifyMessage, true)
            }
        }

        AwawaPayloadEvents.CONNECTION_SUCCESS.register {
            requestVersions()
        }
    }

    private fun requestVersions() {
        Backend.send(Json.encodeToString(OutgoingVersionsPayload.serializer(), OutgoingVersionsPayload()))
    }

    // TODO: open gui with maybe changelog and some buttons here, plus command to open it
}