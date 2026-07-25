package lei.greg.mixin;

import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// accessor for the highlight blocks feature
// the event doesnt provide it :/
@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {
    @Accessor("bufferBuilders")
    BufferBuilderStorage getBufferBuilders();
}