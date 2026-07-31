package lei.greg.mixin;

import lei.catboyaddons.client.events.RaidChallengeCompletedEvent;
import lei.catboyaddons.client.events.TnaTreeEntered;
import lei.catboyaddons.client.events.TnaTreeGrottoEntered;
import lei.catboyaddons.client.events.TnaTreeIsopteraKilled;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ClientPlayNetworkHandler.class)
public class MessageHandlerMixin {

    @Unique private static final long DEDUPE_WINDOW_MS = 500;

    @Unique private static final Pattern GROTTO_PATTERN = Pattern.compile("(.+) has entered the (.+) Grotto");
    @Unique private static final Pattern TREE_PATTERN = Pattern.compile("§5The Interdimensional Isoptera is in the (.+) Grotto");

    @Unique private final Debouncer grottoDebounce = new Debouncer();
    @Unique private final Debouncer treeDebounce = new Debouncer();
    @Unique private final Debouncer isoDebounce = new Debouncer();

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        String content = packet.content().getString();

        if (content.equals("Challenge Completed")) {
            RaidChallengeCompletedEvent.Companion.getEVENT().invoker().onChatMessage(content);
            return;
        }

        if (content.equals("§d[+1 Isoptera Heart]") && isoDebounce.canFire()) {
            TnaTreeIsopteraKilled.Companion.getEVENT().invoker().onChatMessage();
            return;
        }

        Matcher grottoMatcher = GROTTO_PATTERN.matcher(content);
        if (grottoMatcher.matches() && grottoDebounce.canFire()) {
            TnaTreeGrottoEntered.Companion.getEVENT().invoker()
                    .onChatMessage(content, grottoMatcher.group(1), grottoMatcher.group(2));
            return;
        }

        Matcher treeMatcher = TREE_PATTERN.matcher(content);
        if (treeMatcher.matches() && treeDebounce.canFire()) {
            TnaTreeEntered.Companion.getEVENT().invoker().onChatMessage(treeMatcher.group(1));
        }
    }

    @Unique
    private static final class Debouncer {
        private long last = 0;
        boolean canFire() {
            long now = System.currentTimeMillis();
            if (now - last <= DEDUPE_WINDOW_MS) return false;
            last = now;
            return true;
        }
    }

}