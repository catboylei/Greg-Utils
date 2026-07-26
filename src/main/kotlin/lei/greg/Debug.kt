package lei.greg

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import lei.greg.highlights.Highlights
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.Vec3ArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos


object Debug {
    fun register() {
        registerHighlightCommand()
    }

    private fun registerHighlightCommand() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                CommandManager.literal("highlight")
                    .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                        .then(CommandManager.argument("state", BoolArgumentType.bool())
                            .executes { context -> highlightCommand(context) }
                        )
                    )
            )
        }
    }

    private fun highlightCommand(context: CommandContext<ServerCommandSource>): Int {
        val pos = Vec3ArgumentType.getVec3(context, "pos")
        val state = BoolArgumentType.getBool(context, "state")
        context.source.sendFeedback({ Text.literal("Coords: ${pos.x}, ${pos.y}, ${pos.z} State: $state") }, false)

        // it naturally offsets it by 1 in the z axis and i genuinely have not a clue in the world as to why
        val blockPos = BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt() - 1)
        val inList = blockPos in Highlights.getBlockCoords()

        if (state && !inList) {
            Highlights.addBlockCoords(blockPos)
        } else if (!state && inList) {
            Highlights.removeBlockCoords(blockPos)
        }

        context.source.sendFeedback({Text.literal("MEOW MEOW = ${Highlights.getBlockCoords()}")}, false)

        return 1
    }
}