package lei.greg.features

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lei.greg.GregUtils
import lei.greg.Utils
import lei.greg.backend.Backend
import lei.greg.events.backend.AwawaPayloadEvents
import lei.greg.utils.NerdFonts
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Serializable data class OutgoingVersionsPayload(@EncodeDefault val type: String = "getVersions")

object OutdatedNotifier {
    var latestVer = ""
    var latestVerDownload = ""

    var latestNightly = ""
    var latestNightlyDownload = ""

    val notifyMessage: MutableText by lazy {
        Text.empty().apply {
            append(NerdFonts.icon(0xf06b0).withColor(0xAAAAAA).styled {
                it.withClickEvent(ClickEvent.RunCommand("gregutils update"))
                    .withHoverEvent(HoverEvent.ShowText(Text.literal("Download and install update")))
            })
            append(Text.literal(" Update Available! ").withColor(0xAAAAAA))
            append(Text.literal(GregUtils.version.removeSuffix("+1.21.11") + " ").withColor(0xFF5555))
            append(NerdFonts.icon(0xf432).withColor(0xAAAAAA))
            append(Text.literal(" " + (if (GregUtils.isNightly) latestNightly else latestVer) + " ").withColor(0x55FF55))
            append(NerdFonts.icon(0xf465).withColor(0xAAAAAA).styled {
                it.withClickEvent(ClickEvent.OpenUrl(URI.create(latestVerDownload))) // TODO: open tag link here instead
                    .withHoverEvent(HoverEvent.ShowText(Text.literal("Open in browser")))
            })
        }
    }

    fun register() {
        AwawaPayloadEvents.VERSIONS_UPDATE.register {
            print("updating versions")

            latestVer = it.stable.removeSuffix("+1.21.11")
            latestVerDownload = it.stable_link.removeSuffix("+1.21.11")
            latestNightly = it.nightly.removeSuffix("+1.21.11")
            latestNightlyDownload = it.nightly_link.removeSuffix("+1.21.11")
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            if (GregUtils.version != latestVer && GregUtils.version != latestNightly && Backend.isLoggedIn) {
                Utils.notifyChat(notifyMessage, true)
            }
        }

        AwawaPayloadEvents.CONNECTION_SUCCESS.register {
            requestVersions()
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("gregutils").then(
                    literal("update").executes {
                        selfUpdate(
                            if (GregUtils.isNightly) latestNightlyDownload else latestVerDownload,
                            "greg-utils-${if (GregUtils.isNightly) latestNightly else latestVer}"
                        ); 1
                    }
                )
            )
        }
    }

    private fun requestVersions() {
        Backend.send(Json.encodeToString(OutgoingVersionsPayload.serializer(), OutgoingVersionsPayload()))
    }

    /**
     * Creates a new thread that handles downloading `fileName` at `url`, moving it to the mods folder
     * and finally adding a shutdown hook to delete the old mod jar
     *
     * @param url Download link
     * @param fileName Name of file to be downloaded
     */
    private val updating = java.util.concurrent.atomic.AtomicBoolean(false)
    fun selfUpdate(url: String, fileName: String) {
        if (!updating.compareAndSet(false, true)) return // avoid multiple updates at once

        Thread {
            val loader = FabricLoader.getInstance()
            val modsDir = loader.gameDir.resolve("mods")
            val tmp = modsDir.resolve("$fileName.part")   // not .jar, so fabric ignores it
            val target = modsDir.resolve(fileName)

            try {
                Utils.notifyChat("Updating, this may take a moment...")

                val req = HttpRequest.newBuilder(URI.create(url)).build()
                val client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build()
                val response = client.send(req, HttpResponse.BodyHandlers.ofFile(tmp))

                // notify and return if unexpected html response
                if (response.statusCode() != 200) {
                    Utils.notifyChat("Updater: failed to fetch updated file (try again later)")
                    Files.deleteIfExists(tmp); updating.set(false); return@Thread
                }
                // empty file
                if (!Files.isRegularFile(tmp) || Files.size(tmp) == 0L) {
                    Utils.notifyChat("Updater: download corrupted (check your internet connection)")
                    Files.deleteIfExists(tmp); updating.set(false); return@Thread
                }
                // TODO: check SHA-256 against expected release as well

                val oldJar = loader.getModContainer(GregUtils.MOD_ID).get().origin.paths.first()
                if (oldJar == target) {
                    Utils.notifyChat("Updater: already on latest (what are we doing)")
                    Files.deleteIfExists(tmp); updating.set(false); return@Thread
                }

                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING)

                if (Files.isRegularFile(oldJar)) { // skip if in devenv (directory)
                    Runtime.getRuntime().addShutdownHook(Thread {
                        try {
                            Files.delete(oldJar)
                        } catch (e: Exception) {
                            // this is exclusively a windows issue btw
                            if (System.getProperty("os.name").lowercase().contains("win")) {
                                ProcessBuilder("cmd", "/c", "ping -n 8 127.0.0.1 >nul & del /f \"$oldJar\"").start()
                            }
                            // oops
                        }
                    })
                } else {
                    Utils.notifyChat("nice dev environment bbg")
                }

                Utils.notifyChat("Update Success, Restart your game to apply!")
            } catch (e: Exception) {
                Utils.notifyChat("Updater: update failed (${e.javaClass.simpleName})")
                runCatching { Files.deleteIfExists(tmp) }
                updating.set(false)
            }
        }.start()
    }
}