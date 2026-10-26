package com.soo.wardensvault

//Turns a bill into its fantasy enemy: Rent -> Rent Ogre, Electricity -> Electricity Beast ...
data class Enemy(val displayName: String, val drawableRes: Int)

object EnemyCatalog {
    fun forBill(billName: String): Enemy {
        val n = billName.lowercase()
        return when {
            n.contains("rent") || n.contains("bond") || n.contains("mortgage") ->
                Enemy("$billName Ogre", R.drawable.ic_ogre)
            n.contains("electric") || n.contains("power") || n.contains("eskom") ->
                Enemy("$billName Beast", R.drawable.ic_minator)
            n.contains("water") ->
                Enemy("$billName Monster", R.drawable.ic_mimic)
            n.contains("internet") || n.contains("wifi") || n.contains("fibre") || n.contains("phone") ->
                Enemy("$billName Wraith", R.drawable.ic_ghost)
            n.contains("insurance") || n.contains("medical") ->
                Enemy("$billName Skeleton", R.drawable.ic_skel)
            else -> {
                val options = listOf(
                    Enemy("$billName Ogre", R.drawable.ic_ogre),
                    Enemy("$billName Beast", R.drawable.ic_minator),
                    Enemy("$billName Monster", R.drawable.ic_mimic),
                    Enemy("$billName Wraith", R.drawable.ic_ghost),
                    Enemy("$billName Skeleton", R.drawable.ic_skel)
                )
                options[Math.abs(n.hashCode() % options.size)]
            }
        }
    }
}
