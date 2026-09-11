package lei.greg

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.MinecraftClient
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.text.TextContent
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

object Utils {

    fun notifyChat(msg: String) {
        val client = MinecraftClient.getInstance()
        client.execute {
            client.inGameHud.chatHud.addMessage(
                Text.literal("[")
                    .formatted(Formatting.DARK_BLUE)
                    .append(Text.literal("Greg").formatted(Formatting.BLUE))
                    .append(Text.literal("]").formatted(Formatting.DARK_BLUE))
                    .append(Text.literal(" $msg").formatted(Formatting.RESET))
            );

        }
    }

    fun discordMessage(msg: String) {
        val client = MinecraftClient.getInstance()
        client.execute {
            client.inGameHud.chatHud.addMessage(Text.empty()
                .append(msg)
            )
        }
    }

}