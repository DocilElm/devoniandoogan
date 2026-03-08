package com.github.synnerz.devoniandoogan.features

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

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
        Blocks.BEDROCK,
        Blocks.OBSIDIAN
    )

    fun onBreak(blockPos: BlockPos, blockState: BlockState, block: Block) {
        if (block in blacklist) return
        if (!isEnabled() || Location.area != "catacombs" || Dungeons.inBoss.value) return
        val itemStack = minecraft.player?.mainHandItem ?: return
        if (ItemUtils.skyblockId(itemStack) != "DUNGEONBREAKER") return
        val world = minecraft.level ?: return
        val soundType = blockState.soundType

        world.removeBlock(blockPos, false)
        // not accurate but idc
        world.playLocalSound(
            blockPos,
            soundType.hitSound,
            SoundSource.BLOCKS,
            soundType.volume,
            soundType.pitch,
            false
        )
    }
}