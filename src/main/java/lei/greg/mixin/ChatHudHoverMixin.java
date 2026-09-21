package lei.greg.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudHoverMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void renderMixin(DrawContext context, TextRenderer textRenderer, int currentTick, int mouseX, int mouseY, boolean interactable, boolean bl) {

        ChatHud self = (ChatHud) (Object) this;

        //Style style = self.getTextStyleAt(mouseX, mouseY);
        //GifHoverState.onChatHover(style);
    }
}
