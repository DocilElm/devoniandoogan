package com.github.synnerz.devoniandoogan.features

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket

object AutoCloseChest : Feature(
    "autoCloseChest",
    "Automatically closes the secret chests in dungeons",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL",
    cheeto = true
) {
    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundOpenScreenPacket) return@on
            if (Dungeons.inBoss.value) return@on

            val titleStr = packet.title.string
            val containerId = packet.containerId
            if (titleStr != "Chest") return@on

            event.cancel()
            minecraft.connection?.send(ServerboundContainerClosePacket(containerId))
        }
    }
}