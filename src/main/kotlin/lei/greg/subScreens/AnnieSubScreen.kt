package lei.greg.subScreens

import com.mojang.brigadier.arguments.BoolArgumentType
import io.wispforest.owo.braid.core.Alignment
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
import lei.greg.backend.Backend
import lei.greg.events.backend.AwawaPayloadEvents
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
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import java.net.URI
import javax.swing.GroupLayout

// TODO: awawa sends perm level and this shows management stuff based on it
// ofc check the packet on awawa side too
// TODO: subscribe to parties changed event and call updateParties then
// TODO: the entire management system !
// TODO: add a timer from AnnieParties.timestamp
// TODO: last non-null player of any active party may become a "add player" management button

enum class AnnieRoles(val color: Int) {
    HEAL(0xFF0fff73.toInt()),
    TANK(0xFF0fbbff.toInt()),
    DPS(0xFFFF6B8A.toInt()),
    NONE(0xFFbfbfbf.toInt());
}

private const val ADD_MARKER = "%INTERNAL_ADD%"

val customIcons = mapOf(
    "AlfieCrafts" to "custom/alfie-icon", // annie manager since 1932 gets her icon
    "itsyunaya" to "custom/ashley-icon", // wife
    "TheWalrusMaster" to "custom/walrus-icon" // goober gets goober icon
)

class AnnieSubScreen(private val parent: Screen? = null) : BaseUIModelScreen<StackLayout>(StackLayout::class.java, DataSource.asset(GregUtils.id("annie-subscreen"))) {

    private var hoverOwner: UIComponent? = null
    private var isStuffRegistered = false

    override fun build(rootComponent: StackLayout) {
        hookButtons(rootComponent)
        applyCustomTextures(rootComponent)

        updateParties(rootComponent)

        if (!isStuffRegistered) {
            AwawaPayloadEvents.ANNIE_STATUS.register {
                updateParties(rootComponent)
            }
        }

        GregUtils.LOGGER.info("finished building annie screen")
    }

    override fun close() {
        client?.setScreen(parent)
    }

    fun updateParties(rootComponent: StackLayout) {
        rootComponent.childById(FlowLayout::class.java, "party-holder").apply {
            clearChildren()
            horizontalSizing(Sizing.content())
            //alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)

            when {
                !Backend.isLoggedIn -> {
                    horizontalSizing(Sizing.fill())
                    //alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                    child(warningMessage("Not Connected to Awawa :("))
                }
                !AnnieParties.isActive -> {
                    sizing(Sizing.fill())
                    alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                    child(warningMessage("No Annie detected (yet ?)"))
                }
                else -> {
                    AnnieParties.parties.forEach {
                        child(partyPanel(it))
                    }
                }
            }
        }
    }

    private fun warningMessage(msg: String): FlowLayout = UIContainers.verticalFlow(Sizing.content(), Sizing.content()).apply {
        child(icon("disconnected-icon", 16))
        child(ScaledLabel(Text.literal(msg).withColor(0xe66aff), 1f))
        gap(4)
        horizontalAlignment(HorizontalAlignment.CENTER)
        padding(Insets.of(8))
        surface { ctx, component ->
            NinePatchTexture.draw(GregUtils.id("gui-textures/entry"), ctx, component)
        }
    }

