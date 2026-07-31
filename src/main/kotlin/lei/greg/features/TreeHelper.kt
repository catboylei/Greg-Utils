package lei.greg.features

import lei.catboyaddons.client.events.RaidChallengeCompletedEvent
import lei.catboyaddons.client.events.TnaTreeEntered
import lei.catboyaddons.client.events.TnaTreeGrottoEntered
import lei.greg.config.ConfigManager
import lei.greg.data.TreeGrotto
import lei.greg.data.TreeGrottoDoor
import lei.greg.highlights.Highlights
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents

object TreeHelper {

    var isInTree: Boolean = false
    var currentRoom: TreeGrotto? = null
    var isoRoom: TreeGrotto? = null
    var pathToTake: MutableList<TreeGrottoDoor> = mutableListOf()
    var highlightedDoor: TreeGrottoDoor? = null

    fun register() {
        TnaTreeGrottoEntered.EVENT.register { _, _, grotto ->
            if (!ConfigManager.getBool("master toggle") || !ConfigManager.getBool("tree route highlight")) return@register
            if ( pathToTake.isEmpty() || !isInTree ) return@register

            currentRoom = TreeGrotto.valueOf(grotto)
            pathToTake.removeFirst()

            highlightedDoor?.let { unHighlightDoor(it) }
            if (pathToTake.isNotEmpty()) {
                highlightedDoor = pathToTake.first()
                if (highlightedDoor!!.grotto != currentRoom) {
                    cleanup()
                    //notifyChat("wrong door, ur on ur own gng") // todo chat notification util
                    return@register // shit yourself if take the wrong door
                }
                highlightDoor(highlightedDoor!!, ConfigManager.getString("highlight color")!!.removePrefix("#").hexToInt())
                //notifyChat("highlighting next door, remaining: ${pathToTake.size}")
            } else {
                cleanup()
            }
        }

        TnaTreeEntered.EVENT.register { grotto ->
            cleanup()
            if (!ConfigManager.getBool("master toggle") || !ConfigManager.getBool("tree route highlight")) return@register

            isInTree = true
            currentRoom = TreeGrotto.Exit
            isoRoom = TreeGrotto.valueOf(grotto)

            computeFullPath(isoRoom!!)

            //notifyChat("calculating route for $grotto")

            highlightedDoor = pathToTake.first()
            highlightDoor(highlightedDoor!!, 0xFFCEFF)
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> cleanup() }
        RaidChallengeCompletedEvent.EVENT.register { _ -> cleanup() }
    }

    private fun highlightDoor(door: TreeGrottoDoor, color: Int) {
        for (blockpos in door.blocks) {
            Highlights.addBlockCoords(blockpos)
        }
    }

    private fun unHighlightDoor(door: TreeGrottoDoor) {
        for (blockpos in door.blocks) {
            Highlights.removeBlockCoords(blockpos)
        }
    }

    private fun cleanup() {
        isoRoom = null
        currentRoom = null
        isInTree = false
        highlightedDoor?.let { unHighlightDoor(it) }
        pathToTake = mutableListOf()
    }

    private fun computeFullPath(grotto: TreeGrotto) {
        pathToTake = when (grotto) {
            TreeGrotto.Gray -> mutableListOf(TreeGrottoDoor.Exit_1, TreeGrottoDoor.Grey_1)
            TreeGrotto.Blue -> if (ConfigManager.getBool("avoid high exit")) {
                mutableListOf(TreeGrottoDoor.Exit_1, TreeGrottoDoor.Grey_2, TreeGrottoDoor.Blue_1)
            } else {
                mutableListOf(TreeGrottoDoor.Exit_2, TreeGrottoDoor.Blue_1)
            }
            TreeGrotto.Orange -> if (ConfigManager.getBool("avoid high exit")) {
                mutableListOf(TreeGrottoDoor.Exit_1, TreeGrottoDoor.Grey_2, TreeGrottoDoor.Blue_4, TreeGrottoDoor.Orange_1, TreeGrottoDoor.Blue_1)
            } else {
                mutableListOf(TreeGrottoDoor.Exit_2, TreeGrottoDoor.Blue_4, TreeGrottoDoor.Orange_1, TreeGrottoDoor.Blue_1)
            }
            TreeGrotto.Black -> mutableListOf(TreeGrottoDoor.Exit_1, TreeGrottoDoor.Grey_3, TreeGrottoDoor.Black_3, TreeGrottoDoor.Grey_1)
            TreeGrotto.White -> mutableListOf(TreeGrottoDoor.Exit_1, TreeGrottoDoor.Grey_3, TreeGrottoDoor.Black_2, TreeGrottoDoor.White_3, TreeGrottoDoor.Black_3, TreeGrottoDoor.Grey_1)
            else -> mutableListOf()
        }
    }

}