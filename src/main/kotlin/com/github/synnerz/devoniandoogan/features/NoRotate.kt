package com.github.synnerz.devoniandoogan.features

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.api.events.UseItemEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.jvm.optionals.getOrNull

object NoRotate : Feature(
    "noRotate",
    "",
    Categories.MISC,
    cheeto = true,
) {
    private val SETTING_ROTATION_TIME = addSlider(
        "rotationTime",
        300.0,
        0.0, 3000.0,
        "Rotation limit time in ms",
        "Time Limit"
    )
    private val SETTING_ALLOW_HYPE = addSwitch(
        "allowHype",
        true,
        "Allows hype no rotate",
        "Allow Hype"
    )
    private val SETTING_ALLOW_INSTANT = addSwitch(
        "allowInstantTransmission",
        true,
        "Allows Instant Transmission no rotate",
        "Allow Instant Transmission"
    )
    private val witherBlades = listOf("HYPERION", "VALKYRIE", "SCYLLA", "ASTRAEA")
    private val etherwarps = mutableListOf("ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID", "ETHERWARP_CONDUIT")
    private var lastClick = -1L

    override fun initialize() {
        on<UseItemEvent> {
            if (!shouldAllow()) {
                lastClick = -1
                return@on
            }

            if (!checkItem(minecraft.player!!.mainHandItem)) return@on
            lastClick = System.currentTimeMillis()
        }
    }

    fun shouldAllow(): Boolean {
        if (!isEnabled()) return false
        if (Dungeons.floor.floorNum == 7 && Dungeons.inBoss.value) return false
        val currentRoom = DungeonScanner.currentRoom
        if (currentRoom != null) {
            if (currentRoom.type == RoomTypes.TRAP) return false
            if (currentRoom.roomID == 72 || currentRoom.roomID == 83) return false
        }
        return true
    }

    fun canNoRotate(): Boolean {
        if (!isEnabled() || !shouldAllow()) return false

        return System.currentTimeMillis() - lastClick < SETTING_ROTATION_TIME.get()
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        lastClick = -1L
    }

    private fun checkItem(itemStack: ItemStack): Boolean {
        val itemId = ItemUtils.skyblockId(itemStack) ?: return false
        val extraAttributes = ItemUtils.extraAttributes(itemStack) ?: return false
        val requireSneak = itemStack.item == Items.DIAMOND_SHOVEL || itemStack.item == Items.DIAMOND_SWORD
        val isEther = etherwarps.contains(itemId)
        if (isEther && requireSneak && !SETTING_ALLOW_INSTANT.get()) {
            if (!extraAttributes.contains("ethermerge")) return false
            if (!minecraft.player!!.isSteppingCarefully) return false
        }
        val isHype = witherBlades.contains(itemId)
        if (
            isHype &&
            (extraAttributes.getList("ability_scroll").getOrNull() ?: return false).size < 2
        ) return false
        return isEther || (isHype && SETTING_ALLOW_HYPE.get())
    }
}