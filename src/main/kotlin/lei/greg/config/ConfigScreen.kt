package lei.greg.config

// todo category separator title thing entry
// todo switches

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
import net.minecraft.text.Text

class ConfigScreen: BaseUIModelScreen<StackLayout>(StackLayout::class.java, DataSource.asset(GregUtils.id("ui-model"))) {

    override fun build(rootComponent: StackLayout) {
        updateTopBar(rootComponent)
        updateEntries(rootComponent)
        applyCustomTextures(rootComponent)
    }

    private fun applyCustomTextures(rootComponent: StackLayout) {
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "window")!!.surface {ctx, component ->
            NinePatchTexture.draw(GregUtils.id("window"), ctx, component)
        }
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "separator")!!.surface {ctx, component ->
            NinePatchTexture.draw(GregUtils.id("separator"), ctx, component)
        }
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "bg-holder")!!.surface {ctx, component ->
            backgroundRendering(ctx, component)
        }
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "top-bar")!!.surface {ctx, component ->
            NinePatchTexture.draw(GregUtils.id("category-button-inactive"), ctx, component)
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
        var texture = GregUtils.id("category-button-inactive")

        // i wrote this at 5 am
        if (component.isHovered) {
            texture = GregUtils.id("category-button-hovered")
        } else if ("category-${ConfigManager.getString("open category")}" == component.id()) {
            texture = GregUtils.id("category-button-active")
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
        var texture = GregUtils.id("category-button-inactive")
        val textbox = component.parent()!!.childById(TextBoxComponent::class.java, component.id()!!)

        if (textbox.isHovered) {
            texture = GregUtils.id("category-button-hovered")
        } else if (textbox.isSelected) {
            texture = GregUtils.id("category-button-active")
        }

        NinePatchTexture.draw(texture, ctx, component)
    }

    private fun switchRendering(ctx: OwoUIGraphics, component: ButtonComponent) {
        var texture = GregUtils.id("switch-inactive")
        val state = ConfigManager.getBool(component.id()!!)

        if (state && component.isHovered) {
            texture = GregUtils.id("switch-active-hovered")
        } else if (!state && component.isHovered) {
            texture = GregUtils.id("switch-inactive-hovered")
        } else if (state) {
            texture = GregUtils.id("switch-active")
        }

        NinePatchTexture.draw(texture, ctx, component)
    }

    private fun getEntry(title: String, desc: String, configId: String, type: String): FlowLayout {
        val entry = model.expandTemplate(
            FlowLayout::class.java,
            "entry-${type}",
            mapOf("title" to title, "desc" to desc, "configId" to configId))
        entry.surface{ctx, component ->
            NinePatchTexture.draw(GregUtils.id("entry"), ctx, component)
        }
        entry.forEachDescendant { component ->
            if (component.id() != configId) return@forEachDescendant
            if (component is ButtonComponent) {
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

        holder.clearChildren()
        for ((type, title, desc, configId, category) in ScreenEntries.entries) {
            if (category != ConfigManager.getString("open category")) { continue }
            holder.child(getEntry(title, desc, configId, type))
        }
    }
}