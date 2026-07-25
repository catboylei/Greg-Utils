package lei.greg

import net.fabricmc.api.ModInitializer
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object GregUtils : ModInitializer {
	const val MOD_ID: String = "greg-utils"
	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	fun id(path: String): Identifier { return Identifier.of(MOD_ID, path) }

	override fun onInitialize() {

		LOGGER.info("big paws")
	}
}