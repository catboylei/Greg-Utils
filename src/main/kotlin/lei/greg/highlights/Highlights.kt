package lei.greg.highlights

import net.minecraft.util.math.BlockPos

object Highlights {
    fun register() {
        registerBlockHighlights()
    }

    private val BlockCoords = mutableListOf<BlockPos>()

    fun getBlockCoords(): List<BlockPos> { return BlockCoords.toList() }
    fun addBlockCoords(blockPos: BlockPos) { BlockCoords.add(blockPos) }
    fun removeBlockCoords(blockPos: BlockPos) { BlockCoords.remove(blockPos) }
}