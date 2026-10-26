package com.soo.wardensvault

import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

//Goal-based budgeting: create goals, add savings, track progress, earn a reward on completion
class GoalsActivity : ScreenActivity() {

    override suspend fun render() {
        addTitle("Savings Goals")
        addText("Save towards something that matters. Completing a goal earns XP and Gold!", UiKit.MUTED, 14f)
        addButton("+ New Goal") { showGoalForm(null) }

        val goals = db.savingsGoalDao().getAll(userId)
        if (goals.isEmpty()) {
            addText("No goals yet. Try a car, a holiday or an emergency fund.", Color.WHITE)
            return
        }
        val today = DateUtils.startOfToday()
        for (goal in goals) {
            val percent = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount * 100).toInt() else 0
            val card = addCard()
            card.addLine(goal.name, Color.WHITE, 18f, true)
            card.addLine("${money(goal.currentAmount)} / ${money(goal.targetAmount)}", Color.WHITE, 15f)
            card.addBar(percent, if (goal.completed) UiKit.GREEN else UiKit.GOLD)
            card.addLine(
                if (goal.completed) "COMPLETED" else "$percent%",
                if (goal.completed) UiKit.GREEN else UiKit.GOLD, 14f, true
            )
            goal.deadline?.let { deadline ->
                val days = DateUtils.daysBetween(today, deadline)
                val text = when {
                    goal.completed -> "Deadline was ${dateFormat.format(deadline)}"
                    days < 0 -> "Deadline passed ${-days} day(s) ago (${dateFormat.format(deadline)})"
                    else -> "$days day(s) left (${dateFormat.format(deadline)})"
                }
                card.addLine(text, UiKit.MUTED, 12f)
            }
            card.setOnClickListener {
                val labels = if (goal.completed) listOf("Edit", "Delete") else listOf("Add savings", "Edit", "Delete")
                showOptions(goal.name, labels) { which ->
                    when (labels[which]) {
                        "Add savings" -> showAddSavingsForm(goal)
                        "Edit" -> showGoalForm(goal)
                        else -> confirm("Delete goal", "Delete \"${goal.name}\"?") {
                            launchSafe {
                                db.savingsGoalDao().delete(goal)
                                refresh()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showGoalForm(existing: SavingsGoal?) {
        val fields = listOf(
            FormField("name", "Goal name (e.g. New Car)", FormField.Kind.TEXT, existing?.name ?: ""),
            FormField("target", "Target amount", FormField.Kind.NUMBER, existing?.targetAmount?.toString() ?: ""),
            FormField("current", "Amount saved so far", FormField.Kind.NUMBER, existing?.currentAmount?.toString() ?: "0"),
            FormField("deadline", "Deadline (optional)", FormField.Kind.DATE, existing?.deadline?.toString() ?: "")
        )
        showForm(if (existing == null) "New goal" else "Edit goal", fields) { v ->
            val name = v["name"].orEmpty()
            val target = parseAmount(v["target"])
            val current = parseAmount(v["current"])
            if (name.isEmpty()) return@showForm "Enter a goal name"
            if (target == null || target <= 0.0) return@showForm "Enter a target amount greater than 0"
            if (current == null) return@showForm "Enter a valid amount saved (not negative)"
            val deadline = v["deadline"]?.toLongOrNull()

            if (existing == null) {
                val done = current >= target
                db.savingsGoalDao().insert(
                    SavingsGoal(
                        userId = userId, name = name, targetAmount = target, currentAmount = current,
                        deadline = deadline, completed = done
                    )
                )
                if (done) rewardCompletion()
            } else {
                saveProgress(existing.copy(name = name, targetAmount = target, currentAmount = current, deadline = deadline))
            }
            null
        }
    }

    private fun showAddSavingsForm(goal: SavingsGoal) {
        val fields = listOf(FormField("amount", "Amount to add", FormField.Kind.NUMBER))
        showForm("Add savings to ${goal.name}", fields, "Add") { v ->
            val amount = parseAmount(v["amount"])
            if (amount == null || amount <= 0.0) return@showForm "Enter an amount greater than 0"
            saveProgress(goal.copy(currentAmount = goal.currentAmount + amount))
            null
        }
    }

    //saves the goal; the first time it reaches its target the reward is given
    private suspend fun saveProgress(goal: SavingsGoal) {
        val wasCompleted = goal.completed
        val done = goal.currentAmount >= goal.targetAmount
        db.savingsGoalDao().update(goal.copy(completed = done))
        if (done && !wasCompleted) rewardCompletion()
    }

    private suspend fun rewardCompletion() {
        val award = gamification.award(userId, GamificationManager.GOAL_XP, GamificationManager.GOAL_GOLD)
        NotificationHelper.notify(
            this, AppSettings.KEY_GOALS, 7000, "Savings goal completed!",
            "You earned ${award.xpGained} XP and ${award.goldGained} Gold."
        )
        toast("Goal completed! +${award.xpGained} XP +${award.goldGained} Gold")
        announce(AchievementManager.checkAll(this, userId))
    }
}
