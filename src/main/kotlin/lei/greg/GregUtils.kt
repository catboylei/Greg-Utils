package lei.greg

import lei.greg.config.ConfigManager
import lei.greg.features.TreeHelper
import lei.greg.highlights.Highlights
import net.fabricmc.api.ModInitializer
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object GregUtils : ModInitializer {
	const val MOD_ID: String = "greg-utils"
	val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
	fun id(path: String): Identifier { return Identifier.of(MOD_ID, path) }

	override fun onInitialize() {

		ConfigManager.initConfig()
		Debug.register()
		Highlights.register()

		TreeHelper.register()
	}
}