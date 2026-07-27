package lei.greg.config

import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.StackLayout
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.util.NinePatchTexture
import lei.greg.GregUtils
import java.awt.Button

class ConfigScreen: BaseUIModelScreen<StackLayout>(StackLayout::class.java, DataSource.asset(GregUtils.id("ui-model"))) {

    override fun build(rootComponent: StackLayout) {
        applyCustomTextures(rootComponent)
    }

    private fun applyCustomTextures(rootComponent: StackLayout) {
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "window")!!.surface {ctx, component ->
            NinePatchTexture.draw(GregUtils.id("window"), ctx, component)
        }
        rootComponent.childById<FlowLayout?>(FlowLayout::class.java, "separator")!!.surface {ctx, component ->
            NinePatchTexture.draw(GregUtils.id("separator"), ctx, component)
        }

        rootComponent.forEachDescendant { component ->
            if (component is ButtonComponent && component.id()!!.startsWith("category-")) {
                component.renderer { ctx, component, _ -> buttonRendering(ctx, component) }
                component.onPress { ConfigManager.setOption("open category", component.id()!!) }
            }
        }
    }

    private fun buttonRendering(ctx: OwoUIGraphics, component: ButtonComponent) {
        var texture = GregUtils.id("category-button-inactive")

        if (component.isHovered) {
            texture = GregUtils.id("category-button-hovered")
        } else if (ConfigManager.getString("open category") == component.id()) {
            texture = GregUtils.id("category-button-active")
        }

        NinePatchTexture.draw(texture, ctx, component)
    }
}