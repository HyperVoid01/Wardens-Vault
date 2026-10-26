package com.soo.wardensvault

import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

//Net worth = total assets - total liabilities, with a history of how it changed
class NetWorthActivity : ScreenActivity() {

    //"Asset - Cash", "Liability - Loans" ...
    private val typeOptions: List<String> =
        NetWorthManager.assetGroups.map { "Asset - $it" } +
            NetWorthManager.liabilityGroups.map { "Liability - $it" }

    override suspend fun render() {
        addTitle("Net Worth")
        val totals = NetWorthManager.recordSnapshot(this, userId)
        val color = if (totals.netWorth >= 0) UiKit.GREEN else UiKit.RED
        addText("Net worth: ${money(totals.netWorth)}", color, 24f, true)
        addText("Assets ${money(totals.assets)}  -  Liabilities ${money(totals.liabilities)}", UiKit.MUTED, 14f)
        addButton("+ Add Asset or Liability") { showItemForm(null) }

        val items = db.netWorthDao().getItems(userId)
        addItems("Assets", items.filter { it.kind == NetWorthManager.ASSET })
        addItems("Liabilities", items.filter { it.kind == NetWorthManager.LIABILITY })

        val snapshots = db.netWorthDao().getSnapshots(userId)
        addHeader("Net worth over time")
        if (snapshots.size < 2) {
            addText("History builds up as you open this screen on different days.", UiKit.MUTED, 14f)
        }
        val card = addCard()
        var previous: Double? = null
        for (snapshot in snapshots.takeLast(10)) {
            val change = previous?.let { snapshot.netWorth - it }
            val changeText = when {
                change == null -> ""
                change >= 0 -> "  (+${money(change)})"
                else -> "  (-${money(-change)})"
            }
            card.addLine("${dateFormat.format(snapshot.date)}: ${money(snapshot.netWorth)}$changeText", Color.WHITE, 14f)
            previous = snapshot.netWorth
        }
    }

    private fun addItems(header: String, list: List<NetWorthItem>) {
        addHeader(header)
        if (list.isEmpty()) {
            addText("None added.", UiKit.MUTED, 14f)
            return
        }
        for (item in list) {
            val card = addCard()
            card.addLine("${item.name}  -  ${money(item.amount)}", Color.WHITE, 16f, true)
            card.addLine(item.groupName, UiKit.MUTED, 13f)
            card.setOnClickListener {
                showOptions(item.name, listOf("Edit", "Delete")) { which ->
                    if (which == 0) {
                        showItemForm(item)
                    } else {
                        confirm("Delete", "Delete ${item.name}?") {
                            launchSafe {
                                db.netWorthDao().deleteItem(item)
                                refresh()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showItemForm(existing: NetWorthItem?) {
        val currentType = existing?.let {
            (if (it.kind == NetWorthManager.ASSET) "Asset - " else "Liability - ") + it.groupName
        } ?: typeOptions[0]
        val fields = listOf(
            FormField("name", "Name (e.g. Bank account)", FormField.Kind.TEXT, existing?.name ?: ""),
            FormField("type", "Type", FormField.Kind.CHOICE, currentType, typeOptions),
            FormField("amount", "Amount", FormField.Kind.NUMBER, existing?.amount?.toString() ?: "")
        )
        showForm(if (existing == null) "Add to net worth" else "Edit", fields) { v ->
            val name = v["name"].orEmpty()
            val amount = parseAmount(v["amount"])
            val type = v["type"].orEmpty()
            if (name.isEmpty()) return@showForm "Enter a name"
            if (amount == null) return@showForm "Enter a valid amount (not negative)"
            val isAsset = type.startsWith("Asset - ")
            val kind = if (isAsset) NetWorthManager.ASSET else NetWorthManager.LIABILITY
            val group = type.substringAfter(" - ")
            if (existing == null) {
                db.netWorthDao().insertItem(
                    NetWorthItem(userId = userId, name = name, kind = kind, groupName = group, amount = amount)
                )
            } else {
                db.netWorthDao().updateItem(
                    existing.copy(name = name, kind = kind, groupName = group, amount = amount)
                )
            }
            null
        }
    }
}