    private fun applyCustomTextures(rootComponent: StackLayout) {
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "window")!!.surface {ctx, component ->
            NinePatchTexture.draw(GregUtils.id("gui-textures/window"), ctx, component)
        }
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "bg")!!.surface {ctx, component ->
            NinePatchTexture.draw(GregUtils.id("bgs/annie-bg"), ctx, component)
        }
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "top-bar")!!.surface {ctx, component ->
            NinePatchTexture.draw(GregUtils.id("gui-textures/category-button-inactive"), ctx, component)
        }
    }

    private fun hookButtons(rootComponent: StackLayout) {
        rootComponent.childById(ButtonComponent::class.java, "back")
            .renderer { ctx, component, _ ->
                val texture = if (component.isHovered) GregUtils.id("icons/back-icon-hovered") else GregUtils.id("icons/back-icon")
                NinePatchTexture.draw(texture, ctx, component.x, component.y, component.width, component.height - 1)
            }
            .onPress { close() }
        rootComponent.childById(ButtonComponent::class.java, "discord")
            .renderer { ctx, component, _ ->
                val texture = if (component.isHovered) GregUtils.id("icons/discord-logo-hovered") else GregUtils.id("icons/discord-logo")
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
                when (AnnieParties.playerParty) {
                    null -> if (party.active && !party.isFull) AnnieParties.joinAnnie(party.number)
                    else -> if (party.number == AnnieParties.playerParty) AnnieParties.leaveAnnie()
                }
            }).apply {
                positioning(Positioning.relative(90, 40))
                sizing(Sizing.fixed(12))
                // TODO: optimise renderer, only recompute on updating gui
                // TODO: implement a general cooldown for this button
                renderer { ctx, component, _ ->
                    val texture: Identifier = when (AnnieParties.playerParty) {
                        null -> GregUtils.id( if (!party.active || party.isFull) "icons/join-icon-inactive" else (if (component.isHovered) "icons/join-icon-hovered" else "icons/join-icon"))
                        else -> GregUtils.id( if (party.number != AnnieParties.playerParty) "icons/leave-icon-inactive" else (if (component.isHovered) "icons/leave-icon-hovered" else "icons/leave-icon"))
                    }
                    NinePatchTexture.draw(texture, ctx, component.x, component.y, component.width, component.height - 1)
                }
            })
        }

        val shown = party.players.toMutableList()
        if (Backend.hasPerms && party.active) {
            val idx = shown.indexOfFirst { it.name == "-" }
            if (idx != -1) shown[idx] = Player(name = ADD_MARKER)
        }

        panel.surface{ctx, component ->
            NinePatchTexture.draw(GregUtils.id("gui-textures/entry"), ctx, component)
        }
        panel.childById(FlowLayout::class.java, "annie-separator").surface { ctx, component ->
            NinePatchTexture.draw(GregUtils.id("gui-textures/annie-separator"), ctx, component)
        }
        panel.child(hoverLabel)

        panel.apply {
            shown.forEach { child(playerLabel(hoverLabel, it, party)) }
        }

        return panel
    }

    private fun playerLabel(hoverLabel: FlowLayout, player: Player, party: Party): FlowLayout {
        if (player.name == ADD_MARKER) return addSlot(party)

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

        val removeButton = UIComponents.button(Text.empty()) {
            //TODO: remove player manually function
        }.apply {
            positioning(Positioning.absolute(84, 0))
            renderer { ctx, component, _ ->
                NinePatchTexture.draw(GregUtils.id(if (component.isHovered) "icons/remove-icon-hovered" else "icons/remove-icon"), ctx, component)
            }
            sizing(Sizing.fixed(12))
        }

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
                if (Backend.hasPerms && player.name != "-") child(removeButton)

                child(bar)
                child(label)
                child(icons)
                margins(Insets.horizontal(2))
            }
    }

    private fun addSlot(party: Party): FlowLayout {
        val row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fixed(12))
            .gap(2)
            .horizontalAlignment(HorizontalAlignment.LEFT)
            .verticalAlignment(VerticalAlignment.CENTER)
            .padding(Insets.left(2)) as FlowLayout

        row.child(UIComponents.button(Text.empty(), {
            //TODO: manual add player function
        }).apply {
            renderer { ctx, component, _ ->
                NinePatchTexture.draw(GregUtils.id(if (component.isHovered) "icons/add-icon-hovered" else "icons/add-icon"), ctx, component)
            }
            sizing(Sizing.fixed(12))
        })
        return row.margins(Insets.horizontal(1)) as FlowLayout
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

    private fun icon(id: String, size: Int = 8): TextureComponent = UIComponents.texture(GregUtils.id("textures/icons/$id.png"), 0, 0, 16, 16, 16, 16)
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