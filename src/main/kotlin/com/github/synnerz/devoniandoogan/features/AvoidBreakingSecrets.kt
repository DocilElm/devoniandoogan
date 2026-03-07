package com.github.synnerz.devoniandoogan.features

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.entity.SkullBlockEntity
import net.minecraft.world.level.block.state.BlockState

object AvoidBreakingSecrets : Feature(
    "avoidBreakingSecrets",
    "Stops you from breaking secret chests/levers/essence",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL",
    cheeto = true
) {
    var shouldAvoid = false

    fun avoid(blockPos: BlockPos, blockState: BlockState, block: Block): Boolean {
        if (!isEnabled() || Location.area != "catacombs" || Dungeons.inBoss.value) return false
        val itemStack = minecraft.player?.mainHandItem ?: return false
        if (ItemUtils.skyblockId(itemStack) != "DUNGEONBREAKER") return false
        if (block == Blocks.PLAYER_HEAD && blockState.hasBlockEntity()) {
            val entityBlock = minecraft.level?.getBlockEntity(blockPos) ?: return false
            if (entityBlock.type != BlockEntityType.SKULL) return false
            val skullBlock = entityBlock as SkullBlockEntity
            val owner = skullBlock.ownerProfile ?: return false
            val id = owner.partialProfile().id ?: return false

            return "$id" == "e0f3e929-869e-3dca-9504-54c666ee6f23"
        }

        return block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.LEVER
    }
}