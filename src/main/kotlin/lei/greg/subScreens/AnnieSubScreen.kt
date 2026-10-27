package lei.greg.subScreens

import io.wispforest.owo.braid.core.Alignment
import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.component.TextureComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.StackLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Animation
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.Easing
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.Positioning
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.core.UIComponent
import io.wispforest.owo.ui.core.VerticalAlignment
import io.wispforest.owo.ui.util.NinePatchTexture
import lei.greg.GregUtils
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import java.net.URI

// TODO: awawa sends list of people with admin perms and i show management if player is in there
// ofc check the packet on awawa side too

// TODO: better colors maybe
enum class AnnieRoles(val color: Int, val iconName: String) {
    DPS(0xFF6B8A, "a"),
    HEALER(0x6BFFB0, "heart-icon"),
    TANK(0x6B8AFF, "a"),
    NONE(0xbfbfbf, "a");
}

val customIcons = mapOf(
    "AlfieCrafts" to "alfie-icon"
)

class AnnieSubScreen(private val parent: Screen?) : BaseUIModelScreen<StackLayout>(StackLayout::class.java, DataSource.asset(GregUtils.id("annie-subscreen"))) {

    private var hoverOwner: UIComponent? = null
    /*
    val hoverLabel: ScaledLabel = ScaledLabel(Text.empty(), 0.55f)
        .margins(Insets.bottom(4))
        .sizing(Sizing.content(), Sizing.fixed(5)) as ScaledLabel
*/
    override fun build(rootComponent: StackLayout) {
        hookButtons(rootComponent)
        applyCustomTextures(rootComponent)
        addTestPanels(2, rootComponent)

        GregUtils.LOGGER.info("finished building annie screen")
    }

    override fun close() {
        client?.setScreen(parent)
    }

