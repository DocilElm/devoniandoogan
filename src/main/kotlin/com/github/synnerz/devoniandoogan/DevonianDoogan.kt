package com.github.synnerz.devoniandoogan

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.dungeons.clear.BoxStarMob
import com.github.synnerz.devonian.features.dungeons.map.DungeonMap
import com.github.synnerz.devoniandoogan.features.AutoCloseChest
import com.github.synnerz.devoniandoogan.features.AutoCombine
import com.github.synnerz.devoniandoogan.features.AutoRefill
import com.github.synnerz.devoniandoogan.features.AutoSell
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.KeyMapping
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory

object DevonianDoogan : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("devoniandoogan")
	val keybindCategory by lazy {
		KeyMapping.Category.register(
			ResourceLocation.fromNamespaceAndPath(
				"devoniandoogan",
				"keybinds"
			)
		)
	}

	override fun onInitializeClient() {
		logger.warn("Initialized Synnerz/DevonianDoogan")
	}

	fun preInit() {
		Devonian.features.add(object : Feature(
			"dooganMap\$renderHiddenRooms",
			"",
			Categories.DUNGEON_MAP,
			"catacombs",
			displayName = "Render Hidden Rooms",
			subcategory = "Behavior",
			cheeto = true,
		) {
			override fun add() {
				super.add()
				DungeonMap.SETTING_RENDER_HIDDEN_ROOMS = true
			}

			override fun remove() {
				super.remove()
				DungeonMap.SETTING_RENDER_HIDDEN_ROOMS = false
			}
		})

		Devonian.features.add(object : Feature(
			"doogan\$renderStarEsp",
			"",
			Categories.DUNGEONS,
			displayName = "Starred Mobs Esp",
			subcategory = "Highlights",
			cheeto = true,
		) {
			override fun add() {
				super.add()
				BoxStarMob.SETTING_PHASE = true
			}

			override fun remove() {
				super.remove()
				BoxStarMob.SETTING_PHASE = false
			}
		})

		Devonian.addFeatureInstance(AutoSell)
		Devonian.addFeatureInstance(AutoCombine)
		Devonian.addFeatureInstance(AutoRefill)
		Devonian.addFeatureInstance(AutoCloseChest)
	}
}