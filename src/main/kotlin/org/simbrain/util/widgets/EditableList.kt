package org.simbrain.util.widgets

import org.simbrain.util.LabelledItemPanel
import org.simbrain.util.ResourceManager
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * A list of graphical items (JComponents) that can be added or removed.
 *
 * @author Jeff Yoshimi
 */
open class EditableList(val showAddRemove: Boolean = true) : JPanel() {

    val components: MutableList<JComponent> = arrayListOf()
    val mainPanel = EditableItemPanel(components)

    // Yulin
    init {

        layout = BorderLayout()
        preferredSize = Dimension(400, 300)

        add("Center", JScrollPane(mainPanel))
        add("South", JToolBar().apply {
            if (showAddRemove) {
                add(JButton(ResourceManager.getImageIcon("menu_icons/plus.png")).apply {
                    toolTipText = "Add an item to the list"
                    addActionListener {
                        addElementTask.invoke()
                    }
                })
                add(JButton(ResourceManager.getImageIcon("menu_icons/minus.png")).apply {
                    toolTipText = "Remove an item from the list"
                    addActionListener {
                        removeElement()
                    }
                })
            }
        })
    }

    /**
     * Allows a custom add action to be set.
     */
    var addElementTask: () -> Unit = {}

    fun addElement(c: JComponent) {
        components.add(c)
        mainPanel.addItem(c)
    }

    open fun removeElement() {
        if (components.size > 0) {
            removeLast()
        }
    }

    fun removeLast() {
        components.removeLast()
        mainPanel.removeLastItem()
    }

}

/**
 * A [LabelledItemPanel] backed by a list of JComponents which can be added or removed one at a time.
 * Items must be added using [LabelledItemPanel.addItem] for removal to function properly.
 */
class EditableItemPanel(var displayedItems: List<JComponent> = ArrayList()) : LabelledItemPanel() {

    init {
        displayedItems.forEach { c -> addItem(c) }
    }

    /**
     * Remove an item from the panel.
     */
    fun removeLastItem() {
        if (components.isNotEmpty()) {
            remove(components.size - 1)
        }
        repaint()
    }

}
