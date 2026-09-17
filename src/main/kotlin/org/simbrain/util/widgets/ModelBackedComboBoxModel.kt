/**
 * A combo box model whose items and selection are owned by application state rather than by the widget. Programmatic
 * updates go through [ModelBackedComboBoxModel.sync] and never reach the user-selection callback, while picks made in the
 * widget arrive through [ModelBackedComboBoxModel.setSelectedItem]. This separates the two paths structurally, so a
 * view that mirrors a model does not need a guard flag, and populating an empty combo box never selects anything.
 */
package org.simbrain.util.widgets

import javax.swing.AbstractListModel
import javax.swing.ComboBoxModel

class ModelBackedComboBoxModel<T : Any>(
    private val onUserSelect: (T) -> Unit
) : AbstractListModel<T>(), ComboBoxModel<T> {

    private var items: List<T> = emptyList()

    private var selected: T? = null

    override fun getSize(): Int = items.size

    override fun getElementAt(index: Int): T = items[index]

    override fun getSelectedItem(): Any? = selected

    /**
     * The user path. Swing calls this when the person picks an item in the popup or with the keyboard. The widget
     * shows the new choice immediately and the choice is handed to the callback; a later [sync] from the model
     * confirms or corrects it.
     */
    override fun setSelectedItem(anItem: Any?) {
        @Suppress("UNCHECKED_CAST")
        val item = anItem as? T ?: return
        if (item == selected) return
        selected = item
        fireContentsChanged(this, -1, -1)
        onUserSelect(item)
    }

    /**
     * The programmatic path. Replace the items and the selection to mirror the model. Never invokes the callback.
     * A [selected] value that is not among [items] leaves nothing selected.
     */
    fun sync(items: List<T>, selected: T?) {
        this.items = items.toList()
        this.selected = selected?.takeIf { it in this.items }
        fireContentsChanged(this, 0, (this.items.size - 1).coerceAtLeast(0))
    }
}
