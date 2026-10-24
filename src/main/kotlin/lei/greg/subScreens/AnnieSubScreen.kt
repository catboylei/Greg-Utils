package lei.greg.subScreens

import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.container.StackLayout
import lei.greg.GregUtils
import net.minecraft.client.gui.screen.Screen

class AnnieSubScreen(private val parent: Screen?) : BaseUIModelScreen<StackLayout>(StackLayout::class.java, DataSource.asset(GregUtils.id("annie-subscreen"))) {

    override fun build(rootComponent: StackLayout) {
        //rootComponent.childById(ButtonComponent::class.java, "back")?.onPress { close() }
    }

    override fun close() {
        client?.setScreen(parent)
    }
}