    private fun applyCustomTextures(rootComponent: StackLayout) {
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "window")!!.surface {ctx, component ->
            NinePatchTexture.draw(GregUtils.id("window"), ctx, component)
        }
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "bg")!!.surface {ctx, component ->
            NinePatchTexture.draw(GregUtils.id("bgs/annie-bg"), ctx, component)
        }
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "top-bar")!!.surface {ctx, component ->
            NinePatchTexture.draw(GregUtils.id("category-button-inactive"), ctx, component)
        }
    }

    private fun hookButtons(rootComponent: StackLayout) {
        rootComponent.childById(ButtonComponent::class.java, "back")
            .renderer { ctx, component, _ ->
                val texture = if (component.isHovered) GregUtils.id("back-icon-hovered") else GregUtils.id("back-icon")
                NinePatchTexture.draw(texture, ctx, component.x, component.y, component.width, component.height - 1)
            }
            .onPress { close() }
        rootComponent.childById(ButtonComponent::class.java, "discord")
            .renderer { ctx, component, _ ->
                val texture = if (component.isHovered) GregUtils.id("discord-logo-hovered") else GregUtils.id("discord-logo")
                NinePatchTexture.draw(texture, ctx, component.x, component.y, component.width, component.height - 1)
            }
            .onPress { Util.getOperatingSystem().open(URI(GregUtils.DISCORD_LINK)) }
    }

    private fun addTestPanels(amt: Int, rootComponent: StackLayout) {
        val holder = rootComponent.childById(FlowLayout::class.java, "party-holder")
        for (i in 1..amt) {
            holder.child(partyPanel())
        }
        if (holder.children().lastIndex > 2) holder.horizontalSizing(Sizing.content()) // enables scroll and left-side alignement if more than 3 parties
    }

    private fun partyPanel(): FlowLayout {
        val panel = model.expandTemplate(
            FlowLayout::class.java,
            "party-panel",
            emptyMap())

        val hoverLabel = ScaledLabel(Text.empty(), 0.55f).apply {
            margins(Insets.bottom(4))
            sizing(Sizing.content(), Sizing.fixed(5))
        }

        panel.surface{ctx, component ->
            NinePatchTexture.draw(GregUtils.id("entry"), ctx, component)
        }
        panel.childById(FlowLayout::class.java, "annie-separator").surface { ctx, component ->
            NinePatchTexture.draw(GregUtils.id("annie-separator"), ctx, component)
        }
        panel.child(hoverLabel)

        panel.child(playerLabel(hoverLabel, "AlfieCrafts", true, true, AnnieRoles.HEALER))
        panel.child(playerLabel(hoverLabel, "OneZero09", false, false, AnnieRoles.HEALER))
        panel.child(playerLabel(hoverLabel, "Crixx007", true, false, AnnieRoles.TANK))
        panel.child(playerLabel(hoverLabel, "catboylei", true, false, AnnieRoles.DPS))
        panel.child(playerLabel(hoverLabel, "Citr_n", false, false, AnnieRoles.DPS))
        panel.child(playerLabel(hoverLabel, "TheWalrusMaster", true, true, AnnieRoles.DPS))
        panel.child(playerLabel(hoverLabel, "MMMMMMMMMMMMMMMM", false, false, AnnieRoles.DPS))
        panel.child(playerLabel(hoverLabel, "lei", false, true, AnnieRoles.NONE))
        panel.child(playerLabel(hoverLabel, "-", false, false))
        panel.child(playerLabel(hoverLabel, "-", false, false))

        return panel
    }

    private fun playerLabel(hoverLabel: ScaledLabel, name: String, cl: Boolean, hs: Boolean, role: AnnieRoles = AnnieRoles.NONE): FlowLayout {
        val normal = (Text.literal(name))
        val hovered = (Text.literal(name).formatted(Formatting.BOLD))

        val label = ScaledLabel(Text.literal(name), 0.70f)
            .color(Color.ofRgb(role.color))

        val icons = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(8))
        if (name in customIcons.keys) { icons.child(icon(customIcons.getValue(name))) }

        val fullLabel = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fixed(12))
            .gap(2)
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER) as FlowLayout

        if (name != "-") bindHover(
            hoverLabel = hoverLabel,
            source = fullLabel,
            info = { makeInfoText(role, cl, hs) },
            extraEnter = { label.text(hovered) },
            extraLeave = { label.text(normal) }
        )

        return fullLabel
            .child(label)
            .child(icons)
            .margins(Insets.horizontal(2)) as FlowLayout
    }

    class ScaledLabel(text: Text, private val scale: Float) : LabelComponent(text) {
        override fun determineHorizontalContentSize(sizing: Sizing): Int =
            (super.determineHorizontalContentSize(sizing) * scale).toInt()

        override fun determineVerticalContentSize(sizing: Sizing): Int =
            (super.determineVerticalContentSize(sizing) * scale).toInt()

        override fun draw(graphics: OwoUIGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
            val matrices = graphics.matrices
            matrices.pushMatrix()
            matrices.translate(x.toFloat(), y.toFloat())
            matrices.scale(scale, scale)
            matrices.translate(-x.toFloat(), -y.toFloat())
            super.draw(graphics, mouseX, mouseY, partialTicks, delta)
            matrices.popMatrix()
        }
    }

    private fun icon(id: String, size: Int = 8): TextureComponent = UIComponents.texture(GregUtils.id("textures/$id.png"), 0, 0, 16, 16, 16, 16)
        .apply { sizing(Sizing.fixed(size))}

    private fun bindHover(hoverLabel: ScaledLabel, source: UIComponent, info: (() -> Text)?, extraEnter: () -> Unit = {}, extraLeave: () -> Unit = {}) {
        source.mouseEnter().subscribe {
            if (info != null) {
                hoverOwner = source
                hoverLabel.text(info())
            }
            extraEnter()
        }
        source.mouseLeave().subscribe {
            if (hoverOwner === source && info != null) {   // ignore leaves from labels that were already replaced
                hoverOwner = null
                hoverLabel.text(Text.empty())
            }
            extraLeave()
        }
    }

    private fun makeInfoText(role: AnnieRoles, cl: Boolean, hs: Boolean): Text {
        return Text.empty().apply {
            append(Text.literal(role.toString()).withColor(role.color))
            append(Text.literal(" | ").withColor(0xbfbfbf))
            append( if (cl) Text.literal("Can Lead").withColor(0x6BFFB0) else Text.literal("No Lead").withColor(0xFF6B8A))
            append(Text.literal(" | ").withColor(0xbfbfbf))
            append( if (hs) Text.literal("Has Scrolls").withColor(0x6BFFB0) else Text.literal("No Scrolls").withColor(0xFF6B8A))
        }
    }
}