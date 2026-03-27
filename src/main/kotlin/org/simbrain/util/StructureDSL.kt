package org.simbrain.util

import org.simbrain.workspace.WorkspacePreferences
import javax.swing.JMenu
import javax.swing.JMenuItem

/**
 * An entry in a [StructureDir]. Holds the display name, the value (either a leaf item or a sub-[StructureDir]),
 * and whether this entry is marked as beta.
 */
data class StructureEntry(val name: String, val value: Any, val beta: Boolean = false)

/**
 * A DSL to support creating a structured list of menus in a Swing application. Each menu item is associated with a
 * task.
 *
 * @param R the return value (if needed) for the task associated with the menu item.
 * @param alphabetical if true, alphabetize items in this directory
 * @param divAtTop if true, `dir`s are display ahead of loose `item`s
 */
class StructureDir<R>(val name: String, private val alphabetical: Boolean, private val divAtTop: Boolean) {

    private val list = ArrayList<StructureEntry>()

    private var sorted = false

    fun item(name: String, beta: Boolean = false, block: () -> R) {
        sorted = false
        list.add(StructureEntry(name, block() as Any, beta))
    }

    fun dir(
        name: String,
        alphabetical: Boolean = false,
        divAtTop: Boolean = true,
        block: StructureDir<R>.() -> Unit
    ): StructureDir<R> {
        sorted = false
        return StructureDir<R>(name, alphabetical, divAtTop).apply(block).also {
            list.add(StructureEntry(name, it as Any))
        }
    }

    fun asMenu(itemAction: (R) -> Unit): JMenu {
        return JMenu(name).also { addToMenu(it, itemAction) }
    }

    fun addToMenu(menu: JMenu, itemAction: (R) -> Unit) {
        val showBeta = WorkspacePreferences.showBetaSimulations
        sortList()
        menu.apply {
            list.forEach { entry ->
                if (entry.value is StructureDir<*>) {
                    @Suppress("UNCHECKED_CAST")
                    val subDir = entry.value as StructureDir<R>
                    if (subDir.hasVisibleItems(showBeta)) {
                        add(subDir.asMenu(itemAction))
                    }
                } else if (!entry.beta || showBeta) {
                    val label = if (entry.beta) "${entry.name} (beta)" else entry.name
                    JMenuItem(label).apply {
                        addActionListener {
                            @Suppress("UNCHECKED_CAST")
                            itemAction(entry.value as R)
                        }
                    }.also { add(it) }
                }
            }
        }
    }

    /**
     * Returns true if this directory has any visible items given the current beta visibility setting.
     */
    fun hasVisibleItems(showBeta: Boolean): Boolean {
        return list.any { entry ->
            when (entry.value) {
                is StructureDir<*> -> entry.value.hasVisibleItems(showBeta)
                else -> !entry.beta || showBeta
            }
        }
    }

    val items: Sequence<Pair<String, R>> get() = sequence {
        sortList()
        list.forEach { entry ->
            if (entry.value is StructureDir<*>) {
                @Suppress("UNCHECKED_CAST")
                yieldAll(entry.value.items as Sequence<Pair<String, R>>)
            } else {
                @Suppress("UNCHECKED_CAST")
                yield(entry.name to entry.value as R)
            }
        }
    }

    private fun sortList() {
        if (!sorted) {
            if (alphabetical) list.sortBy { it.name }
            if (divAtTop) list.sortBy { if (it.value is StructureDir<*>) 0 else 1 }
            sorted = true
        }
    }
}

fun <R> dir(
    name: String,
    alphabetical: Boolean = false,
    divAtTop: Boolean = true,
    block: StructureDir<R>.() -> Unit
): StructureDir<R> {
    return StructureDir<R>(name, alphabetical, divAtTop).apply(block)
}
