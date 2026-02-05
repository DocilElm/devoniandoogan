package com.github.synnerz.devoniandoogan.features

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.PersistentJsonClass
import com.github.synnerz.devoniandoogan.DevonianDoogan
import com.google.gson.reflect.TypeToken
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import kotlin.jvm.optionals.getOrNull

object AutoSell : Feature(
    "autoSell",
    "Auto sells set items with the keybinds in controls",
    subcategory = "General",
    cheeto = true
) {
    private val SETTING_DELAY = addSlider(
        "delay",
        250.0,
        50.0, 1000.0,
        "The delay for each click (too low might ban)",
        "Auto Sell Delay"
    )
    private val sellByName = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.devoniandoogan.sellname",
            GLFW.GLFW_KEY_UNKNOWN,
            DevonianDoogan.keybindCategory
        )
    )
    private val sellById = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.devoniandoogan.sellid",
            GLFW.GLFW_KEY_UNKNOWN,
            DevonianDoogan.keybindCategory
        )
    )
    private val namesData = object : PersistentJsonClass<MutableList<String>>(
        "devonian/autosellnames.json",
        object : TypeToken<MutableList<String>>() {}
    ) {
        override fun onLoadDefault() {
            data = mutableListOf()
        }
    }
    private val itemIDData = object : PersistentJsonClass<MutableList<String>>(
        "devonian/autosellids.json",
        object : TypeToken<MutableList<String>>() {}
    ) {
        override fun onLoadDefault() {
            data = mutableListOf()
        }
    }
    private val validGuis = listOf("Ophelia", "Trades", "Booster Cookie")
    private var lastClick = -1L

    override fun initialize() {
        namesData.load()
        itemIDData.load()

        on<GuiKeyDownEvent> { event ->
            if (sellById.matches(event.event)) {
                val itemStack = ScreenUtils.cursorStack(event.screen) ?: return@on
                val sbId = getSkyblockId(itemStack) ?: return@on
                if (itemIDData.data!!.contains(sbId)) {
                    itemIDData.data!!.remove(sbId)
                    ChatUtils.sendMessage("&bAutoSell &cremoved&b ItemID &a$sbId", true)
                    return@on
                }
                itemIDData.data!!.add(sbId)
                ChatUtils.sendMessage("&bAutoSell &aadded&b ItemID &a$sbId", true)
                return@on
            }
            if (!sellByName.matches(event.event)) return@on
            val itemStack = ScreenUtils.cursorStack(event.screen) ?: return@on
            val itemName = getName(itemStack) ?: return@on

            if (namesData.data!!.contains(itemName)) {
                namesData.data!!.remove(itemName)
                ChatUtils.sendMessage("&bAutoSell &cremoved&b Name &a$itemName", true)
                return@on
            }

            namesData.data!!.add(itemName)
            ChatUtils.sendMessage("&bAutoSell &aadded&b Name &a$itemName", true)
        }

        on<TickEvent> {
            val screen = minecraft.screen ?: return@on
            val container = screen as? AbstractContainerScreen<*> ?: return@on
            if (!validGuis.contains(container.title.string)) return@on
            val items = container.menu.items
            if (items.getOrNull(49)?.item == Items.BARRIER) return@on
            if (lastClick != -1L && System.currentTimeMillis() - lastClick < SETTING_DELAY.get()) return@on

            for (idx in 0..items.size) {
                if (idx < 54) continue
                val itemStack = items.getOrNull(idx) ?: continue
                if (itemStack.isEmpty) continue

                if (namesData.data!!.contains(getName(itemStack)) || itemIDData.data!!.contains(getSkyblockId(itemStack))) {
                    if (lastClick != 1L && System.currentTimeMillis() - lastClick < SETTING_DELAY.get()) break

                    lastClick = System.currentTimeMillis()
                    ScreenUtils.click(idx, false)
                    break
                }
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        lastClick = -1L
    }

    private fun getName(itemStack: ItemStack): String? {
        val name = itemStack.customName?.string?.replace("x\\d+".toRegex(), "") ?: return null
        val reforge = ItemUtils.extraAttributes(itemStack)?.getString("modifier") ?: return null
        var ( _, clearName ) = "([A-z' ]+)".toRegex().matchEntire(name)?.groupValues ?: return null

        clearName = clearName.lowercase().replace("\'".toRegex(), "")
        reforge.getOrNull()?.let { clearName = clearName.replace(it, "") }

        return clearName.trim()
    }

    private fun getSkyblockId(itemStack: ItemStack): String? = ItemUtils.skyblockId(itemStack)
}