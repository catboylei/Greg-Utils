package lei.greg.config

import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.component.TextBoxComponent
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.StackLayout
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.ParentUIComponent
import io.wispforest.owo.ui.util.NinePatchTexture
import lei.greg.GregUtils
import lei.greg.backend.Backend.status
import lei.greg.events.backend.AwawaStatusUpdated
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.server.command.CommandManager
import net.minecraft.text.Text

// ties together the screen and all the components from ressources/assets/greg-utils/owo-ui/ui-model.xml
// also ties them to the config manager, and applies the custom textures
// this file probably contains a lot of spaghetti code, you may PR and try to fix it lol
class ConfigScreen: BaseUIModelScreen<StackLayout>(StackLayout::class.java, DataSource.asset(GregUtils.id("ui-model"))) {

    private var active = true
    private var backendDescLabel: LabelComponent? = null

    private val hiddenFields = setOf(
        "fkl password"
    )

    override fun build(rootComponent: StackLayout) {
        active = true

        updateTopBar(rootComponent)
        updateEntries(rootComponent)
        applyCustomTextures(rootComponent)
        hookBackendStatusLabel()

        GregUtils.LOGGER.info("finished building ui screen")
    }

    override fun init() {
        active = true
        super.init()
    }

    private fun hookBackendStatusLabel() {
        AwawaStatusUpdated.EVENT.register { status ->
            if (active) {
                GregUtils.LOGGER.info("backend status label found: ${backendDescLabel}, status=${status.string}")
                backendDescLabel?.text(Text.literal("Status: ").append(status))
            }
        }
    }

    override fun removed() {
        active = false
        super.removed()
    }

