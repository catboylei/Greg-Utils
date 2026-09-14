package lei.greg

import lei.greg.config.ConfigManager
import net.minecraft.client.MinecraftClient
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object Utils {

    // stores the unicode values for various things in the wynncraft texture pack
    // many warnings but none of them matter !
    object characters {
        object spacing {
            val CHAR_JOINER = Text.literal("\u2064")
            val CHAR_OVERLAPPER = Text.literal("\uE012")
            val CHAR_OVERLAPPER_SHORT = Text.literal("\uE071")
            val SPACE = Text.literal("\uE013")
        }

        object banner {
            val START = Text.literal("\uE010")
            val BODY = Text.literal("\uE00F")
            val BODY_SHORT = Text.literal("\uE070")
            val END = Text.literal("\uE011")
        }

        object five {
            val A = Text.literal("\uE040")
            val B = Text.literal("\uE041")
            val C = Text.literal("\uE042")
            val D = Text.literal("\uE043")
            val E = Text.literal("\uE044")
            val F = Text.literal("\uE045")
            val G = Text.literal("\uE046")
            val H = Text.literal("\uE047")
            val I = Text.literal("\uE048")
            val J = Text.literal("\uE049")
            val K = Text.literal("\uE04A")
            val L = Text.literal("\uE04B")
            val M = Text.literal("\uE04C")
            val N = Text.literal("\uE04D")
            val O = Text.literal("\uE04E")
            val P = Text.literal("\uE04F")
            val Q = Text.literal("\uE050")
            val R = Text.literal("\uE051")
            val S = Text.literal("\uE052")
            val T = Text.literal("\uE053")
            val U = Text.literal("\uE054")
            val V = Text.literal("\uE055")
            val W = Text.literal("\uE056")
            val X = Text.literal("\uE057")
            val Y = Text.literal("\uE058")
            val Z = Text.literal("\uE059")

            val QUESTION_MARK = Text.literal("\uE05A")
            val SQUARE_BRACKET_OPEN = Text.literal("\uE05B")
            val SQUARE_BRACKET_CLOSE = Text.literal("\uE05C")
            val BACKSLASH = Text.literal("\uE05D")
            val PERCENT = Text.literal("\uE05E")
            val AND = Text.literal("\uE05F")
            val EXCLAMATION = Text.literal("\uE06A")
            val BRACKET_OPEN = Text.literal("\uE06B")
            val BRACKET_CLOSE = Text.literal("\uE06C")
            val LESS_THAN = Text.literal("\uE06D")
            val EQUALS = Text.literal("\uE06E")
            val GREATER_THAN = Text.literal("\uE06F")

            val ZERO = Text.literal("\uE060")
            val ONE = Text.literal("\uE061")
            val TWO = Text.literal("\uE062")
            val THREE = Text.literal("\uE063")
            val FOUR = Text.literal("\uE064")
            val FIVE = Text.literal("\uE065")
            val SIX = Text.literal("\uE066")
            val SEVEN = Text.literal("\uE067")
            val EIGHT = Text.literal("\uE068")
            val NINE = Text.literal("\uE069")
        }
    }

    // TODO: improve notifyChat
    fun notifyChat(msg: String) {
        val client = MinecraftClient.getInstance()
        client.execute {
            client.inGameHud.chatHud.addMessage(
                Text.literal("[")
                    .formatted(Formatting.DARK_BLUE)
                    .append(Text.literal("Greg").formatted(Formatting.BLUE))
                    .append(Text.literal("]").formatted(Formatting.DARK_BLUE))
                    .append(Text.literal(" $msg").formatted(Formatting.RESET))
            )
        }
    }

    fun discordMessage(type: String, message: String, player: String = "", channel: String = "") {
        val client = MinecraftClient.getInstance()
        client.execute {
            var msg = Text.empty()

            if (type == "info") {
                msg = Text.literal(message).withColor(0xAAAAAA).styled { it.withItalic(true) }
            } else if (type == "chat") {
                if (!ConfigManager.getBool("bridge chat")) return@execute
                msg = Text.empty()
                    .append(Text.literal("#$channel ").withColor(0xAAAAAA).styled { it.withItalic(true) })
                    .append(Text.literal("$player: ").withColor(0x00AAAA))
                    .append(Text.literal(message).withColor(0x55FFFF))
            } else if (type == "raid") {
                if (!ConfigManager.getBool("raid pings")) return@execute
                client.player?.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
                client.inGameHud.chatHud.addMessage(Text.empty())
                msg = Text.empty()
                    .append(Text.literal("RAID ").withColor(0xFF5555).styled { it.withBold(true) })
                    .append(Text.literal("$player: ").withColor(0xAA00AA))
                    .append(Text.literal("$message \n").withColor(0xFF55FF))
            } else return@execute

            client.inGameHud.chatHud.addMessage(Text.empty()
                .append(Text.literal("  "))
                .append(discordPill)
                .append(msg)
            )
        }
    }

    // wynncraft-styled banner/pill for discord
    val discordPill = Text.empty()

        .append(characters.banner.START.withColor(0x5555FF))
        .append(characters.spacing.CHAR_JOINER)

        .append(characters.banner.BODY.withColor(0x5555FF))
        .append(characters.spacing.CHAR_OVERLAPPER)
        .append(characters.five.D.withoutShadow().withColor(0x000000))

        .append(characters.banner.BODY.withColor(0x5555FF))
        .append(characters.spacing.CHAR_OVERLAPPER)
        .append(characters.five.I.withoutShadow().withColor(0x000000))

        .append(characters.banner.BODY.withColor(0x5555FF))
        .append(characters.spacing.CHAR_OVERLAPPER)
        .append(characters.five.S.withoutShadow().withColor(0x000000))

        .append(characters.banner.BODY.withColor(0x5555FF))
        .append(characters.spacing.CHAR_OVERLAPPER)
        .append(characters.five.C.withoutShadow().withColor(0x000000))

        .append(characters.banner.BODY.withColor(0x5555FF))
        .append(characters.spacing.CHAR_OVERLAPPER)
        .append(characters.five.O.withoutShadow().withColor(0x000000))

        .append(characters.banner.BODY.withColor(0x5555FF))
        .append(characters.spacing.CHAR_OVERLAPPER)
        .append(characters.five.R.withoutShadow().withColor(0x000000))

        .append(characters.banner.BODY_SHORT.withColor(0x5555FF))
        .append(characters.spacing.CHAR_OVERLAPPER_SHORT)
        .append(characters.five.D.withoutShadow().withColor(0x000000))
        .append(characters.spacing.CHAR_JOINER)

        .append(characters.banner.END.withColor(0x5555FF))
        .append(characters.spacing.SPACE)
}