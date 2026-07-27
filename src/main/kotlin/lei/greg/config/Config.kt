package lei.greg.config

import lei.greg.GregUtils
import net.fabricmc.loader.api.FabricLoader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.Properties

// owoconfig hates me and so does every other option
// so this is what we are doing now <3

// these are defaults, value will only be applied if no file or missing option
// only keys listed here are kept in the config file (non-matching keys get deleted on init)
private val defaults: Map<String, *> = mapOf(
    "debug mode" to false,
    "highlight color" to 0xFFCEFF,
    "open category" to "category-general"
)

object ConfigManager {
    private val path = FabricLoader.getInstance().configDir.resolve("greg.properties")
    private val properties = Properties()

    fun initConfig() {
        GregUtils.LOGGER.info("loading config")

        // dump all defaults to file if it wasnt generated yet
        if (!Files.exists(path)) {
            GregUtils.LOGGER.info("creating new config file at $path")

            defaults.forEach { (key, value) -> properties.setProperty(key, value.toString()) }
            save()
            return
        }

        properties.load(FileInputStream(path.toFile()))
        // add fields from defaults if they dont exist
        for ((key, value) in defaults) {
            properties.getProperty(key) ?: run { properties.setProperty(key, value.toString()) }
        }
        // remove fields if they arent in defaults
        for ((key, _) in properties.entries) {
            if (!defaults.containsKey(key)) { properties.remove(key) }
        }

        save()
    }

    fun setOption(key: String, value: String) {
        properties.setProperty(key, value)
        save()
    }

    fun getString(key: String): String? = properties.getProperty(key)
    fun getBool(key: String): Boolean = properties.getProperty(key).toBoolean()
    fun getInt(key: String): Int = properties.getProperty(key).toIntOrNull() ?: 0

    private fun save() {
        properties.store(FileOutputStream(path.toFile()), "meaow nrrp mrroww :3")
    }
}