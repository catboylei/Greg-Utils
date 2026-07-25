package lei.greg.highlights

import lei.greg.mixin.WorldRendererAccessor
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayers
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random

fun registerBlockHighlights() {
    WorldRenderEvents.AFTER_ENTITIES.register(WorldRenderEvents.AfterEntities { context ->
        val entries: List<BlockPos> = Highlights.getBlockCoords()

        for (pos in entries) {
            highlightBlock(pos, context)
        }
    })
}

private fun highlightBlock(pos: BlockPos, context: WorldRenderContext) {

    // cast as my accessor mixin because fabric is stupid and hates me personally
    val accessor = MinecraftClient.getInstance().worldRenderer as WorldRendererAccessor
    val consumers = accessor.bufferBuilders.outlineVertexConsumers

    val client = MinecraftClient.getInstance()
    val world = client.world ?: return
    val state = world.getBlockState(pos)
    if (state.isAir) return

    val camPos = client.gameRenderer.camera.cameraPos
    val matrices = context.matrices()

    // todo constants
    consumers.setColor(0xFFCEFF)

    matrices.push()
    matrices.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z)
    client.blockRenderManager.renderBlock(
        state, pos, world, matrices,
        consumers.getBuffer(RenderLayers.solid()),
        false,
        client.blockRenderManager.getModel(state).getParts(Random.create(42L))
    )
    matrices.pop()
}