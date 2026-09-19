package com.github.synnerz.devoniandoogan.features

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import kotlinx.atomicfu.atomic
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
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
    private var lastItemStack = atomic(ItemStack.EMPTY)

    fun onBreak(blockPos: BlockPos, blockState: BlockState, block: Block, destroyingItem: ItemStack): Boolean {
        if (block in blacklist) return false
        if (!isEnabled() || Location.area != "catacombs" || Dungeons.inBoss.value) return false
        val heldItem = minecraft.player?.mainHandItem ?: return false
        // soft check
        if (heldItem.item != lastItemStack.value.item) return false
        if (lastItemStack.value.item == Items.DIAMOND_PICKAXE && ItemUtils.skyblockId(lastItemStack.value) != "DUNGEONBREAKER") return true
        else if (ItemUtils.skyblockId(lastItemStack.value) != "DUNGEONBREAKER") return false
        val world = minecraft.level ?: return false
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

        return false
    }

    fun onHeldSlotChange(slot: Int) {
        lastItemStack.value = minecraft.player?.inventory?.getItem(slot) ?: return
    }
}