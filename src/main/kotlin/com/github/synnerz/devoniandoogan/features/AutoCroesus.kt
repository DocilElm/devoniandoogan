package com.github.synnerz.devoniandoogan.features

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.api.dungeon.CroesusListener
import com.github.synnerz.devonian.api.dungeon.CroesusListener.ChestItem
import com.github.synnerz.devonian.api.events.RenderSlotEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devoniandoogan.DevonianDoogan
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.lwjgl.glfw.GLFW
import kotlin.collections.contains
import kotlin.math.roundToInt

// Credits: https://github.com/UnclaimedBloom6/RandomStuff
object AutoCroesus : Feature(
    "autoCroesus",
    "Croesus but auto",
    Categories.DUNGEONS,
    "dungeon hub",
    subcategory = "QOL",
    cheeto = true,
) {
    // TODO: rewrite dv to be able to dynamically do this with just its event system
    // TODO: whitelist/blacklist items
    // TODO: add items bought logger
    private val SETTING_DELAY = addSlider(
        "delay",
        500.0,
        50.0, 1000.0,
        "The delay for each click",
        "Delay"
    )
    private val SETTING_CLICK_CONTAINER = addSwitch(
        "clickContainer",
        true,
        "Automatically clicks the container slots it needs to",
        "Auto Click"
    )
    private val SETTING_USE_DUNGEON_KEY = addSwitch(
        "useDungeonKey",
        true,
        "Allows the use of dungeon key to open second most profitable chest if equal/over the threshold",
        "DungeonKey"
    )
    private val SETTING_DUNGEON_KEY_PROFIT = addSlider(
        "dungeonKeyProfit",
        2.0,
        1.0, 30.0,
        "Dungeon key minimum profit, this is multiplied by 100k so 1 = 100k (for 1M its 10)",
        "DungeonKey Profit Threshold"
    )
    private val SETTING_CLICK_ENTITY = addSwitch(
        "clickEntity",
        true,
        "Clicks the croesus entity if in front (and reach) of the player",
        "Click Entity"
    )
    private val killSwitch = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.devoniandoogan.croesusKillSwitch",
            GLFW.GLFW_KEY_LEFT_SHIFT,
            DevonianDoogan.keybindCategory
        )
    )
    private val specialIds = mapOf(
        // big thank Unclaimed NOOB Six
        "WITHER_SHARD" to "SHARD_WITHER",
        "THORN_SHARD" to "SHARD_THORN",
        "APEX_DRAGON_SHARD" to "SHARD_APEX_DRAGON",
        "POWER_DRAGON_SHARD" to "SHARD_POWER_DRAGON",
        "SCARF_SHARD" to "SHARD_SCARF",
        "NECROMANCERS_BROOCH" to "NECROMANCER_BROOCH",
        "WITHER_SHIELD" to "WITHER_SHIELD_SCROLL",
        "IMPLOSION" to "IMPLOSION_SCROLL",
        "SHADOW_WARP" to "SHADOW_WARP_SCROLL",
        "WARPED_STONE" to "AOTE_STONE",
        "SPIRIT_STONE" to "SPIRIT_DECOY",
    )
    private val runChestRegex = "^(?:Master )?Catacombs - Floor [IV]+$".toRegex()
    private val chestNameRegex = "^(Wood|Gold|Diamond|Emerald|Obsidian|Bedrock)$".toRegex()
    private val enchantedBookRegex = "^Enchanted Book \\(([\\w ]+) ([IV]+)\\)$".toRegex()
    private val essenceRegex = "^(Wither|Undead) Essence x(\\d+)$".toRegex()
    private val costRegex = "^(\\d[\\d,]+) Coins$".toRegex()
    private val chestNames = setOf("Wood", "Gold", "Diamond", "Emerald", "Obsidian", "Bedrock")
    private val chestSlots = listOf(
        10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43,
    )
    private val blacklistedSlots = mutableListOf<Int>()
    private var claiming = false
    private var queCroesus = false // Clicking croesus entity to open
    private var queChest = false // Normal croesus menu chest
    private var queRunChest = false // Inner croesus menu chest (the actual run chest)
    private var quePage = -1
    private var lastClick = -1L
    private var slotToClick = -1
    private var lastPage = -1
    private var currentChest: ClaimingChestInfo? = null
    private val logger = mutableListOf<String>()

    data class ClaimingChestInfo(
        val floor: String,
        val page: Int,
        val slot: Int,
        var chestSlot: Int = -1,
        var skipKismet: Boolean = false,
    )

    override fun initialize() {
        DevonianCommand.command.subcommand("acc") { _, args ->
            val type = args.firstOrNull() as? String?
            if (type.isNullOrEmpty()) {
                ChatUtils.sendMessage("&cAuto Croesus please provide a valid &6TYPE")
                return@subcommand 0
            }
            when (type) {
                "go" -> {
                    logger.add("force enabled")
                    claiming = true
                    ChatUtils.sendMessage("&cAuto Croesus &aactivated")
                }
                "reset" -> {
                    logger.add("force reset")
                    reset()
                    ChatUtils.sendMessage("&cAuto Croesus &areset")
                }
                "copylog" -> minecraft.keyboardHandler.clipboard = logger.joinToString("\n")
            }
            1
        }
            .string("TYPE")
            .suggest("TYPE", *listOf("go", "reset", "copylog").toTypedArray())

        on<TickEvent> {
            if (killSwitch.isDown && claiming) {
                logger.add("kill switch activated")
                reset()
                ChatUtils.sendMessage("&cAuto Croesus kill switch was activated &7(if you wish to change the keybind, head over to Minecraft Control settings)")
                return@on
            }
            onClickTick()
            onCroesusTick()
            onClaimingTick()
            onRunChestTick()
            onChestTick()
        }

        on<RenderSlotEvent> { event ->
            // Highlight the slot to click, mostly for debugging
            if (event.isInventory()) return@on
            val slot = event.slot
            if (slot.containerSlot != slotToClick) return@on

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, -1)
        }.prio = 50
    }

    private fun onClickTick() {
        if (slotToClick == -1 || System.currentTimeMillis() - lastClick < SETTING_DELAY.get()) return

        val container = container() ?: return
        if (container.menu.items.size <= slotToClick) return

        if (!SETTING_CLICK_CONTAINER.get()) {
            logger.add("should click $slotToClick")
            ChatUtils.sendMessage("click slot $slotToClick")
            slotToClick = -1
            return
        }

        logger.add("clicking $slotToClick with containerName(${container.title.string})")
        ScreenUtils.click(slotToClick)
        lastClick = System.currentTimeMillis()
        slotToClick = -1
    }

    private fun onCroesusTick() {
        if (!inCroesus() || !claiming || queRunChest) return
        queCroesus = false
        val container = container() ?: return
        if (!invLoaded(container)) return
        val page = page()
        if (page == -1 || quePage != -1 && quePage != page) {
            logger.add("returning because page was unexpected $page - $quePage")
            return
        }
        logger.add("onCroesusTick($page)")

        if (currentChest != null && currentChest!!.slot != -1) {
            if (page != currentChest!!.page) {
                if (lastPage == page) return

                lastPage = page
                slotToClick = 53
                return
            }

            lastPage = -1
            slotToClick = currentChest!!.slot
            queRunChest = true
            return
        }

        val ( slot, floor ) = unopenedChest(page)
        if (slot != null && floor != null) {
            currentChest = ClaimingChestInfo(floor, page, slot)

            logger.add("clicking unopened chest $slot - $currentChest")
            queRunChest = true
            slotToClick = slot
            return
        }

        val items = container.menu.items
        if (items.getOrNull(53)?.item == Items.ARROW) {
            lastPage = page
            slotToClick = 53
            quePage = page + 1
            return
        }

        ChatUtils.sendMessage("&cAuto Croesus &aall chests looted")
        logger.add("All chests done")
        reset()
        if (minecraft.screen != null) {
            val container = container() ?: return
            minecraft.connection?.send(ServerboundContainerClosePacket(container.menu.containerId))
            minecraft.setScreen(null)
        }
    }

    private fun onClaimingTick() {
        if (!claiming || queCroesus) return

        if (container() != null) return

        if (queRunChest || queChest) {
            logger.add("onClaimingTick is out of sync")
            ChatUtils.sendMessage("&cAuto Croesus out of sync resetting.")
            reset()
            return
        }

        startClaiming()
    }

    private fun onRunChestTick() {
        if (!claiming) {
            reset()
            return
        }
        if (!inRunChest()) return
        val container = container() ?: return
        if (!invLoaded(container) || queChest) return

        queRunChest = false
        lastPage = -1
        quePage = -1

        if (currentChest != null && currentChest!!.chestSlot != -1) {
            queChest = true
            slotToClick = currentChest!!.chestSlot
            currentChest!!.chestSlot = -1
            logger.add("clicking early $slotToClick return")
            return
        }

        val items = container.menu.items
        val data = mutableListOf<CroesusListener.ChestData>()

        for (idx in 0..27) {
            val itemStack = items.getOrNull(idx) ?: continue
            val name = itemStack.customName?.string ?: continue
            if (!name.matches(chestNameRegex)) {
                logger.add("skipping item $name")
                continue
            }
            val lore = ItemUtils.lore(itemStack)
            if (lore == null) {
                logger.add("skipping item $name because lore was null")
                continue
            }
            val formattedLore = ItemUtils.lore(itemStack, true)
            if (formattedLore == null) {
                logger.add("skipping item $name because formatted lore was null")
                continue
            }
            val cache = CroesusListener.ChestData(name)
            cache.slot = idx

            for (jdx in 0..lore.lastIndex) {
                val line = lore.getOrNull(jdx) ?: continue
                if (line == "Contents" || line.isBlank()) continue
                if (line == "Already opened!") {
                    cache.purchased = true
                    break
                }

                if (line == "Cost") {
                    val chestPriceLore = lore[jdx + 1]
                    val possibleKey = lore[jdx + 2]
                    var price = costRegex
                        .matchEntire(chestPriceLore)
                        ?.groupValues
                        ?.drop(1)
                        ?.getOrNull(0)
                        ?.replace(",", "")
                        ?.toIntOrNull() ?: 0
                    if (possibleKey == "Dungeon Chest Key") {
                        price += SkyblockPrices.buyPrice("DUNGEON_CHEST_KEY").roundToInt()
                        cache.requiresKey = true
                    }

                    cache.price = price
                    break
                }

                val loreName = formattedLore[jdx]
                val enchantMatch = enchantedBookRegex.matchEntire(line)?.groupValues?.drop(1)
                if (enchantMatch != null) {
                    val name = enchantMatch[0]
                    val numeral = enchantMatch[1]
                    val tier = StringUtils.parseRoman(numeral)
                    val cleanName = name.replace(" ", "_").uppercase()
                    var price = SkyblockPrices.buyPrice("ENCHANTMENT_${cleanName}_$tier").roundToInt()
                    val itemId = if (price == 0) "ENCHANTMENT_ULTIMATE_${cleanName}_$tier" else "ENCHANTMENT_${cleanName}_$tier"
                    if (price == 0)
                        price = SkyblockPrices.buyPrice("ENCHANTMENT_ULTIMATE_${cleanName}_$tier").roundToInt()

                    cache.items.add(ChestItem(itemId, loreName, price, book = true))
                    continue
                }

                val essenceMatch = essenceRegex.matchEntire(line)?.groupValues?.drop(1)
                if (essenceMatch != null) {
                    val type = essenceMatch[0].uppercase()
                    val amount = essenceMatch[1].toIntOrNull() ?: continue
                    val price = SkyblockPrices.buyPrice("ESSENCE_$type")

                    cache.items.add(ChestItem("ESSENCE_$type", loreName, price.roundToInt(), amount, true))
                    continue
                }

                var itemId = line
                    .uppercase()
                    .replace("- ", "")
                    .replace("'", "")
                    .replace(" ", "_")
                if (itemId in specialIds) itemId = specialIds[itemId]!!

                val price = SkyblockPrices.buyPrice(itemId).roundToInt()

                if (price == 0) {
                    logger.add("ItemId not found name=\"$itemId\", line=\"$line\"")
                    continue
                }

                cache.items.add(ChestItem(itemId, loreName, price))
            }

            data.add(cache)
        }

        if (!claiming) return
        val sorted = data.map { it to it.totalProfit(true) }.sortedByDescending { it.second }
        val mostProfitable = sorted.firstOrNull()?.let { if (it.second <= 0) null else it.first }
        if (mostProfitable == null) {
            logger.add("could not find most profitable chest || $data || $sorted\n")
            currentChest?.let { blacklistedSlots.add(it.slot + (it.page - 1) * 54) }
            currentChest = null
            slotToClick = 30
            return
        }
        val secondProfitable = sorted.getOrNull(1)?.let { if (it.second <= 0) null else it.first }
        if (secondProfitable != null && SETTING_USE_DUNGEON_KEY.get() && secondProfitable.totalProfit(true) >= SETTING_DUNGEON_KEY_PROFIT.get() * 100_000) {
            ChatUtils.sendMessage("&cAuto Croesus &ausing &9Dungeon Chest Key&a on ${secondProfitable.name}")
            currentChest?.chestSlot = secondProfitable.slot
        }
        // TODO: bedrock + kismet check
        slotToClick = mostProfitable.slot
        queChest = true
        currentChest?.let { blacklistedSlots.add(it.slot + (it.page - 1) * 54) }
    }

    private fun onChestTick() {
        if (!queChest) return
        val container = container() ?: return
        if (!invLoaded(container) || container.menu.items.size < 32) return
        if (!chestNames.contains(container.title.string)) return

//        val items = container.menu.items
        queChest = false
        // TODO: bedrock + kismet check
        slotToClick = 31
        if (currentChest?.chestSlot == -1) currentChest = null
    }

    private fun startClaiming() {
        claiming = true

        if (!clickCroesus()) {
            claiming = false
            ChatUtils.sendMessage("&cAuto Croesus entity too far away?")
            logger.add("entity too far ?")
            reset()
            return
        }

        queCroesus = true
    }

    private fun clickCroesus(): Boolean {
        // yes this can be more efficiently made but idc enough
        if (!SETTING_CLICK_ENTITY.get()) {
            ChatUtils.sendMessage("&cAuto Croesus &ashould click croesus entity")
            return true
        }
        val world = minecraft.level
        if (world == null) {
            logger.add("world was null")
            return false
        }
        val player = minecraft.player
        if (player == null) {
            logger.add("player was null")
            return false
        }
        val hitResult = minecraft.hitResult
        if (hitResult == null) {
            logger.add("hitResult was null")
            return false
        }
        if (hitResult.type != HitResult.Type.ENTITY) {
            logger.add("hitResult was not Entity type")
            return false
        }
        val entity = (hitResult as? EntityHitResult)?.entity
        if (entity == null) {
            logger.add("hitResult entity was null")
            return false
        }
        if (entity.uuid.version() != 2) {
            logger.add("entity uuid version was not 2 \"${entity.uuid.version()}\"")
            return false
        }
        val displayEntity = world.getEntities(entity, entity.boundingBox.expandTowards(1.0, 1.0, 1.0))?.firstOrNull()
        if (displayEntity == null || displayEntity.x - entity.x != 0.0 || displayEntity.y - entity.y != 0.0 || displayEntity.z - entity.z != 0.0) {
            logger.add("croesus display name was not found properly")
            return false
        }
        if (displayEntity.customName?.string != "Croesus") {
            logger.add("display entity was not named croesus \"${displayEntity.customName?.string}\"")
            return false
        }
        val connection = minecraft.connection
        if (connection == null) {
            logger.add("connection was null")
            return false
        }
        connection.send(ServerboundInteractPacket.createInteractionPacket(
            entity,
            player.isShiftKeyDown,
            InteractionHand.MAIN_HAND,
            Vec3(0.0, 0.0, 0.0)
        ))
        logger.add("left clicking croesus")
        return true
    }

    private fun page(): Int {
        val container = container() ?: return -1
        val items = container.menu.items

        val prevArrow = items.getOrNull(45) ?: return -1
        val nextArrow = items.getOrNull(53) ?: return -1
        val isNext = nextArrow.item == Items.ARROW

        if (isNext || prevArrow.item == Items.ARROW) {
            val itemStack = if (isNext) nextArrow else prevArrow
            val lore = ItemUtils.lore(itemStack)?.firstOrNull()
            if (lore == null) {
                logger.add("lore for itemStack($itemStack) was null in pages")
                return -1
            }
            val match = "^Page (\\d+)$".toRegex().matchEntire(lore)
            if (match == null) {
                logger.add("regex did not match $lore in pages")
                return -1
            }
            val page = match.groupValues.getOrNull(1)?.toIntOrNull()
            if (page == null) {
                logger.add("could not get page int in pages ${match.groupValues}")
                return -1
            }

            return page + (if (isNext) -1 else 1)
        }

        return 1
    }

    private fun unopenedChest(page: Int): Pair<Int?, String?> {
        val items = container()?.menu?.items
        if (items == null) {
            logger.add("unopenedChest(containerNull)")
            return null to null
        }

        for (idx in chestSlots) {
            val jdx = idx + (page - 1) * 54
            if (blacklistedSlots.contains(jdx)) {
                logger.add("skipping blacklisted jdx $jdx")
                continue
            }
            val itemStack = items.getOrNull(idx)
            if (itemStack == null) {
                logger.add("no itemStack found at $idx")
                continue
            }
            if (itemStack.item != Items.PLAYER_HEAD) {
                logger.add("itemStack($itemStack) at $idx is not a playerHead")
                continue
            }
            val dungeonType = itemStack.customName?.string
            if (dungeonType == null) {
                logger.add("itemStack dungeonType is null at $idx")
                continue
            }
            val lore = ItemUtils.lore(itemStack)
            if (lore == null) {
                logger.add("itemStack lore is null at $idx")
                continue
            }
            val floor = lore.firstOrNull()
            if (floor == null) {
                logger.add("itemStack floor is null at $idx")
                continue
            }
            val isMM = dungeonType == "Master Mode The Catacombs"
            val match = "^Floor (\\w+)$".toRegex().matchEntire(floor)?.groupValues?.firstOrNull()
            if (!lore.any { it == "No chests opened yet!" }) {
                logger.add("$idx - $jdx has already been looted")
                continue
            }
            if (match == null) {
                logger.add("blacklisting $idx - $jdx - $dungeonType - $match - $lore")
                blacklistedSlots.add(jdx)
                break
            }
            val floorStr = "${if (isMM) "M" else "F"}${StringUtils.parseRoman(match)}"
            logger.add("returning idx($idx) with jdx($jdx) and floor($floorStr)")
            return idx to floorStr

            // TODO: kismet blacklist?
        }

        logger.add("unopenedChest(null)")
        return null to null
    }

    private fun inCroesus(): Boolean = minecraft.screen?.title?.string == "Croesus"

    private fun inRunChest(): Boolean = minecraft.screen?.title?.string?.matches(runChestRegex) ?: false

    private fun container() = minecraft.screen as? AbstractContainerScreen<*>

    private fun invLoaded(container: AbstractContainerScreen<*>?): Boolean {
        val inv = container ?: container() ?: return false
        val items = inv.menu.items
        logger.add("container(${inv.title.string}, ${items.size}, ${items.size - 45}, ${items.getOrNull(items.size - 45)})")
        return items.size > 45 && !(items.getOrNull(items.size - 45)?.isEmpty ?: true)
    }

    private fun reset() {
        claiming = false
        queCroesus = false
        queChest = false
        queRunChest = false
        quePage = -1
        lastClick = -1L
        slotToClick = -1
        lastPage = -1
        currentChest = null
        blacklistedSlots.clear()
    }
}