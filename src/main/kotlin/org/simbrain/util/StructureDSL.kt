package org.simbrain.util

import javax.swing.JMenu
import javax.swing.JMenuItem

class StructureDir<R>(val name: String, private val alphabetical: Boolean, private val divAtTop: Boolean) {

    private val list = ArrayList<Pair<String, Any>>()

    private var sorted = false

    fun item(name: String, block: () -> R) {
        sorted = false
        block().also { list.add(Pair(name, it as Any)) }
    }

    fun dir(
        name: String,
        alphabetical: Boolean = true,
        divAtTop: Boolean = true,
        block: StructureDir<R>.() -> Unit
    ): StructureDir<R> {
        sorted = false
        return StructureDir<R>(name, alphabetical, divAtTop).apply(block).also { list.add(Pair(name, it as Any)) }
    }

    fun asMenu(itemAction: (R) -> Unit): JMenu {
        return JMenu(name).also { addToMenu(it, itemAction) }
    }

    fun addToMenu(menu: JMenu, itemAction: (R) -> Unit) {
        sortList()
        menu.apply {
            list.forEach { (name, item) ->
                if (item is StructureDir<*>) {
                    @Suppress("UNCHECKED_CAST")
                    add((item as StructureDir<R>).asMenu(itemAction))
                } else {
                    JMenuItem(name).apply {
                        addActionListener {
                            @Suppress("UNCHECKED_CAST")
                            itemAction(item as R)
                        }
                    }.also { add(it) }
                }
            }
        }
    }

    val items: Sequence<Pair<String, R>> get() = sequence {
        sortList()
        list.forEach { (name, item) ->
            if (item  is StructureDir<*>) {
                @Suppress("UNCHECKED_CAST")
                yieldAll(item.items as Sequence<Pair<String, R>>)
            } else {
                @Suppress("UNCHECKED_CAST")
                yield(name to item as R)
            }
        }
    }

    private fun sortList() {
        if (!sorted) {
            if (alphabetical) list.sortBy { (name, _) -> name }
            if (divAtTop) list.sortBy { (_, item) -> if (item is StructureDir<*>) 0 else 1 }
            sorted = true
        }
    }
}

fun <R> dir(
    name: String,
    alphabetical: Boolean = true,
    divAtTop: Boolean = true,
    block: StructureDir<R>.() -> Unit
): StructureDir<R> {
    return StructureDir<R>(name, alphabetical, divAtTop).apply(block)
}