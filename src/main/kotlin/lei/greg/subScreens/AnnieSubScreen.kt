package lei.greg.subScreens

import com.mojang.brigadier.arguments.BoolArgumentType
import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.component.TextureComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.StackLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Color
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
import lei.greg.Utils.notifyToast
import lei.greg.features.AnnieParties
import lei.greg.features.Party
import lei.greg.features.Player
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.toast.SystemToast
import net.minecraft.command.argument.Vec3ArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import java.net.URI

// TODO: awawa sends list of people with admin perms and i show management if player is in there
// ofc check the packet on awawa side too
// TODO: subscribe to parties changed event and call updateParties then
// TODO: register command to open this

enum class AnnieRoles(val color: Int, val iconName: String) {
    HEAL(0xFF0fff73.toInt(), "heart-icon"),
    TANK(0xFF0fbbff.toInt(), "a"),
    DPS(0xFFFF6B8A.toInt(), "a"),
    NONE(0xFFbfbfbf.toInt(), "a");
}

val customIcons = mapOf(
    "AlfieCrafts" to "alfie-icon" // annie manager since 1932 gets her icon
)

class AnnieSubScreen(private val parent: Screen? = null) : BaseUIModelScreen<StackLayout>(StackLayout::class.java, DataSource.asset(GregUtils.id("annie-subscreen"))) {

    private var hoverOwner: UIComponent? = null

    override fun build(rootComponent: StackLayout) {
        hookButtons(rootComponent)
        applyCustomTextures(rootComponent)
        AnnieParties.populateWithTest() // TODO: ykyk explode the test population
        updateParties(rootComponent)

        GregUtils.LOGGER.info("finished building annie screen")
    }

    override fun close() {
        client?.setScreen(parent)
    }

    private fun updateParties(rootComponent: StackLayout) {
        rootComponent.childById(FlowLayout::class.java, "party-holder").apply {
            clearChildren()
            AnnieParties.populateWithTest()
            AnnieParties.parties.forEach {
                child(partyPanel(it))
            }
        }
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
            .onPress {
                Util.getOperatingSystem().open(URI(GregUtils.DISCORD_LINK));
                notifyToast("FKL Discord", "Check your browser :3")
            }
    }

    private fun partyPanel(party: Party): FlowLayout {
        val panel = model.expandTemplate(
            FlowLayout::class.java,
            "party-panel",
            emptyMap())

        val hoverLabel = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(5)).apply {
            margins(Insets.bottom(4))
        }

        panel.childById(FlowLayout::class.java, "panel-top").apply {
            child(icon(if (!party.active) "scroll-icon-disabled" else "scroll-icon", 12).positioning(Positioning.relative(10, 40)))
            child(icon(if (!party.hasScrolls) "cross-icon" else "checkmark-icon", 8).positioning(Positioning.relative(15, 90)))
            child(UIComponents.label(Text.literal("Party ${party.number}").withColor(if (!party.active) 0xbfbfbf else 0xe66aff)))
            child(UIComponents.button(Text.empty(), {
                if (party.active && !party.isFull) AnnieParties.joinAnnie(party.number)
            }).apply {
                positioning(Positioning.relative(90, 40))
                sizing(Sizing.fixed(12))
                renderer { ctx, component, _ ->
                    val texture = GregUtils.id( if (!party.active || party.isFull) "join-icon-inactive" else (if (component.isHovered) "join-icon-hovered" else "join-icon"))
                    NinePatchTexture.draw(texture, ctx, component.x, component.y, component.width, component.height - 1)
                }
            })
        }

        panel.surface{ctx, component ->
            NinePatchTexture.draw(GregUtils.id("entry"), ctx, component)
        }
        panel.childById(FlowLayout::class.java, "annie-separator").surface { ctx, component ->
            NinePatchTexture.draw(GregUtils.id("annie-separator"), ctx, component)
        }
        panel.child(hoverLabel)

