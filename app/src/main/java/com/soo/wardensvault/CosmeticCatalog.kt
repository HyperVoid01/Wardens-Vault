package com.soo.wardensvault

//Slots a Warden can equip. Cosmetics never affect any financial calculation.
enum class Slot(val label: String) {
    HELMET("Helmets"),
    ARMOUR("Armour"),
    WEAPON("Weapons"),
    SHIELD("Shields"),
    COLOUR("Warden Colours"),
    BACKGROUND("Dungeon Backgrounds"),
    TITLE("Titles"),
    DECORATION("Decorations")
}

//color is used for COLOUR (tint on the Warden) and BACKGROUND (profile background); 0 = unused
data class Cosmetic(
    val key: String,
    val name: String,
    val slot: Slot,
    val price: Int,
    val minLevel: Int,
    val color: Int = 0
)

object CosmeticCatalog {
    val items: List<Cosmetic> = listOf(
        Cosmetic("helmet_iron", "Iron Helm", Slot.HELMET, 100, 1),
        Cosmetic("helmet_knight", "Knight's Helm", Slot.HELMET, 250, 3),
        Cosmetic("helmet_dragon", "Dragon Helm", Slot.HELMET, 600, 7),

        Cosmetic("armour_leather", "Leather Armour", Slot.ARMOUR, 120, 1),
        Cosmetic("armour_chain", "Chainmail", Slot.ARMOUR, 300, 3),
        Cosmetic("armour_plate", "Plate Armour", Slot.ARMOUR, 700, 8),

        Cosmetic("weapon_wood", "Wooden Sword", Slot.WEAPON, 80, 1),
        Cosmetic("weapon_steel", "Steel Sword", Slot.WEAPON, 250, 3),
        Cosmetic("weapon_flame", "Flame Blade", Slot.WEAPON, 650, 8),

        Cosmetic("shield_wood", "Wooden Shield", Slot.SHIELD, 80, 1),
        Cosmetic("shield_iron", "Iron Shield", Slot.SHIELD, 250, 3),
        Cosmetic("shield_aegis", "Aegis", Slot.SHIELD, 600, 8),

        Cosmetic("colour_crimson", "Crimson", Slot.COLOUR, 150, 1, 0xFFB71C1C.toInt()),
        Cosmetic("colour_azure", "Azure", Slot.COLOUR, 150, 1, 0xFF1565C0.toInt()),
        Cosmetic("colour_emerald", "Emerald", Slot.COLOUR, 150, 1, 0xFF2E7D32.toInt()),
        Cosmetic("colour_purple", "Royal Purple", Slot.COLOUR, 400, 4, 0xFF6A1B9A.toInt()),
        Cosmetic("colour_gold", "Golden", Slot.COLOUR, 800, 10, 0xFFFFC107.toInt()),

        Cosmetic("bg_stone", "Stone Dungeon", Slot.BACKGROUND, 100, 1, 0xFF37474F.toInt()),
        Cosmetic("bg_crimson", "Crimson Keep", Slot.BACKGROUND, 250, 3, 0xFF4A1414.toInt()),
        Cosmetic("bg_emerald", "Emerald Cavern", Slot.BACKGROUND, 250, 3, 0xFF14402A.toInt()),
        Cosmetic("bg_midnight", "Midnight Vault", Slot.BACKGROUND, 500, 6, 0xFF0D1330.toInt()),

        Cosmetic("title_penny", "Penny Pincher", Slot.TITLE, 100, 1),
        Cosmetic("title_hunter", "Treasure Hunter", Slot.TITLE, 300, 5),
        Cosmetic("title_legend", "Vault Legend", Slot.TITLE, 900, 12),

        Cosmetic("deco_torch", "Wall Torch", Slot.DECORATION, 60, 1),
        Cosmetic("deco_banner", "War Banner", Slot.DECORATION, 150, 2),
        Cosmetic("deco_chest", "Treasure Chest", Slot.DECORATION, 400, 5)
    )

    fun byKey(key: String): Cosmetic? = items.firstOrNull { it.key == key }
}
