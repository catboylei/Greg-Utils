package lei.greg.highlights

import lei.greg.mixin.WorldRendererAccessor
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayers
import net.minecraft.text.Text
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.random.Random
import net.minecraft.world.RaycastContext

fun registerBlockHighlights() {
    WorldRenderEvents.AFTER_ENTITIES.register(WorldRenderEvents.AfterEntities { context ->
        val entries: List<BlockPos> = Highlights.getBlockCoords()

        for (pos in entries) {
            highlightBlock(pos, context)
        }
    })
}

private fun highlightBlock(pos: BlockPos, context: WorldRenderContext) {

    // explode if block is not visible
    // todo tie this to a compliance constant
    if (!isBlockVisible(pos)) return

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

    client.player!!.sendMessage(Text.literal("is visible : ${isBlockVisible(pos)}"), false)
}

private fun isBlockVisible(pos: BlockPos): Boolean {
    val client = MinecraftClient.getInstance()
    val world = client.world ?: return false
    val camera = client.gameRenderer.camera
    val camPos = camera.cameraPos

    listOf(
        Vec3d(pos.x + 0.5, pos.y + 1.0, pos.z + 0.5),
        Vec3d(pos.x + 0.0, pos.y + 0.5, pos.z + 0.5),
        Vec3d(pos.x + 1.0, pos.y + 0.5, pos.z + 0.5),
        Vec3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.0),
        Vec3d(pos.x + 0.5, pos.y + 0.5, pos.z + 1.0),
    ).iterator().forEach {
        val context = RaycastContext(
            camPos,
            it,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            client.player
        )
        val result = world.raycast(context)
        if (result.type == HitResult.Type.MISS || result.blockPos == pos) {
            return true
        }
    }

    return false
}