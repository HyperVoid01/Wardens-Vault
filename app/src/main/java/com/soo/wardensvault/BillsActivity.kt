package com.soo.wardensvault

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

//Dungeon: recurring bills shown as enemies. Paying a bill defeats its enemy.
class BillsActivity : ScreenActivity() {

    override suspend fun render() {
        BillManager.rollover(this, userId)

        addTitle("Dungeon")
        addText("Every recurring bill is an enemy. Pay it to defeat it and earn XP and Gold!", UiKit.MUTED, 14f)
        addButton("+ Summon New Bill Enemy") { showBillForm(null) }

        val categories = db.categoryDao().getCategoriesForUser(userId)
        val categoryNames = categories.associate { it.id to it.name }
        val bills = db.billDao().getAll(userId)
        if (bills.isEmpty()) {
            addText("No enemies yet. Add a bill such as Rent or Electricity.", Color.WHITE)
            return
        }

        val today = DateUtils.startOfToday()
        for (bill in bills) {
            val enemy = EnemyCatalog.forBill(bill.name)
            val card = addCard()

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val image = ImageView(this).apply {
                setImageDrawable(UiKit.crisp(this@BillsActivity, enemy.drawableRes))
                layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@BillsActivity, 72), UiKit.dp(this@BillsActivity, 72))
                if (bill.isPaid) alpha = 0.35f
            }
            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = UiKit.dp(this@BillsActivity, 12)
                }
            }
            info.addView(UiKit.text(this, enemy.displayName, Color.WHITE, 18f, true))
            //enemy health = how much of the bill is still owed (partial payments wound it)
            info.addView(healthBar(healthLevel(BillManager.health(bill))))
            info.addView(
                UiKit.text(
                    this,
                    "${money(bill.amount)} - ${bill.frequency} - due ${dateFormat.format(bill.dueDate)}",
                    UiKit.MUTED, 13f
                )
            )
            info.addView(UiKit.text(this, "Category: ${categoryNames[bill.categoryId] ?: "-"}", UiKit.MUTED, 13f))

            val days = DateUtils.daysBetween(today, bill.dueDate)
            val (statusText, statusColor) = when {
                bill.isPaid -> Pair("DEFEATED - returns next cycle", UiKit.GREEN)
                bill.paidThisCycle > 0.0 && days >= 0 ->
                    Pair("WOUNDED - ${money(BillManager.remaining(bill))} left, due in $days day(s)", UiKit.ORANGE)
                days < 0 -> Pair("OVERDUE by ${-days} day(s)", UiKit.RED)
                days == 0 -> Pair("DUE TODAY", UiKit.ORANGE)
                else -> Pair("Alive - due in $days day(s)", UiKit.ORANGE)
            }
            info.addView(UiKit.text(this, statusText, statusColor, 14f, true))

            row.addView(image)
            row.addView(info)
            card.addView(row)

            val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            if (!bill.isPaid) {
                buttons.addView(smallButton("Attack (Pay)") { showPayForm(bill, enemy) })
            }
            buttons.addView(smallButton("Edit") { showBillForm(bill) })
            buttons.addView(smallButton("Delete") {
                confirm("Delete bill", "Delete ${enemy.displayName}? Past payments stay in your expense history.") {
                    launchSafe {
                        db.billDao().delete(bill)
                        refresh()
                    }
                }
            })
            card.addView(buttons)
        }
    }

    /*
    Health bar from HealthBar.png: a grey empty bar with the red bar drawn on top,
    cut to [level] (0 = empty, FULL_HEALTH = full) with a ClipDrawable.
     */
    private fun healthBar(level: Int, heightDp: Int = 18): FrameLayout {
        val frame = FrameLayout(this).apply {
            background = UiKit.crisp(this@BillsActivity, R.drawable.hp_bar_empty)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@BillsActivity, heightDp)
            ).apply { topMargin = UiKit.dp(this@BillsActivity, 6) }
        }
        val fill = ImageView(this).apply {
            val clip = ClipDrawable(
                UiKit.crisp(this@BillsActivity, R.drawable.hp_bar_full), Gravity.START, ClipDrawable.HORIZONTAL
            )
            clip.level = level
            setImageDrawable(clip)
            scaleType = ImageView.ScaleType.FIT_XY
        }
        frame.addView(fill, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        frame.tag = fill   //lets the battle animation find the fill
        return frame
    }

    //0..1 health -> ClipDrawable level
    private fun healthLevel(health: Double): Int = (health * FULL_HEALTH).toInt().coerceIn(0, FULL_HEALTH)

    private fun smallButton(label: String, onClick: () -> Unit): Button {
        return UiKit.button(this, label, onClick).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    private fun showBillForm(existing: Bill?) {
        launchSafe {
            val categories = db.categoryDao().getCategoriesForUser(userId)
            if (categories.isEmpty()) {
                toast("Add a category first (Market > Categories)")
                return@launchSafe
            }
            val names = categories.map { it.name }
            val currentCategory = categories.firstOrNull { it.id == existing?.categoryId }?.name ?: names[0]
            val fields = listOf(
                FormField("name", "Bill name (e.g. Rent)", FormField.Kind.TEXT, existing?.name ?: ""),
                FormField("amount", "Amount (R)", FormField.Kind.NUMBER, existing?.amount?.toString() ?: ""),
                FormField("due", "Next due date", FormField.Kind.DATE, existing?.dueDate?.toString() ?: ""),
                FormField("frequency", "Recurring frequency", FormField.Kind.CHOICE,
                    existing?.frequency ?: "Monthly", listOf("Weekly", "Monthly", "Yearly")),
                FormField("category", "Category", FormField.Kind.CHOICE, currentCategory, names)
            )
            showForm(if (existing == null) "New bill enemy" else "Edit bill", fields) { v ->
                val name = v["name"].orEmpty()
                val amount = parseAmount(v["amount"])
                val due = v["due"]?.toLongOrNull()
                val category = categories.firstOrNull { it.name == v["category"] }
                when {
                    name.isEmpty() -> return@showForm "Enter a bill name"
                    amount == null || amount <= 0.0 -> return@showForm "Enter a valid amount greater than 0"
                    due == null -> return@showForm "Pick a due date"
                    category == null -> return@showForm "Pick a category"
                }
                if (existing == null) {
                    db.billDao().insert(
                        Bill(
                            userId = userId, name = name, amount = amount, dueDate = due,
                            frequency = v["frequency"] ?: "Monthly", categoryId = category.id, isPaid = false
                        )
                    )
                } else {
                    db.billDao().update(
                        existing.copy(
                            name = name, amount = amount, dueDate = due,
                            frequency = v["frequency"] ?: "Monthly", categoryId = category.id,
                            //a new due date starts a fresh cycle
                            isPaid = if (due == existing.dueDate) existing.isPaid else false,
                            paidThisCycle = if (due == existing.dueDate) existing.paidThisCycle else 0.0
                        )
                    )
                }
                null
            }
        }
    }

    private fun showPayForm(bill: Bill, enemy: Enemy) {
        val owed = BillManager.remaining(bill)
        val fields = listOf(
            FormField(
                "amount", "Amount paid (R) - ${money(owed)} defeats it",
                FormField.Kind.NUMBER, String.format(java.util.Locale.US, "%.2f", owed)
            )
        )
        showForm("Attack ${enemy.displayName}", fields, "Pay & Attack") { v ->
            val amount = parseAmount(v["amount"])
            if (amount == null || amount <= 0.0) return@showForm "Enter a valid amount greater than 0"
            val result = BillManager.payBill(this@BillsActivity, userId, bill, amount)
            val achievements = AchievementManager.checkAll(this@BillsActivity, userId)
            AlertEngine.notifyNew(this@BillsActivity, userId)
            showDefeatDialog(enemy, result, achievements)
            null
        }
    }

    /*
    Battle: the health bar drops by the amount paid while the enemy shakes.
    Paid in full -> it spins and fades away and the rewards are shown.
    Paid less    -> it survives, wounded, and the dialog says how much is left.
     */
    private fun showDefeatDialog(enemy: Enemy, result: BillManager.PayResult, achievements: List<AchievementDef>) {
        val image = ImageView(this).apply {
            setImageDrawable(UiKit.crisp(this@BillsActivity, enemy.drawableRes))
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@BillsActivity, 160), UiKit.dp(this@BillsActivity, 160))
        }
        val message = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 16f
            text = "Attacking ${enemy.displayName}..."
        }
        val hp = healthBar(healthLevel(result.healthBefore), 22)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val pad = UiKit.dp(this@BillsActivity, 20)
            setPadding(pad, pad, pad, pad)
            addView(image)
            addView(hp)
            addView(message)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Battle!")
            .setView(box)
            .setPositiveButton(if (result.defeated) "Victory!" else "Continue", null)
            .create()
        dialog.show()

        val award = result.award
        val sb = StringBuilder()
        if (result.defeated && award != null) {
            sb.append("${enemy.displayName} defeated!\n+${award.xpGained} XP  +${award.goldGained} Gold")
            if (result.early) sb.append("\n(early payment bonus included)")
            if (award.leveledUp) sb.append("\nLEVEL UP! You are now level ${award.newLevel}")
        } else {
            sb.append("A solid hit, but ${enemy.displayName} is still standing!\n")
            sb.append("Pay the remaining ${money(result.stillOwed)} to defeat it.")
        }
        for (a in achievements) sb.append("\nAchievement: ${a.name}")

        //the enemy's health drains while it shakes
        val clip = (hp.tag as ImageView).drawable
        ValueAnimator.ofInt(healthLevel(result.healthBefore), healthLevel(result.healthAfter)).apply {
            duration = 900
            addUpdateListener { clip.level = it.animatedValue as Int }
            start()
        }

        val shake = ObjectAnimator.ofFloat(image, "translationX", 0f, 30f, -30f, 30f, -30f, 0f)
        shake.duration = 600
        shake.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (result.defeated) {
                    image.animate().alpha(0f).scaleX(0.2f).scaleY(0.2f).rotation(180f).setDuration(700)
                        .withEndAction { message.text = sb.toString() }
                        .start()
                } else {
                    message.text = sb.toString()   //the enemy survives
                }
            }
        })
        shake.start()
    }

    companion object {
        private const val FULL_HEALTH = 10000   //ClipDrawable's full level
    }
}
