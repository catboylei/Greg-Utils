package lei.greg.config

import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.container.StackLayout
import lei.greg.GregUtils

class ConfigScreen: BaseUIModelScreen<StackLayout>(StackLayout::class.java, DataSource.asset(GregUtils.id("ui-model"))) {

    override fun build(rootComponent: StackLayout) {

    }
}