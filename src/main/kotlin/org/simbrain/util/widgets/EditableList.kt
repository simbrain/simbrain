package org.simbrain.util.widgets

import org.simbrain.util.LabelledItemPanel
import org.simbrain.util.ResourceManager
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * A list of graphical items (JComponents) that can be added or removed.
 *
 * @param showAddRemove if true, show the +/- signs. In that case [newElementTask] must be defined for the + button.
 *
 * @author Jeff Yoshimi
 */
open class EditableList<O: EditableObject>(private val showAddRemove: Boolean = true) : JPanel() {


    val components: MutableList<AnnotatedPropertyEditor<O>> = arrayListOf()
    val mainPanel = EditableItemPanel(components)

    /**
     * This is the block that gets executed when the plus button is pressed.
     */
    var newElementTask: () -> Unit = {}

    init {

        layout = BorderLayout()
        preferredSize = Dimension(400, 450)

        add("Center", JScrollPane(mainPanel))
        add("South", JToolBar().apply {
            if (showAddRemove) {
                add(JButton(ResourceManager.getImageIcon("menu_icons/plus.png")).apply {
                    toolTipText = "Add an item to the list"
                    addActionListener {
                        newElementTask.invoke()
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

    fun addElement(c: AnnotatedPropertyEditor<O>) {
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