    private fun applyCustomTextures(rootComponent: StackLayout) {
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "window")!!.surface {ctx, component ->
            NinePatchTexture.draw(GregUtils.id("gui-textures/window"), ctx, component)
        }
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "separator")!!.surface {ctx, component ->
            NinePatchTexture.draw(GregUtils.id("gui-textures/separator"), ctx, component)
        }
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "bg-holder")!!.surface {ctx, component ->
            backgroundRendering(ctx, component)
        }
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "top-bar")!!.surface {ctx, component ->
            NinePatchTexture.draw(GregUtils.id("gui-textures/category-button-inactive"), ctx, component)
        }

        rootComponent.forEachDescendant { component ->
            if (component is ButtonComponent && component.id()!!.startsWith("category-")) {
                component.renderer { ctx, component, _ -> buttonRendering(ctx, component) }
                component.onPress {
                    ConfigManager.setOption("open category", component.id()!!.removePrefix("category-"))
                    updateTopBar(rootComponent)
                    updateEntries(rootComponent)
                }
            }
        }
    }

    private fun buttonRendering(ctx: OwoUIGraphics, component: ButtonComponent) {
        var texture = GregUtils.id("gui-textures/category-button-inactive")

        // i wrote this at 5 am
        if (component.isHovered) {
            texture = GregUtils.id("gui-textures/category-button-hovered")
        } else if ("category-${ConfigManager.getString("open category")}" == component.id()) {
            texture = GregUtils.id("gui-textures/category-button-active")
        }

        // splendid
        NinePatchTexture.draw(texture, ctx, component.x, component.y, component.width, component.height - 1)
    }

    private fun backgroundRendering(ctx: OwoUIGraphics, component: ParentUIComponent) {
        val texture = when (ConfigManager.getString("open category")) {
            "General Settings" -> GregUtils.id("bgs/general-bg")
            "Flooding Canyon (Void Holes) Room" -> GregUtils.id("bgs/holes-bg")
            "Sunken Grotto (Hold) Room" -> GregUtils.id("bgs/hold-bg")
            "Nameless Cave (Light) Room" -> GregUtils.id("bgs/light-bg")
            "Weeping Soulroot (Tree) Room" -> GregUtils.id("bgs/tree-bg")
            "Blueshift Wilds (Bulb) Room" -> GregUtils.id("bgs/bulb-bg")
            "Twisted Jungle (Gather) Room" -> GregUtils.id("bgs/gather-bg")
            "Gregory Bossfight" -> GregUtils.id("bgs/greg-bg")
            "Random" -> GregUtils.id("bgs/other-bg")
            else -> GregUtils.id("bgs/general-bg")
        }

        NinePatchTexture.draw(texture, ctx, component)
    }

    private fun updateTopBar(rootComponent: StackLayout) {
        rootComponent.childById(LabelComponent::class.java, "top-bar-label").text(
            Text.literal(ConfigManager.getString("open category"))
        )
    }

    private fun fieldRendering(ctx: OwoUIGraphics, component: FlowLayout) {
        var texture = GregUtils.id("gui-textures/category-button-inactive")
        val textbox = component.parent()!!.childById(TextBoxComponent::class.java, component.id()!!)

        if (textbox.isHovered) {
            texture = GregUtils.id("gui-textures/category-button-hovered")
        } else if (textbox.isSelected) {
            texture = GregUtils.id("gui-textures/category-button-active")
        }

        NinePatchTexture.draw(texture, ctx, component)
    }

    private fun switchRendering(ctx: OwoUIGraphics, component: ButtonComponent) {
        var texture = GregUtils.id("icons/switch-inactive")
        val state = ConfigManager.getBool(component.id()!!)

        if (state && component.isHovered) {
            texture = GregUtils.id("icons/switch-active-hovered")
        } else if (!state && component.isHovered) {
            texture = GregUtils.id("icons/switch-inactive-hovered")
        } else if (state) {
            texture = GregUtils.id("icons/switch-active")
        }

        NinePatchTexture.draw(texture, ctx, component)
    }

    private fun actionButtonRendering(ctx: OwoUIGraphics, component: ButtonComponent) {
        // TODO: what is that please get rid of it
        val type = if (component.width == 16) "icons/refresh-button" else "icons/external-link" // i lwk dont even know anymore

        val texture = if (component.isHovered) GregUtils.id("$type-hovered")
        else GregUtils.id(type)
        NinePatchTexture.draw(texture, ctx, component.x, component.y, component.width, component.height)
    }

    private fun getEntry(title: String, desc: String, configId: String, type: String): FlowLayout {
        val entry = model.expandTemplate(
            FlowLayout::class.java,
            "entry-${type}",
            mapOf("title" to title, "desc" to desc, "configId" to configId))
        entry.surface{ctx, component ->
            NinePatchTexture.draw(GregUtils.id("gui-textures/entry"), ctx, component)
        }
        if (configId == "reconnect backend") {
            val labels = mutableListOf<LabelComponent>()
            entry.forEachDescendant { if (it is LabelComponent) labels.add(it) }
            backendDescLabel = labels.getOrNull(1) // [0] = title, [1] = description
            backendDescLabel?.text(Text.literal("Status: ").append(status))
        }
        entry.forEachDescendant { component ->
            if (component.id() != configId) return@forEachDescendant
            if (component is ButtonComponent && type == "button") {
                if (configId == "reconnect backend") {
                    component.width = 16
                    component.height = 17
                }
                val action = ConfigActions.actions[configId]
                component.setMessage(Text.empty())
                component.renderer { ctx, btn, _ -> actionButtonRendering(ctx, btn) }
                component.onPress {
                    if (action == null) GregUtils.LOGGER.warn("No action registered for \"$configId\"")
                    else action()
                }
            } else if (component is ButtonComponent) {
                component.renderer { ctx, component, _ ->
                    switchRendering(ctx, component)
                }
                component.onPress {
                    ConfigManager.setOption(configId, (!ConfigManager.getBool(configId)).toString())
                }
            } else if (component is TextBoxComponent) {
                component.onChanged().subscribe { value ->
                    ConfigManager.setOption(configId, value)
                }
                component.text(ConfigManager.getString(configId))
                component.setPlaceholder(Text.literal("Input..."))

                // hide inputs in configIds listed
                if (configId in hiddenFields) {
                    component.addFormatter { text, _ ->
                        Text.literal("#".repeat(text.length)).asOrderedText()
                    }
                }
            } else if (component is FlowLayout) {
                component.surface {ctx, component ->
                    fieldRendering(ctx, component as FlowLayout)
                }
            }
        }
        return entry
    }

    private fun updateEntries(rootComponent: StackLayout) {
        val holder = rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "entry-holder")!!
        backendDescLabel = null

        holder.clearChildren()
        for ((type, title, desc, configId, category) in ScreenEntries.entries) {
            if (category != ConfigManager.getString("open category")) { continue }
            holder.child(getEntry(title, desc, configId, type))
        }

        val label = rootComponent.childById(LabelComponent::class.java, "desc-reconnect backend") ?: return
        label.text(Text.literal("Status: ").append(status))
    }

    companion object {
        fun registerCommand() {
            CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
                dispatcher.register(
                    CommandManager.literal("greg").executes {
                        val client = MinecraftClient.getInstance();
                        client.execute { client.setScreen(ConfigScreen()) }
                        1
                    }
                )
            }
        }
    }
}