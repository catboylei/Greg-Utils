package lei.greg.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

// this makes the mod menu config button actually lead to the custom ui screen
class GregUtilsModMenuApi : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return { ConfigScreen() }
    }
}