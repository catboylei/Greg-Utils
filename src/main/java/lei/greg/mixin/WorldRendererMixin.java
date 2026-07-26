package lei.greg.mixin;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.WorldRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// this thing makes the highlight thingy work properly
// dont question this nonsense
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Final @Shadow
    private WorldRenderState worldRenderState;

    @Inject(method = "renderMain", at = @At("HEAD"))
    private void onRenderMainHead(CallbackInfo ci) { this.worldRenderState.hasOutline = true; }
}