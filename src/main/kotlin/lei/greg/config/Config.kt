package lei.greg.config

import lei.greg.GregUtils
import lei.greg.backend.Backend
import lei.greg.subScreens.AnnieSubScreen
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.*

// custom persistent storage config manager using .properties
// i did this because owoconfig hates me personally

// these are defaults, value will only be applied if no file or missing option
// only keys listed here are kept in the config file (non-matching keys get deleted on init)
private val defaults: Map<String, *> = mapOf(
    // internal
    "open category" to "General Settings",

    // linked to gui entries
    "master toggle" to true,
    "debug mode" to false,
    "highlight color" to "#FFCEFF",

    "tree route highlight" to false,
    "avoid high exit" to true,

    "fkl discord bridge" to false,
    "fkl password" to "",
    "raid pings" to true,
    "bridge chat" to true,
    "use unsecured socket" to false,
    "awawa url" to null,

    "watched visualiser" to false,
    "watched chat" to false,
    "watched title" to true,

    "annie role" to "NONE",
    "have scrolls" to false,
    "can lead" to false
)


data class SettingEntry(val type: String, val title: String, val desc: String, val configId: String, val category: String)

// define entries for the config here
object ScreenEntries {
    val entries = listOf(
        // general/general
        SettingEntry("separator", "General", "", "", "General Settings"),
        SettingEntry("bool", "Enable Greg Utils", "Toggles whether the mod should be \nactive or not", "master toggle", "General Settings"),

        // general/fkl
        SettingEntry("separator", "FKL", "", "", "General Settings"),
        SettingEntry("field", "FKL Bridge Password", "Your personal FKL password \nrequest with \"f!resetPassword\"", "fkl password", "General Settings"),
        SettingEntry("button", "Backend Connection", "Status: N/A", "reconnect backend", "General Settings"),
        SettingEntry("bool", "Use unsecured socket", "\"less secure\" \ndoesnt disconnect you", "use unsecured socket", "General Settings"),

        // general/other
        SettingEntry("separator", "Other", "", "", "General Settings"),
        SettingEntry("field", "Highlight Color", "Color of highlights in hex and \ndefaults to \"#FFCEFF\"", "highlight color", "General Settings"),
        SettingEntry("bool", "Debug Mode", "This breaks stuff badly (obviously) \nOnly useful for debug/dev", "debug mode", "General Settings"),
        SettingEntry("field", "Custom Awawa url", "Custom ip if you want to host \nour lord and saviour locally", "awawa url", "General Settings"),

        // tree/tree path
        SettingEntry("separator", "Tree Path", "", "", "Weeping Soulroot (Tree) Room"),
        SettingEntry("bool", "Highlight Next Door", "", "tree route highlight", "Weeping Soulroot (Tree) Room"),
        SettingEntry("bool", "Avoid High Exit", "Avoid high door from tree entrance \ndefaults to true", "avoid high exit", "Weeping Soulroot (Tree) Room"),

        // random/fkl bridge
        SettingEntry("separator", "FKL Bridge", "", "", "Random"),
        SettingEntry("bool", "FKL Bridge", "bridge to fkl discord \nneed to be in the guild and linked", "fkl discord bridge", "Random"),
        SettingEntry("bool", "Raid Pings", "whether to show raid pings \nfrom discord", "raid pings", "Random"),
        SettingEntry("bool", "Show Chat", "whether to display messages \nfrom discord chats", "bridge chat", "Random"),

        // random/fkl annie
        SettingEntry("separator", "FKL Annie", "", "", "Random"),
        SettingEntry("button", "Annie Parties", "View parties and sign up --->", "open annie", "Random"),
        SettingEntry("field", "Annie Role", "Your role for annie \neither healer, tank, or dps", "annie role", "Random"),
        SettingEntry("bool", "Have scrolls", "Are you bringing scrolls ?", "have scrolls", "Random"),
        SettingEntry("bool", "Can Lead", "Can you lead a party ?", "can lead", "Random"),

        // boss/watched
        SettingEntry("separator", "Watched", "", "", "Gregory Bossfight"),
        // i wanted to make this a dropdown with "none, chat, title, both" but im scawed
        SettingEntry("bool", "Watched visualiser", "Should watched sounds be displayed \nas visual cue?", "watched visualiser", "Gregory Bossfight"),
        SettingEntry("bool", "Watched in chat", "Shows watched cue as chat message", "watched chat", "Gregory Bossfight"),
        SettingEntry("bool", "Watched title", "Shows watched cue as biiig title", "watched title", "Gregory Bossfight"),
    )
}

// ties button configIds to the function they should execute
object ConfigActions {
    val actions: Map<String, () -> Unit> = mapOf(
        "reconnect backend" to  { if (Backend.status != Text.literal("CONNECTING...").formatted(Formatting.YELLOW)) Backend.reconnect() },
        "open annie" to { val client = MinecraftClient.getInstance(); client.setScreen(AnnieSubScreen(client.currentScreen))}
    )
}

object ConfigManager {
    private val path = FabricLoader.getInstance().configDir.resolve("greg.properties") // file to use
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

        GregUtils.LOGGER.info("saved key $key to value $value")
    }

    fun getString(key: String): String? {
        val value = properties.getProperty(key)
        return if (value == "null") null else value // for some reason properties saves null as a string and then gives you a "null" string lol
    }
    fun getBool(key: String): Boolean = properties.getProperty(key).toBoolean()
    fun getInt(key: String): Int = properties.getProperty(key).toIntOrNull() ?: 0

    private fun save() {
        properties.store(FileOutputStream(path.toFile()), "meaow nrrp mrroww :3")
    }
}
