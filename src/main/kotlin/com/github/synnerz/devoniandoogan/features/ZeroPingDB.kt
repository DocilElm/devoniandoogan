package com.github.synnerz.devoniandoogan.features

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

object ZeroPingDB : Feature(
    "zeroPingDb",
    "Makes dungeon breaker zero ping",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL",
    cheeto = true
) {
    private val blacklist = listOf(
        Blocks.CHEST,
        Blocks.TRAPPED_CHEST,
        Blocks.LEVER,
        Blocks.COMMAND_BLOCK,
        Blocks.STONE_BUTTON,
        Blocks.PLAYER_HEAD,
        Blocks.BEDROCK
    )

    fun onBreak(blockPos: BlockPos, block: Block) {
        if (block in blacklist) return
        if (!isEnabled() || Location.area != "catacombs" || Dungeons.inBoss.value) return
        val itemStack = minecraft.player?.mainHandItem ?: return
        if (ItemUtils.skyblockId(itemStack) != "DUNGEONBREAKER") return

        minecraft.level?.removeBlock(blockPos, false)
    }
}