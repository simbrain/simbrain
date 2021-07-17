package org.simbrain.util.widgets

import org.simbrain.network.kotlindl.TFDenseLayer
import org.simbrain.network.kotlindl.TFFlattenLayer
import org.simbrain.util.LabelledItemPanel
import org.simbrain.util.ResourceManager
import org.simbrain.util.StandardDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.ObjectTypeEditor
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * A list of items that can be edited with an [AnnotatedPropertyEditor].
 *
 * @author Jeff Yoshimi
 */
class EditableList(val objects: ArrayList<JComponent>): JPanel() {

    val mainPanel = EditableItemPanel(objects)

    init {
        layout = BorderLayout()

        preferredSize = Dimension(400,300)

        add("Center", JScrollPane(mainPanel))
        add("South", JToolBar().apply {
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
        })
    }

    /**
     * Allows a custom add action to be set.
     */
    var addElementTask: () -> Unit = {}

    fun addElement(c: JComponent) {
        objects.add(c)
        mainPanel.addItem(c)
    }

    fun removeElement() {
        if(objects.size > 0) {
            objects.removeLast()
            mainPanel.removeLastItem()
        }
    }

}

/**
 * A [LabelledItemPanel] backed by a list of JComponents which can be added or removed one at a time.
 * Items must be added using [LabelledItemPanel.addItem] for removal to function properly.
 */
class EditableItemPanel(var displayedItems: List<JComponent> = ArrayList()) : LabelledItemPanel() {

    init {
        displayedItems.forEach{c -> addItem(c)}
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

/**
 * Test main. Introduces broader Simbrain dependencies but these are easily removed if desired.
 */
fun main() {

    fun getEditor(obj : CopyableObject):  JPanel {
        return ObjectTypeEditor.createEditor(listOf(obj), "getTypes", "Layer",
            false)
    }

    val objs = arrayListOf<JComponent>(getEditor(TFDenseLayer()), getEditor(TFFlattenLayer()))
    StandardDialog().apply {
        val list = EditableList(objs).apply {
            addElementTask = {
                addElement(getEditor(TFFlattenLayer()))
            }
        }
        contentPane = list
        pack()
        setLocationRelativeTo(null)
        isVisible = true
        addClosingTask {
            println("Closing..")
            list.objects.forEach { p -> println((p as ObjectTypeEditor).value) }
        }
    }

}