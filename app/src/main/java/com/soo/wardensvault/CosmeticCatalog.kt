package com.soo.wardensvault

//Slots a Warden can equip. Cosmetics never affect any financial calculation.
enum class Slot(val label: String) {
    HELMET("Helmets"),
    ARMOUR("Armour"),
    WEAPON("Weapons"),
    SHIELD("Shields"),
    COLOUR("Warden Colours"),
    BACKGROUND("Bedroom Themes"),
    TITLE("Titles"),
    DECORATION("Bedroom Decorations")
}

/*
One item in the Armory.
  drawableRes - the picture layered onto the knight (armour) or placed in the bedroom (decorations)
  color       - COLOUR: tint on the Warden; BACKGROUND: tint over the bedroom (0 = unused)
The keys never change, so items bought before keep working.
 */
data class Cosmetic(
    val key: String,
    val name: String,
    val slot: Slot,
    val price: Int,
    val minLevel: Int,
    val color: Int = 0,
    val drawableRes: Int = 0
)

object CosmeticCatalog {
    val items: List<Cosmetic> = listOf(
        //three armour sets: Crucible (starter), Death Knight (mid), Maliketh (late)
        Cosmetic("helmet_iron", "Crucible Helm", Slot.HELMET, 100, 1, drawableRes = R.drawable.gear_crucible_helm),
        Cosmetic("helmet_knight", "Death Knight Helm", Slot.HELMET, 250, 3, drawableRes = R.drawable.gear_deathknight_helm),
        Cosmetic("helmet_dragon", "Maliketh Helm", Slot.HELMET, 600, 7, drawableRes = R.drawable.gear_maliketh_helm),

        Cosmetic("armour_leather", "Crucible Armour", Slot.ARMOUR, 120, 1, drawableRes = R.drawable.gear_crucible_body),
        Cosmetic("armour_chain", "Death Knight Armour", Slot.ARMOUR, 300, 3, drawableRes = R.drawable.gear_deathknight_body),
        Cosmetic("armour_plate", "Maliketh Armour", Slot.ARMOUR, 700, 8, drawableRes = R.drawable.gear_maliketh_body),

        Cosmetic("weapon_wood", "Crucible Sword", Slot.WEAPON, 80, 1, drawableRes = R.drawable.gear_crucible_sword),
        Cosmetic("weapon_steel", "Death Knight Axe", Slot.WEAPON, 250, 3, drawableRes = R.drawable.gear_deathknight_sword),
        Cosmetic("weapon_flame", "Maliketh Blade", Slot.WEAPON, 650, 8, drawableRes = R.drawable.gear_maliketh_sword),

        Cosmetic("shield_wood", "Crucible Shield", Slot.SHIELD, 80, 1, drawableRes = R.drawable.gear_crucible_shield),
        Cosmetic("shield_iron", "Death Knight Shield", Slot.SHIELD, 250, 3, drawableRes = R.drawable.gear_deathknight_shield),
        Cosmetic("shield_aegis", "Maliketh Shield", Slot.SHIELD, 600, 8, drawableRes = R.drawable.gear_maliketh_shield),

        Cosmetic("colour_crimson", "Crimson", Slot.COLOUR, 150, 1, 0xFFB71C1C.toInt()),
        Cosmetic("colour_azure", "Azure", Slot.COLOUR, 150, 1, 0xFF1565C0.toInt()),
        Cosmetic("colour_emerald", "Emerald", Slot.COLOUR, 150, 1, 0xFF2E7D32.toInt()),
        Cosmetic("colour_purple", "Royal Purple", Slot.COLOUR, 400, 4, 0xFF6A1B9A.toInt()),
        Cosmetic("colour_gold", "Golden", Slot.COLOUR, 800, 10, 0xFFFFC107.toInt()),

        Cosmetic("bg_stone", "Stone Chamber", Slot.BACKGROUND, 100, 1, 0xFF37474F.toInt()),
        Cosmetic("bg_crimson", "Crimson Keep", Slot.BACKGROUND, 250, 3, 0xFF8B1A1A.toInt()),
        Cosmetic("bg_emerald", "Emerald Cavern", Slot.BACKGROUND, 250, 3, 0xFF14602A.toInt()),
        Cosmetic("bg_midnight", "Midnight Vault", Slot.BACKGROUND, 500, 6, 0xFF0D1330.toInt()),

        Cosmetic("title_penny", "Penny Pincher", Slot.TITLE, 100, 1),
        Cosmetic("title_hunter", "Treasure Hunter", Slot.TITLE, 300, 5),
        Cosmetic("title_legend", "Vault Legend", Slot.TITLE, 900, 12),

        Cosmetic("deco_torch", "Wall Torch", Slot.DECORATION, 60, 1, drawableRes = R.drawable.deco_torch),
        Cosmetic("deco_banner", "War Banner", Slot.DECORATION, 150, 2, drawableRes = R.drawable.deco_banner),
        Cosmetic("deco_chest", "Treasure Chest", Slot.DECORATION, 400, 5, drawableRes = R.drawable.deco_chest)
    )

    fun byKey(key: String): Cosmetic? = items.firstOrNull { it.key == key }
}
