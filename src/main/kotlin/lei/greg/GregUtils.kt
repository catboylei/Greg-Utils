package lei.greg

import lei.greg.backend.Backend
import lei.greg.config.ConfigManager
import lei.greg.config.ConfigScreen
import lei.greg.features.AnnieParties
import lei.greg.features.DiscordChat
import lei.greg.features.TreeHelper
import lei.greg.features.WatchedVisualiser
import lei.greg.highlights.Highlights
import lei.greg.subScreens.AnnieSubScreen
import lei.greg.utils.Scheduler
import net.fabricmc.api.ModInitializer
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Properties

object GregUtils : ModInitializer {
	val isRelease = getReleaseState()

	const val MOD_ID: String = "greg-utils"
	@JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
	const val DISCORD_LINK = "https://discord.gg/CN4GfgXZMm"
	fun id(path: String): Identifier { return Identifier.of(MOD_ID, path) }

	// uses dummy uuid in debug mode for testing in gradle dev env
	val PLAYER_UUID: String by lazy {
		if (isRelease) MinecraftClient.getInstance().session.uuidOrNull.toString() else "4a451026-c279-40fa-80de-6fcd02169bc5"
	}
	val PLAYER_NAME: String by lazy {
		if (isRelease) MinecraftClient.getInstance().session.username else "catboylei"
	}

	override fun onInitialize() {

		// only register debug if not in a release build
		if (!isRelease) {
			Debug.register()
		}

		// utils
		ConfigManager.initConfig()
		Highlights.register()
		Scheduler.register()

		// online backend (awawa)
		Backend.register()

		// features
		TreeHelper.register()
		DiscordChat.register()
		WatchedVisualiser.register()
		AnnieParties.register()

		// screens
		AnnieSubScreen.registerCommand()
		ConfigScreen.registerCommand()
	}
}

fun getReleaseState(): Boolean {
	val p = Properties()
	GregUtils::class.java.getResourceAsStream("/greg-build.properties").use { `in` ->
		p.load(`in`)
	}
	return p.getProperty("isrelease").toBoolean()
}