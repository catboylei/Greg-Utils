package lei.greg

import lei.greg.config.ConfigManager
import lei.greg.features.DiscordChat
import lei.greg.features.TreeHelper
import lei.greg.highlights.Highlights
import lei.greg.utils.Scheduler
import net.fabricmc.api.ModInitializer
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object GregUtils : ModInitializer {
	const val MOD_ID: String = "greg-utils"
	val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
	fun id(path: String): Identifier { return Identifier.of(MOD_ID, path) }

	val PLAYER_UUID: String by lazy {
		//MinecraftClient.getInstance().session.uuidOrNull.toString()
		"4a451026-c279-40fa-80de-6fcd02169bc5"
	}

	override fun onInitialize() {

		ConfigManager.initConfig()
		Debug.register()
		Highlights.register()
		Scheduler.register()

		TreeHelper.register()
		DiscordChat.register()
	}
}