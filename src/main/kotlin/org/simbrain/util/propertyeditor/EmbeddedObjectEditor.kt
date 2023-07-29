/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.util.propertyeditor

import org.simbrain.util.widgets.DropDownTriangle
import java.awt.Dimension
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*

class EmbeddedObjectEditor(
    private val objectList: List<CopyableObject>, val label: String, val showDetails: Boolean,
    private var parent: Window? = null
) : JPanel() {

    /**
     * Editor panel for the set of objects (null panel if they are inconsistent).
     */
    private var editorPanel: AnnotatedPropertyEditor? = null

    /**
     * For showing/hiding the property editor.
     */
    private var detailTriangle: DropDownTriangle? = null

    /**
     * A task that is run after the object type is changed using the combo box.
     */
    private var objectChangedTask = Runnable {}

    /**
     * Container for editor panel used to clear the panel on updates.
     */
    private var editorPanelContainer: JPanel? = null

    /**
     * The object used to set the type of all edited objects, when their type
     * is changed.  This is why edited objects must have a copy function.
     */
    var prototypeObject: CopyableObject? = null
        private set

    /**
     * Prototype mode is set to true as soon  as the combo box is changed. In
     * this mode when committing changes the prototype object is used (via
     * [CopyableObject.copy]) to set the types of all objects in the
     * edited list. If the combo box is not changed, the prototype object is not
     * used. The types of the objects all stay the same and only their
     * parameters are changed.
     */
    var isPrototypeMode = false
        private set

    val consistent = objectList.map { it::class }.toSet().size == 1

    /**
     * Create the editor from a set of objects and a type map. Currently just used by the test method.
     *
     * @param objectList the list of objects to edit
     * @param typeMap    the mapping from strings to types
     * @param label      label around border
     * @param showDetails whether the detail triangle should be down when open
     * @param parent     the parent window
     */
    init {
        check(objectList.isNotEmpty()) { "Can't edit empty list of objects" }
        editorPanel = if (!consistent) {
            AnnotatedPropertyEditor(emptyList())
        } else {
            AnnotatedPropertyEditor(objectList)
        }
        layoutPanel(showDetails)
    }

    /**
     * Initialize the panel.
     */
    private fun layoutPanel(showDetails: Boolean) {

        // General layout setup
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        val padding = BorderFactory.createEmptyBorder(5, 5, 5, 5)

        // Top Panel contains the combo box and detail triangle
        val topPanel = JPanel()
        topPanel.layout = BoxLayout(topPanel, BoxLayout.X_AXIS)
        val tb = BorderFactory.createTitledBorder(label)
        border = tb
        topPanel.alignmentX = CENTER_ALIGNMENT
        topPanel.border = padding
        this.add(Box.createRigidArea(Dimension(0, 5)))
        this.add(topPanel)

        // Set up detail triangle
        detailTriangle =
            DropDownTriangle(DropDownTriangle.UpDirection.LEFT, showDetails, "Settings", "Settings", parent)
        detailTriangle!!.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(arg0: MouseEvent) {
                syncPanelToTriangle()
            }
        })
        topPanel.add(Box.createHorizontalStrut(30))
        topPanel.add(Box.createHorizontalGlue())
        topPanel.add(detailTriangle)

        // Container for editor panel, so that it can easily be refreshed
        editorPanelContainer = JPanel()
        this.add(editorPanelContainer)
        editorPanel!!.alignmentX = CENTER_ALIGNMENT
        editorPanel!!.border = padding
        editorPanelContainer!!.add(editorPanel)
        editorPanel!!.isVisible = detailTriangle!!.isDown
        updateEditorPanel()
    }

    /**
     * Fill field values for [AnnotatedPropertyEditor] representing
     * objects being edited.
     */
    fun fillFieldValues() {
        editorPanel!!.fillFieldValues(objectList)
    }

    /**
     * Set a callback that is called only after when the combo box is changed.
     */
    fun setObjectChangedTask(callback: Runnable) {
        objectChangedTask = callback
    }

    /**
     * Update the graphical state of the editor panel.  Called when
     * changing the combo box.
     */
    private fun updateEditorPanel() {

        // This occurs when the object type is inconsistent, i
        // i.e.  "..." in the combo box
        if (!consistent) {
            return
        }

        // In the case where the object has no properties at all to edit, and
        // the type editor is just being used to set a type, just remove
        // everything from the panel, and hide the detail triangle.
        if (editorPanel!!.widgets.size == 0) {
            isVisible = false
        } else {
            isVisible = true
            detailTriangle!!.isVisible = true
        }
    }

    /**
     * If detail triangle is down, show the panel; if not hide the panel.
     */
    private fun syncPanelToTriangle() {
        editorPanel!!.isVisible = detailTriangle!!.isDown
        repaint()
        if (parent == null) {
            parent = SwingUtilities.getWindowAncestor(this@EmbeddedObjectEditor)
        }
        if (parent != null) {
            parent!!.pack()
        }
    }

    val value: Any?
        /**
         * The current value of this widget. It should be the object that can be
         * copied, or null.
         *
         * @return the object to be copied when done, or null.
         */
        get() = if (!consistent) {
            null
        } else {
            if (isPrototypeMode) {
                prototypeObject
            } else {
                objectList[0]
            }
        }

    fun refreshPrototype(newPrototype: CopyableObject) {
        isPrototypeMode = true
        prototypeObject = newPrototype
        editorPanel = AnnotatedPropertyEditor(prototypeObject!!)
        editorPanelContainer!!.removeAll()
        editorPanelContainer!!.add(editorPanel)
        updateEditorPanel()
        syncPanelToTriangle()
        objectChangedTask.run()

        // TODO: Remove or at least simplify
        // Maybe move to some utility class.  Like Simbrain.pack().
        if (parent == null) {
            parent = SwingUtilities.getWindowAncestor(this)
        }
        if (parent != null) {
            parent!!.pack()
        }
    }

    /**
     * Commit any changes made.
     */
    fun commitChanges() {
        if (!consistent) {
            return
        }
        if (isPrototypeMode) {
            // Sync prototype object to editor panel
            editorPanel!!.commitChanges(mutableListOf(prototypeObject!!))
        } else {
            editorPanel!!.commitChanges(objectList)
        }
    }

    /**
     * Set the state of the detail triangle on the object type editor
     *
     * @param isOpen if true the detail triangle is open, else closed.
     */
    fun setDetailTriangleOpen(isOpen: Boolean) {
        detailTriangle!!.setState(isOpen)
        syncPanelToTriangle()
    }
}