        panel.apply {
            party.players.forEach {
                child(playerLabel(hoverLabel, it))
            }
        }

        return panel
    }

    private fun playerLabel(hoverLabel: FlowLayout, player: Player): FlowLayout {
        val normal = (Text.literal(player.name))
        val hovered = (Text.literal(player.name).formatted(Formatting.BOLD))

        val label = ScaledLabel(Text.literal(player.name), 0.70f).color(Color.ofRgb(0xbfbfbf))

        val icons = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(8))
        if (player.name in customIcons.keys) { icons.child(icon(customIcons.getValue(player.name))) }

        val bar = UIContainers.verticalFlow(Sizing.fixed(1), Sizing.fixed(12))
            .surface(if (player.role == AnnieRoles.NONE) Surface.BLANK else Surface.flat(player.role.color))

        val fullLabel = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fixed(12))
            .gap(2)
            .horizontalAlignment(HorizontalAlignment.LEFT)
            .verticalAlignment(VerticalAlignment.CENTER)
            .padding(Insets.left(2)) as FlowLayout

        if (player.name != "-") {
            fullLabel.mouseDown().subscribe { click, _ ->
                if (click.buttonInfo.button == 0) {
                    MinecraftClient.getInstance().keyboard.clipboard = player.name
                    notifyToast("Player name copied", player.name)
                    true
                } else if (click.buttonInfo.button == 1) {
                    MinecraftClient.getInstance().keyboard.clipboard = "/party invite ${player.name}"
                    notifyToast("Command copied", "/party invite ${player.name}")
                    true
                } else false
            }

            bindHover(
                hoverLabel = hoverLabel,
                source = fullLabel,
                info = { makeInfoText(player.role, player.canLead, player.hasScrolls) },
                extraEnter = { label.text(hovered) },
                extraLeave = { label.text(normal) }
            )
        }

        return fullLabel.apply {
                child(bar)
                child(label)
                child(icons)
                margins(Insets.horizontal(2))
            }
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

    private fun bindHover(hoverLabel: FlowLayout, source: UIComponent, info: (() -> FlowLayout)?, extraEnter: () -> Unit = {}, extraLeave: () -> Unit = {}) {
        source.mouseEnter().subscribe {
            if (info != null) {
                hoverOwner = source
                hoverLabel.clearChildren()
                hoverLabel.child(info())
            }
            extraEnter()
        }
        source.mouseLeave().subscribe {
            if (hoverOwner === source && info != null) {   // ignore leaves from labels that were already replaced
                hoverOwner = null
                hoverLabel.clearChildren()
            }
            extraLeave()
        }
    }

    private fun makeInfoText(role: AnnieRoles, cl: Boolean, hs: Boolean): FlowLayout {

        return UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(5)).apply {
            child(ScaledLabel(Text.literal(role.toString()).withColor(role.color), 0.55f).apply {
                horizontalSizing(Sizing.fixed(25))
                horizontalTextAlignment(HorizontalAlignment.RIGHT)
                margins(Insets.right(-12))
            })

            child(ScaledLabel(Text.literal(" | ").withColor(0xbfbfbf), 0.55f))

            child(icon(if (cl) "checkmark-icon" else "cross-icon", 5))
            child(ScaledLabel(Text.literal(" Leader").withColor(if (cl) 0x0fff73 else 0xFF6B8A), 0.55f))

            child(ScaledLabel(Text.literal(" | ").withColor(0xbfbfbf), 0.55f))

            child(icon(if (hs) "checkmark-icon" else "cross-icon", 5))
            child(ScaledLabel(Text.literal(" Scrolls").withColor(if (hs) 0x0fff73 else 0xFF6B8A), 0.55f))
        }
    }

    companion object {
        fun registerCommand() {
            CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
                dispatcher.register(
                    CommandManager.literal("annie").executes {
                        val client = MinecraftClient.getInstance();
                        client.execute { client.setScreen(AnnieSubScreen()) }
                        1
                    }
                )
            }
        }
    }
}