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
import net.minecraft.text.Text
import java.awt.Button

class ConfigScreen: BaseUIModelScreen<StackLayout>(StackLayout::class.java, DataSource.asset(GregUtils.id("ui-model"))) {

    override fun build(rootComponent: StackLayout) {
        applyCustomTextures(rootComponent)
        updateTopBar(rootComponent)
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
                }
            }
        }
    }

    private fun buttonRendering(ctx: OwoUIGraphics, component: ButtonComponent) {
        var texture = GregUtils.id("category-button-inactive")

        if (component.isHovered) {
            texture = GregUtils.id("category-button-hovered")
        } else if ("category-${ConfigManager.getString("open category")}" == component.id()) {
            texture = GregUtils.id("category-button-active")
        }

        NinePatchTexture.draw(texture, ctx, component)
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
}