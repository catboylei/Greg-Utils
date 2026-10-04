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
            val CHAR_JOINER get() = Text.literal("\u2064")
            val CHAR_OVERLAPPER get() = Text.literal("\uE012")
            val CHAR_OVERLAPPER_SHORT get() = Text.literal("\uE071")
            val SPACE get() = Text.literal("\uE013")
        }

        object banner {
            val START get() = Text.literal("\uE010")
            val BODY get() = Text.literal("\uE00F")
            val BODY_SHORT get() = Text.literal("\uE070")
            val END get() = Text.literal("\uE011")
        }

        object five {
            val A get() = Text.literal("\uE040")
            val B get() = Text.literal("\uE041")
            val C get() = Text.literal("\uE042")
            val D get() = Text.literal("\uE043")
            val E get() = Text.literal("\uE044")
            val F get() = Text.literal("\uE045")
            val G get() = Text.literal("\uE046")
            val H get() = Text.literal("\uE047")
            val I get() = Text.literal("\uE048")
            val J get() = Text.literal("\uE049")
            val K get() = Text.literal("\uE04A")
            val L get() = Text.literal("\uE04B")
            val M get() = Text.literal("\uE04C")
            val N get() = Text.literal("\uE04D")
            val O get() = Text.literal("\uE04E")
            val P get() = Text.literal("\uE04F")
            val Q get() = Text.literal("\uE050")
            val R get() = Text.literal("\uE051")
            val S get() = Text.literal("\uE052")
            val T get() = Text.literal("\uE053")
            val U get() = Text.literal("\uE054")
            val V get() = Text.literal("\uE055")
            val W get() = Text.literal("\uE056")
            val X get() = Text.literal("\uE057")
            val Y get() = Text.literal("\uE058")
            val Z get() = Text.literal("\uE059")

            val QUESTION_MARK get() = Text.literal("\uE05A")
            val SQUARE_BRACKET_OPEN get() = Text.literal("\uE05B")
            val SQUARE_BRACKET_CLOSE get() = Text.literal("\uE05C")
            val BACKSLASH get() = Text.literal("\uE05D")
            val PERCENT get() = Text.literal("\uE05E")
            val AND get() = Text.literal("\uE05F")
            val EXCLAMATION get() = Text.literal("\uE06A")
            val BRACKET_OPEN get() = Text.literal("\uE06B")
            val BRACKET_CLOSE get() = Text.literal("\uE06C")
            val LESS_THAN get() = Text.literal("\uE06D")
            val EQUALS get() = Text.literal("\uE06E")
            val GREATER_THAN get() = Text.literal("\uE06F")

            val ZERO get() = Text.literal("\uE060")
            val ONE get() = Text.literal("\uE061")
            val TWO get() = Text.literal("\uE062")
            val THREE get() = Text.literal("\uE063")
            val FOUR get() = Text.literal("\uE064")
            val FIVE get() = Text.literal("\uE065")
            val SIX get() = Text.literal("\uE066")
            val SEVEN get() = Text.literal("\uE067")
            val EIGHT get() = Text.literal("\uE068")
            val NINE get() = Text.literal("\uE069")
        }
    }

    fun notifyChat(msg: String) {
        val client = MinecraftClient.getInstance()
        client.execute {
            client.inGameHud.chatHud.addMessage(
                Text.empty()
                    .append(Text.literal("   "))
                    .append(gregPill)
                    .append(msg)
            )
        }
    }

    // w name?
    fun notifyTitle(title: String = "", subtitle: String = "", fadeInTicks: Int = 0, stayTicks :Int=10, fadeOutTicks :Int = 0) {
        val client = MinecraftClient.getInstance()
        client.execute {
            val hud = client.inGameHud
            hud.setTitleTicks(fadeInTicks, stayTicks, fadeOutTicks)
            hud.setTitle(Text.literal(title))
            hud.setSubtitle(Text.literal(subtitle))
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
                .append(Text.literal("   "))
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
        .append(characters.five.D.withoutShadow().withColor(0x00000))

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

    val gregPill = Text.empty()

        .append(characters.banner.START.withColor(0x0a1461))
        .append(characters.spacing.CHAR_JOINER)

        .append(characters.banner.BODY.withColor(0x14186d))
        .append(characters.spacing.CHAR_OVERLAPPER)
        .append(characters.five.G.withoutShadow().withColor(0xE6DEFF))

        .append(characters.banner.BODY.withColor(0x1e1b78))
        .append(characters.spacing.CHAR_OVERLAPPER)
        .append(characters.five.R.withoutShadow().withColor(0xE6DEFF))

        .append(characters.banner.BODY.withColor(0x291f84))
        .append(characters.spacing.CHAR_OVERLAPPER)
        .append(characters.five.E.withoutShadow().withColor(0xE6DEFF))

        .append(characters.banner.BODY.withColor(0x33228f))
        .append(characters.spacing.CHAR_OVERLAPPER)
        .append(characters.five.G.withoutShadow().withColor(0xE6DEFF))

        .append(characters.banner.BODY.withColor(0x3e259a))
        .append(characters.spacing.CHAR_OVERLAPPER)
        .append(characters.five.O.withoutShadow().withColor(0xE6DEFF))

        .append(characters.banner.BODY.withColor(0x4a28a5))
        .append(characters.spacing.CHAR_OVERLAPPER)
        .append(characters.five.R.withoutShadow().withColor(0xE6DEFF))

        .append(characters.banner.BODY_SHORT.withColor(0x562aaf))
        .append(characters.spacing.CHAR_OVERLAPPER_SHORT)
        .append(characters.five.Y.withoutShadow().withColor(0xE6DEFF))
        .append(characters.spacing.CHAR_JOINER)

        .append(characters.banner.END.withColor(0x622db9))
        .append(characters.spacing.SPACE)
}