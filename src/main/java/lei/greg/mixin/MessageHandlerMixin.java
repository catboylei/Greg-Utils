package lei.greg.mixin;

import lei.greg.GregUtils;
import RandomUtils.Debouncer;
import lei.greg.events.*;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// listens to chat and fires events associated to some messages
@Mixin(ClientPlayNetworkHandler.class)
public class MessageHandlerMixin {
    @Unique private static final Pattern GROTTO_PATTERN = Pattern.compile("(.+) has entered the (.+) Grotto");
    @Unique private static final Pattern TREE_PATTERN = Pattern.compile("§5The Interdimensional Isoptera is in the (.+) Grotto");

    @Unique private final Debouncer grottoDebounce = new Debouncer();
    @Unique private final Debouncer treeDebounce = new Debouncer();
    @Unique private final Debouncer isoDebounce = new Debouncer();

    @Unique private final Debouncer tnaBossDebounce = new Debouncer();

    @Unique private final Debouncer raidFinishedDebounce = new Debouncer();

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

        if (content.equals("§7\uDB00\uDC6DRaid Completed!")  && raidFinishedDebounce.canFire()){
            RaidFinishedEvent.Companion.getEVENT().invoker().onChatMessage();
        }
        if (content.equals("§7§oDoomed to mutation, desolation, darkness, and despair...from that which bears no name.")  && tnaBossDebounce.canFire()){
            TnaBossEntered.Companion.getEVENT().invoker().onChatMessage();
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

    /// use this to get the actual usable string that u can slap in .equals and it just works
    /// note that you might (will) have to remove duplicated backslashes because when pasting
    /// in quotes intellij turns \ into \\
    /// also if intellij suggests it you can replace the mental illnesses with "normal"
    /// characters like §
    private String getActualMessage(String message) {
        StringBuilder actualMessage = new StringBuilder();
        for (char character : message.toCharArray()){
            if (character >= 0x20 && character <= 0x7e) // normal characters
                actualMessage.append(character);
            else
                actualMessage.append(String.format("\\u%04x", (int) character)); // mental illnesses
        }
        return actualMessage.toString();
    }
}