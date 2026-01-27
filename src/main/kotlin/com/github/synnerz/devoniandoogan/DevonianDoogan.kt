package com.github.synnerz.devoniandoogan

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.dungeons.clear.BoxStarMob
import com.github.synnerz.devonian.features.dungeons.map.DungeonMap
import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

object DevonianDoogan : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("devoniandoogan")

	override fun onInitializeClient() {
		logger.warn("Initialized Synnerz/DevonianDoogan")
	}

	fun preInit() {
		Devonian.features.add(object : Feature(
			"dooganMap\$renderHiddenRooms",
			"",
			Categories.DUNGEON_MAP,
			"catacombs",
			displayName = "§cRender Hidden Rooms",
			subcategory = "Behavior",
			cheeto = true,
		) {
			override fun setRegistered(b: Boolean) = apply {
				super.setRegistered(b)
				DungeonMap.SETTING_RENDER_HIDDEN_ROOMS = b
			}
		})

		Devonian.features.add(object : Feature(
			"doogan\$renderStarEsp",
			"",
			Categories.DUNGEONS,
			displayName = "§cStarred Mobs Esp",
			subcategory = "Highlights",
			cheeto = true,
		) {
			override fun setRegistered(b: Boolean) = apply {
				super.setRegistered(b)
				BoxStarMob.SETTING_PHASE = b
			}
		})
	}
}