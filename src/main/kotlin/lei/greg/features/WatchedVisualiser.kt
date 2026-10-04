package lei.greg.features

import lei.greg.Utils.notifyChat
import lei.greg.Utils.notifyTitle
import lei.greg.config.ConfigManager
import lei.greg.events.RaidFinishedEvent
import lei.greg.events.TnaBossEntered
import lei.greg.events.TnaWatchedBeam
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents

object WatchedVisualiser {
    var isInGreg: Boolean = false

    fun register() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { _, _ ->
            cleanup()
        }

        TnaWatchedBeam.EVENT.register { beamNr ->
            if(!isInGreg) return@register
            if (!ConfigManager.getBool("master toggle") || !ConfigManager.getBool("watched visualiser")) return@register

            if(ConfigManager.getBool("watched chat")) {
                notifyChat("§fBeam " + beamNr)
            }

            if(ConfigManager.getBool("watched title")) {
                if ( beamNr == 1)
                    notifyTitle(title = "§a.§7...§a.", stayTicks = 30)
                else if (beamNr == 2)
                    notifyTitle(title = "§7.§e.§7.§e.§7.", stayTicks = 30)
                else if (beamNr == 3)
                    notifyTitle(title = "§c!", stayTicks = 5, fadeOutTicks = 10)
            }
        }

        TnaBossEntered.EVENT.register {
//            notifyChat("Entered boss")
            isInGreg = true
        }

        RaidFinishedEvent.EVENT.register {
//            notifyChat("Raid finished")
            cleanup()
        }
    }

    private fun cleanup(){
        isInGreg = false
    }
}