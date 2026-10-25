package lei.greg

import lei.greg.config.ConfigManager
import lei.greg.features.DiscordChat
import lei.greg.features.TreeHelper
import lei.greg.features.WatchedVisualiser
import lei.greg.highlights.Highlights
import lei.greg.utils.Scheduler
import lei.greg.backend.Backend
import net.fabricmc.api.ModInitializer
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object GregUtils : ModInitializer {
	const val MOD_ID: String = "greg-utils"
	@JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
	const val DISCORD_LINK = "https://discord.gg/CN4GfgXZMm"
	fun id(path: String): Identifier { return Identifier.of(MOD_ID, path) }

	// uses dummy uuid in debug mode for testing in gradle dev env
	val PLAYER_UUID: String by lazy {
		// NOTE: remember to always uncomment this line before pushing
		// MinecraftClient.getInstance().session.uuidOrNull.toString()
		// and comment this one !
		"4a451026-c279-40fa-80de-6fcd02169bc5"
	}

	override fun onInitialize() {

		// utils
		ConfigManager.initConfig()
		Debug.register()
		Highlights.register()
		Scheduler.register()

		// online backend (awawa)
		Backend.register()

		// features
		TreeHelper.register()
		DiscordChat.register()
		WatchedVisualiser.register()
	}
}
