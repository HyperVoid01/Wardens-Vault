package com.soo.wardensvault

import android.content.Context

//Net Worth = Total Assets - Total Liabilities
object NetWorthManager {
    const val ASSET = "ASSET"
    const val LIABILITY = "LIABILITY"

    val assetGroups = listOf("Cash", "Savings", "Investments", "Other assets")
    val liabilityGroups = listOf("Loans", "Credit card debt", "Other debts")

    data class Totals(val assets: Double, val liabilities: Double) {
        val netWorth: Double get() = assets - liabilities
    }

    fun totals(items: List<NetWorthItem>): Totals {
        val assets = items.filter { it.kind == ASSET }.sumOf { it.amount }
        val liabilities = items.filter { it.kind == LIABILITY }.sumOf { it.amount }
        return Totals(assets, liabilities)
    }

    //stores today's net worth (one snapshot per day) so changes over time can be shown
    suspend fun recordSnapshot(context: Context, userId: String): Totals {
        val dao = AppDatabase.getDatabase(context).netWorthDao()
        val totals = totals(dao.getItems(userId))
        val today = DateUtils.startOfToday()
        val existing = dao.getSnapshotForDay(userId, today)
        if (existing == null) {
            dao.insertSnapshot(NetWorthSnapshot(userId = userId, date = today, netWorth = totals.netWorth))
        } else {
            dao.updateSnapshot(existing.copy(netWorth = totals.netWorth))
        }
        return totals
    }
}
