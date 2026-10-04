package lei.greg.mixin;

import RandomUtils.Debouncer;
import lei.greg.events.*;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class SoundHandlerMixin {
    @Unique private final Debouncer watchedBeamDebounce = new Debouncer();


    @Inject(method = "onPlaySound", at = @At("HEAD"))
    private void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        SoundEvent sound = packet.getSound().value();
        Identifier soundId = sound.id();
        float pitch = packet.getPitch();

        if (soundId.toString().equals("minecraft:entity.evoker.prepare_summon") && watchedBeamDebounce.canFire()){
            if (checkPitch(pitch, 1.0f)){
                TnaWatchedBeam.Companion.getEVENT().invoker().onBeam(1);
            }
            else if (checkPitch(pitch, 1.5f)){
                TnaWatchedBeam.Companion.getEVENT().invoker().onBeam(2);
            }
        }
        if(soundId.toString().equals("minecraft:item.trident.thunder") && watchedBeamDebounce.canFire()){
            if (checkPitch(pitch, 0.8f)) {
                TnaWatchedBeam.Companion.getEVENT().invoker().onBeam(3);
            }
        }
    }

    // maybe this wouldnt be needed if i used doubles everywhere i think but i like the word float so ill keep using it
    private boolean checkPitch(float pitch, float value){
        return Math.abs(pitch - value) < 0.01f;
    }
}