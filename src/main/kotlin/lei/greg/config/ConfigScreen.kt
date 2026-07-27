package lei.greg.config

import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.StackLayout
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.ParentUIComponent
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.util.NinePatchTexture
import lei.greg.GregUtils
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import java.awt.Button

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
        var texture = when (ConfigManager.getString("open category")) {
            "General Settings" -> GregUtils.id("bgs/general-bg")
            "Flooding Canyon (Void Holes) Room" -> GregUtils.id("bgs/holes-bg")
            "Sunken Grotto (Hold) Room" -> GregUtils.id("bgs/hold-bg")
            "Nameless Cave (Light) Room" -> GregUtils.id("bgs/light-bg")
            "Weeping Soulroot (Tree) Room" -> GregUtils.id("bgs/tree-bg")
            "Blueshift Wilds (Bulb) Room" -> GregUtils.id("bgs/bulb-bg")
            "Twisted Jungle (Gather) Room" -> GregUtils.id("bgs/gather-bg")
            "Gregory Bossfight" -> GregUtils.id("bgs/greg-bg")
            else -> GregUtils.id("bgs/general-bg")
        }

        NinePatchTexture.draw(texture, ctx, component)
    }

    private fun updateTopBar(rootComponent: StackLayout) {
        rootComponent.childById(LabelComponent::class.java, "top-bar-label").text(
            Text.literal(ConfigManager.getString("open category"))
        )
    }

    private fun getBoolEntry(title: String, desc: String, configId: String): FlowLayout {
        val entry = model.expandTemplate(
            FlowLayout::class.java,
            "entry",
            mapOf("title" to title, "desc" to desc, "configId" to configId))
        entry.surface{ctx, component ->
            NinePatchTexture.draw(GregUtils.id("entry"), ctx, component)
        }
        entry.forEachDescendant { component ->
            if (component is ButtonComponent && component.id() == configId) {
                component.renderer { ctx, component, _ ->
                    val texture =  if (ConfigManager.getBool(configId)) (GregUtils.id("switch-active")) else GregUtils.id("switch-inactive")
                    NinePatchTexture.draw(texture, ctx, component)
                }
                component.onPress {
                    ConfigManager.setOption(configId, (!ConfigManager.getBool(configId)).toString())
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
            if (type == "bool") {
                holder.child(getBoolEntry(title, desc, configId))
            }
        }
    }
}