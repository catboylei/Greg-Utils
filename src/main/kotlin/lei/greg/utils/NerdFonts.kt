package lei.greg.utils

import lei.greg.GregUtils
import net.minecraft.text.MutableText
import net.minecraft.text.Style
import net.minecraft.text.StyleSpriteSource
import net.minecraft.text.Text

object NerdFonts {
    private val FONT = StyleSpriteSource.Font(GregUtils.id("nerd"))

    fun icon(codepoint: Int): MutableText =
        Text.literal(String(Character.toChars(codepoint))).setStyle(Style.EMPTY.withFont(FONT))   // see note below
}