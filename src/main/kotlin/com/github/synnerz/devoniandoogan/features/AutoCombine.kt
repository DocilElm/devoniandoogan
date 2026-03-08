package com.github.synnerz.devoniandoogan.features

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.jvm.optionals.getOrNull

object AutoCombine : Feature(
    "autoCombine",
    "Combines specific enchanted books to their max level (the enchant list is set by the mod)",
    subcategory = "General",
    cheeto = true
) {
    private val SETTING_DELAY = addSlider(
        "delay",
        250.0,
        50.0, 1000.0,
        "The delay for each click (too low might ban)",
        "AutoCombine Delay"
    )
    private val books = mapOf(
        "ultimate_legion" to 5,
        "ultimate_wise" to 5,
        "ultimate_wisdom" to 5,
        "ultimate_last_stand" to 5,
        "ultimate_swarm" to 5,
        "ultimate_soul_eater" to 5,
        "overload" to 5,
        "rejuvenate" to 5,
        "dragon_hunter" to 5,
        "smoldering" to 5,
        "green_thumb" to 5
    )
    private val data = mutableListOf<BookSlot>()
    private val sortedData = mutableMapOf<String, MutableList<BookSlot>>()
    private var inAnvil = false
    private var currentEnchant: String? = null
    private var lastClick = -1L
    private var wasUsed = false

    data class BookSlot(val slot: Int, val enchantName: String, val level: Int, val itemStack: ItemStack)

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            inAnvil = event.titleStr == "Anvil"
            if (!inAnvil) reset()
        }

        on<ServerContainerCloseEvent> {
            reset()
            wasUsed = false
        }

        on<ClientContainerCloseEvent> {
            reset()
            wasUsed = false
        }

        on<ServerContainerSetContentEvent> { event ->
            if (!inAnvil) return@on

            reset()
            event.forEach { idx, itemStack ->
                if (idx < 54) return@forEach
                if (itemStack == null || itemStack.item != Items.ENCHANTED_BOOK) return@forEach
                val extraAttributes = ItemUtils.extraAttributes(itemStack) ?: return@forEach
                val enchs = extraAttributes.getCompound("enchantments")
                if (!enchs.isPresent || enchs.isEmpty) return@forEach
                val enchants = mutableMapOf<String, Int>()

                enchs.get().forEach { k, v ->
                    val level = v.asInt().getOrNull() ?: return@forEach
                    enchants[k] = level
                }
                if (enchants.size > 1) return@forEach
                val ( k, v ) = enchants.entries.first()
                val max = books[k] ?: return@forEach
                if (v == max) return@forEach

                data.add(BookSlot(idx, k, v, itemStack))
            }

            Scheduler.scheduleTask {
                data.forEach { sortedData.getOrPut("${it.enchantName}:${it.level}") { mutableListOf() }.add(it) }
                sortedData.entries.reversed().forEach { (k, v) -> if (v.size < 2) sortedData.remove(k) }
                lastClick = System.currentTimeMillis()
            }
        }

        on<ServerContainerSetSlotEvent> { event ->
            if (currentEnchant == null) return@on
            val idx = event.slot
            if (idx < 54) return@on
            val itemStack = event.itemStack
            if (itemStack.item != Items.ENCHANTED_BOOK) return@on
            val extraAttributes = ItemUtils.extraAttributes(itemStack) ?: return@on
            val enchs = extraAttributes.getCompound("enchantments")
            if (!enchs.isPresent || enchs.isEmpty) return@on
            val enchants = mutableMapOf<String, Int>()

            enchs.get().forEach { k, v ->
                val level = v.asInt().getOrNull() ?: return@forEach
                enchants[k] = level
            }
            if (enchants.size > 1) return@on
            val ( k, v ) = enchants.entries.first()
            val max = books[k] ?: return@on
            if (v == max) return@on

            Scheduler.scheduleTask {
                data.removeIf { it.slot == idx }
                data.add(BookSlot(idx, k, v, itemStack))
                sortedData.clear()
                data.forEach { sortedData.getOrPut("${it.enchantName}:${it.level}") { mutableListOf() }.add(it) }
                sortedData.entries.reversed().forEach { (k, v) -> if (v.size < 2) sortedData.remove(k) }
                lastClick = System.currentTimeMillis()
            }
        }

        on<TickEvent> {
            if (!inAnvil || !canClick()) return@on
            val screen = minecraft.screen ?: return@on
            val container = screen as? AbstractContainerScreen<*> ?: return@on
            if (container.title.string != "Anvil") return@on
            val items = container.menu.items

            val midItem = items.getOrNull(22) ?: return@on
            val isEnchanted = midItem.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)
            val isSign = midItem.item == Items.OAK_SIGN

            if (midItem.item == Items.BARRIER) {
                ChatUtils.sendMessage("&c[&4Doogan&c] &cSeems like the Auto Combine did not get correct books, try re-opening the anvil menu")
                reset()
                wasUsed = false
                return@on
            }
            if (isEnchanted == true || isSign) {
                if (!wasUsed) return@on
                lastClick = System.currentTimeMillis()
                ScreenUtils.click(22, false)
                if (isSign) wasUsed = false
                return@on
            }
            if (data.isEmpty()) return@on
            if (currentEnchant != null && currentEnchant !in sortedData)
                currentEnchant = null

            sortedData.forEach { (k, v) ->
                if (currentEnchant == null) currentEnchant = k

                v.reversed().forEach {
                    val containerItems = container.menu.items
                    if (
                        containerItems.getOrNull(29)?.item == Items.ENCHANTED_BOOK &&
                        containerItems.getOrNull(33)?.item == Items.ENCHANTED_BOOK
                    ) return@forEach
                    val enchantName = it.enchantName
                    val level = it.level
                    if (currentEnchant != "$enchantName:$level") return@forEach
                    if (!canClick()) return@forEach

                    lastClick = System.currentTimeMillis()
                    ScreenUtils.click(it.slot, true)
                    wasUsed = true
                    v.remove(it)
                    if (v.isEmpty()) currentEnchant = null
                }
            }
        }
    }

    private fun canClick(): Boolean =
        lastClick == -1L || System.currentTimeMillis() - lastClick > SETTING_DELAY.get()

    private fun reset() {
        data.clear()
        sortedData.clear()
        currentEnchant = null
    }
}