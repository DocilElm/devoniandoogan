package com.github.synnerz.devoniandoogan.features

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature

object AutoRefill : Feature(
    "autoRefill",
    "Refills your pearl/leaps/superbooms/decoy/jerry",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL"
) {
    private val SETTING_ON_MORT_ONLY = addSwitch(
        "onMortOnly",
        true,
        "Runs the commands only when mort starts",
        "Only Mort"
    )
    private val SETTING_PEARLS = addSwitch(
        "pearls",
        true,
        "Does /gfs pearls",
        "Pearls"
    )
    private val SETTING_LEAPS = addSwitch(
        "leaps",
        true,
        "Does /gfs spirit leap",
        "Leaps"
    )
    private val SETTING_TNT = addSwitch(
        "tnt",
        true,
        "Does /gfs superboomtnt",
        "SuperBoomTNT"
    )
    private val SETTING_DECOY = addSwitch(
        "decoy",
        false,
        "Does /gfs decoy",
        "Decoy"
    )
    private val SETTING_JERRY = addSwitch(
        "jerry",
        false,
        "jerry.",
        "Jerry"
    )
    private val SETTING_THRESHOLD = addSlider(
        "threshold",
        0.0,
        0.0, 100.0,
        "only runs after mort started. if pearls are 16 max if 50% is set here it'll be 8 if below this it'll send /gfs pearls",
        "Threshold"
    )
    private val mortRegex = "^\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.$".toRegex()
    private val commandQue = CommandQue()
    private var lastCommand = -1L
    private var hasStarted = false

    class CommandQue {
        private val _que = mutableListOf<String>()

        fun add(command: String) {
            if (_que.contains(command)) return
            _que.add(command)
        }

        fun isEmpty(): Boolean = _que.isEmpty()

        fun removeLast(): String = _que.removeLast()

        fun clear() = _que.clear()
    }

    override fun initialize() {
        on<ChatEvent> { event ->
            event.matches(mortRegex) ?: return@on
            hasStarted = true
            if (!SETTING_ON_MORT_ONLY.get()) return@on

            findCommands(0)
        }

        on<TickEvent> {
            commands()

            if (!hasStarted) return@on
            val percent = SETTING_THRESHOLD.get().toInt()
            if (percent < 1) return@on

            findCommands(percent)
        }
    }

    private fun commands() {
        if (commandQue.isEmpty()) return
        if (lastCommand != -1L && System.currentTimeMillis() - lastCommand < 2100) return

        val cmd = commandQue.removeLast()
        lastCommand = System.currentTimeMillis()
        ChatUtils.command(cmd)
    }

    private fun findCommands(percent: Int) {
        val inv = minecraft.player?.inventory ?: return
        // nobody cares for optimization in cheats anyways
        val pearls = if (!SETTING_PEARLS.get()) 0 else {
            var stacks = 0
            var amount = 0
            inv.forEach {
                if (ItemUtils.skyblockId(it) != "ENDER_PEARL") return@forEach
                stacks++
                amount += it.count
            }
            if (amount == 0) stacks = 1
            stacks * 16 - amount
        }
        val leaps = if (!SETTING_LEAPS.get()) 0 else {
            var stacks = 0
            var amount = 0
            inv.forEach {
                if (ItemUtils.skyblockId(it) != "SPIRIT_LEAP") return@forEach
                stacks++
                amount += it.count
            }
            if (amount == 0) stacks = 1
            stacks * 16 - amount
        }
        val tnt = if (!SETTING_TNT.get()) 0 else {
            var stacks = 0
            var amount = 0
            inv.forEach {
                if (ItemUtils.skyblockId(it) != "SUPERBOOM_TNT") return@forEach
                stacks++
                amount += it.count
            }
            if (amount == 0) stacks = 1
            stacks * 64 - amount
        }
        val decoy = if (!SETTING_DECOY.get()) 0 else {
            var stacks = 0
            var amount = 0
            inv.forEach {
                if (ItemUtils.skyblockId(it) != "DUNGEON_DECOY") return@forEach
                stacks++
                amount += it.count
            }
            if (amount == 0) stacks = 1
            stacks * 64 - amount
        }
        val jerry = if (!SETTING_JERRY.get()) 0 else {
            var stacks = 0
            var amount = 0
            inv.forEach {
                if (ItemUtils.skyblockId(it) != "INFLATABLE_JERRY") return@forEach
                stacks++
                amount += it.count
            }
            if (amount == 0) stacks = 1
            stacks * 64 - amount
        }

        if (pearls > 0 && (percent == 0 || pearls < 16 * percent))
            commandQue.add("gfs ender pearl $pearls")
        if (leaps > 0 && (percent == 0 || leaps < 16 * percent))
            commandQue.add("gfs spirit leap $leaps")
        if (tnt > 0 && (percent == 0 || tnt < 64 * percent))
            commandQue.add("gfs superboom tnt $tnt")
        if (decoy > 0 && (percent == 0 || decoy < 64 * percent))
            commandQue.add("gfs decoy $decoy")
        if (jerry > 0 && (percent == 0 || jerry < 64 * percent))
            commandQue.add("gfs inflatable jerry $jerry")
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        hasStarted = false
        commandQue.clear()
    }
}