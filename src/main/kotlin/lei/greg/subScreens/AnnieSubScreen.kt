package lei.greg.subScreens

import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.StackLayout
import io.wispforest.owo.ui.util.NinePatchTexture
import lei.greg.GregUtils
import net.minecraft.client.gui.screen.Screen
import net.minecraft.util.Util
import java.net.URI

class AnnieSubScreen(private val parent: Screen?) : BaseUIModelScreen<StackLayout>(StackLayout::class.java, DataSource.asset(GregUtils.id("annie-subscreen"))) {

    override fun build(rootComponent: StackLayout) {
        hookButtons(rootComponent)
        applyCustomTextures(rootComponent)

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
}