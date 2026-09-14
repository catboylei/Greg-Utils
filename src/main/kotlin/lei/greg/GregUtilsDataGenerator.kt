package lei.greg

import lei.greg.config.ConfigManager
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

// datagenerator for testing config manager
// ignore this for releases

object GregUtilsDataGenerator : DataGeneratorEntrypoint {
	override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
		ConfigManager.initConfig()
	}
}