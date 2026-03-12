package com.github.synnerz.devoniandoogan.features

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.NameChangeEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.network.chat.ClickEvent
import net.minecraft.world.entity.LivingEntity
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object TrevorESP : Feature(
    "trevorESP",
    "mushroom desert, trevor esp, trevor, tyler.",
    Categories.MISC,
    "the farming islands",
    cheeto = true,
) {
    private val mobNames = listOf("Trackable", "Untrackable", "Undetected", "Endangered", "Elusive")
    private val mobDeadRegex = "^Return to the Trapper soon to get a new animal to hunt!$".toRegex()
    private val desertRegex = "^\\[NPC] Trevor: You can find your \\w+ animal near the Desert Settlement\\.$".toRegex()
    private val entityIds = CopyOnWriteArrayList<Int>()
    private val entities = mutableListOf<LivingEntity>()

    override fun initialize() {
        on<ChatEvent> { event ->
            if (
                event.message.contains("Accept the trapper's task to hunt the animal?") &&
                event.message.contains("Click an option: [YES] - [NO]")
            ) {
                val component = event.text
                component.siblings.forEach {
                    val clickEvent = it.style.clickEvent as? ClickEvent.RunCommand? ?: return@forEach
                    val clickAction = clickEvent.action()
                    if (clickAction != ClickEvent.Action.RUN_COMMAND) return@forEach
                    val command = clickEvent.command
                    if (!command.endsWith("YES")) return@forEach
                    Scheduler.scheduleTask { ChatUtils.command(command.replace("/", "")) }
                    return@on
                }
                return@on
            }
            if (event.matches(desertRegex) != null) {
                Scheduler.scheduleTask { ChatUtils.command("warp desert") }
                return@on
            }
            event.matches(mobDeadRegex) ?: return@on
            Scheduler.scheduleTask { ChatUtils.command("warp trapper") }
        }

        on<NameChangeEvent> { event ->
            val name = event.name
            if (!mobNames.any { name.contains("$it ") }) return@on

            entityIds.add(event.entityId)
        }

        on<TickEvent> {
            val world = minecraft.level ?: return@on

            entityIds.forEach { id ->
                val entity = world.getEntity(id - 1) as? LivingEntity? ?: return@forEach
                if (!entities.contains(entity))
                    entities.add(entity)
            }
        }

        on<RenderWorldEvent> {
            entities.removeIf { ent ->
                if (ent.isDeadOrDying || ent.isRemoved) return@removeIf true

                val pos = ent.getPosition(minecraft.deltaTracker.getGameTimeDeltaPartialTick(false))

                Render3DImmediate.renderWireframeBox(
                    pos.x,
                    pos.y,
                    pos.z,
                    ent.bbWidth.toDouble(), ent.bbHeight.toDouble(),
                    Color.GREEN,
                    phase = true,
                    lineWidth = 1.5,
                    centered = true,
                )
                Render3DImmediate.renderFilledBox(
                    pos.x,
                    pos.y,
                    pos.z,
                    ent.bbWidth.toDouble(), ent.bbHeight.toDouble(),
                    Color(0, 255, 0, 80),
                    phase = true,
                    centered = true,
                )
                Render3DImmediate.renderTracer(
                    ent.x, ent.y + 1.0, ent.z,
                    Color.GREEN, lineWidth = 2.0,
                )

                false
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        entityIds.clear()
        entities.clear()
    }
}