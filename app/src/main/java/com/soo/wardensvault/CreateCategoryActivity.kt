// Code Attribution
// Based on: Dialogs
// Author:   Android Developers
// URL:      https://developer.android.com/develop/ui/views/components/dialogs
// Accessed: 27 October 2026
// Used for: AlertDialog builder

package com.soo.wardensvault

import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/*
Category management: add, rename, delete and view the expenses of a category.
A category that is still used by expenses or bills can't just be deleted - the user must
move those records to another category first.
 */
class CreateCategoryActivity : ScreenActivity() {

    override suspend fun render() {
        addTitle("Categories")
        addButton("+ Add Category") { showNameForm(null) }

        val categories = db.categoryDao().getCategoriesForUser(userId)
        if (categories.isEmpty()) {
            addText("No categories yet. Add one to start logging expenses.", Color.WHITE)
            return
        }
        for (category in categories) {
            val used = db.expenseDao().countForCategory(userId, category.id)
            val card = addCard()
            card.addLine(category.name, Color.WHITE, 18f, true)
            card.addLine("$used expense(s) - tap for options", UiKit.MUTED, 13f)
            card.setOnClickListener {
                showOptions(category.name, listOf("View expenses", "Rename", "Delete")) { which ->
                    when (which) {
                        0 -> showExpenses(category)
                        1 -> showNameForm(category)
                        else -> deleteCategory(category)
                    }
                }
            }
        }
    }

    private fun showNameForm(existing: Category?) {
        val fields = listOf(FormField("name", "Category name", FormField.Kind.TEXT, existing?.name ?: ""))
        showForm(if (existing == null) "New category" else "Rename category", fields) { v ->
            val name = v["name"].orEmpty()
            if (name.isEmpty()) return@showForm "Enter a category name"
            val duplicate = db.categoryDao().getCategoriesForUser(userId).any {
                it.name.equals(name, ignoreCase = true) && it.id != existing?.id
            }
            if (duplicate) return@showForm "A category with that name already exists"
            if (existing == null) {
                db.categoryDao().insertCategory(Category(userId = userId, name = name))
            } else {
                db.categoryDao().updateCategory(existing.copy(name = name))
            }
            null
        }
    }

    private fun showExpenses(category: Category) {
        launchSafe {
            val all = db.expenseDao().getExpensesWithCategoryForPeriod(userId, 0L, Long.MAX_VALUE)
                .filter { it.categoryName == category.name }
            val lines = if (all.isEmpty()) {
                listOf("No expenses in this category yet.")
            } else {
                all.take(100).map { "${dateFormat.format(it.date)}  ${money(it.amount)}  ${it.description}" }
            }
            AlertDialog.Builder(this@CreateCategoryActivity)
                .setTitle("${category.name} (${all.size})")
                .setItems(lines.toTypedArray(), null)
                .setPositiveButton("Close", null)
                .show()
        }
    }

    private fun deleteCategory(category: Category) {
        launchSafe {
            val expenses = db.expenseDao().countForCategory(userId, category.id)
            val bills = db.billDao().countForCategory(userId, category.id)
            if (expenses + bills == 0) {
                confirm("Delete category", "Delete \"${category.name}\"?") {
                    launchSafe {
                        db.categoryDao().deleteCategory(category)
                        refresh()
                    }
                }
                return@launchSafe
            }

            val others = db.categoryDao().getCategoriesForUser(userId).filter { it.id != category.id }
            if (others.isEmpty()) {
                toast("\"${category.name}\" is in use. Create another category first so its records can be moved.")
                return@launchSafe
            }
            val fields = listOf(
                FormField(
                    "target",
                    "$expenses expense(s) and $bills bill(s) use \"${category.name}\". Move them to:",
                    FormField.Kind.CHOICE, others[0].name, others.map { it.name }
                )
            )
            showForm("Reassign and delete", fields, "Move & Delete") { v ->
                val target = others.firstOrNull { it.name == v["target"] } ?: return@showForm "Pick a category"
                db.expenseDao().reassignCategory(userId, category.id, target.id)
                db.billDao().reassignCategory(userId, category.id, target.id)
                db.categoryDao().deleteCategory(category)
                null
            }
        }
    }
}
