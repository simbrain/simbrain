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

import org.simbrain.util.BiMap
import org.simbrain.util.SimbrainConstants
import org.simbrain.util.callNoArgConstructor
import org.simbrain.util.propertyeditor.ParameterWidget.Companion.getTypeMap
import org.simbrain.util.widgets.DropDownTriangle
import java.awt.Dimension
import java.awt.Window
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import kotlin.reflect.KClass

/**
 * Panel for editing the type of a set of objects with a drop-down, and for
 * editing the properties of an object of that type based on class annotations
 * (with an [AnnotatedPropertyEditor]). The top of the panel has the
 * dropdown, and the main panel has the property editor. For example, the update
 * rule (Linear, Binary, etc) associated with a set of neurons can be selected
 * using the dropdown, and then the properties of that rule (e.g the Linear
 * Rule) can be edited using the property editor.
 *
 *
 * The value of this is that all the headaches for dealing with inconsistent
 * states are managed by this class.  The interface is also pretty simple and
 * clean. Finally, the internal property editor can itself contain
 * ObjectTypeEditor, so that we can edit some fairly complex objects using this
 * widget.
 *
 *
 * If the objects being edited are in a consistent state then they can be edited
 * directly with the annotated property editor.
 *
 *
 * If the objects are in an inconsistent state (e.g. some neurons are linear,
 * some are Decay), then a "..." appears in the combo box and no editor panel is
 * displayed. If at this point "ok" is pressed, then nothing is written to the
 * objects. If the dropdown box is changed the default values for the new type
 * are shown, and pressing "ok" then writes those values to every object being
 * edited.
 *
 *
 * To use this class:
 *
 *  * Designate the relevant type of object (e.g. NeuronUpdateRule,
 * LearningRule, etc.) as a [CopyableObject]
 *  * Annotate the object field (e.g. Neuron.neuronUpdateRule) using the
 * [org.simbrain.util.UserParameter] annotation, with "objectType" set to
 * true.
 *  *  Embed the object itself in a higher level AnnotatedPropertyEditor.
 *
 */
class ObjectTypeEditor private constructor(
    private val objectList: List<CopyableObject>, val typeMap: BiMap<String, KClass<*>>, val label: String, val showDetails: Boolean,
    private var parent: Window?
) : JPanel() {

    /**
     * The possible types of this object.
     */
    var dropDown: JComboBox<String> = JComboBox()
        private set

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
     * Initial state of the combo box.
     */
    private var cbStartState: String? = null

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
        val consistent = objectList.map { it::class }.toSet().size == 1
        if (!consistent) {
            cbStartState = SimbrainConstants.NULL_STRING
            editorPanel = AnnotatedPropertyEditor(emptyList())
        } else {
            cbStartState = typeMap.getInverse(objectList.first()::class)
            editorPanel = AnnotatedPropertyEditor(objectList)
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

        // Set up the combo box
        val labels: List<String?> = typeMap!!.keys.sortedBy { it }.toList()
        for (label in labels) {
            dropDown!!.addItem(label)
        }
        if (cbStartState === SimbrainConstants.NULL_STRING) {
            setNull()
        } else {
            dropDown!!.setSelectedItem(cbStartState)
        }
        topPanel.add(dropDown)
        addDropDownListener()

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
     * Set the editor to a null state (editing an inconsistent set of objects)
     *
     *
     * Should only be called once.
     */
    private fun setNull() {
        dropDown!!.removeAllItems()
        dropDown!!.addItem(SimbrainConstants.NULL_STRING)
        for (label in typeMap!!.keys) {
            dropDown!!.addItem(label)
        }
        dropDown!!.selectedIndex = 0
        dropDown!!.repaint()
    }

    val isInconsistent: Boolean
        /**
         * If true the editor is representing objects of different types
         * and the combo box has "..." in it.
         */
        get() = dropDown!!.selectedItem === SimbrainConstants.NULL_STRING

    /**
     * Initialize the combo box to react to events.
     */
    private fun addDropDownListener() {
        dropDown!!.addActionListener { e: ActionEvent? ->

            // As soon as the dropdown is changed once, it's in prototype mode. User
            // must cancel to stop prototype mode
            isPrototypeMode = true

            // Create the prototype object and refresh editor panel
            try {
                val proto = typeMap[dropDown!!.selectedItem as String]!!
                prototypeObject = proto.callNoArgConstructor() as CopyableObject
                editorPanel = AnnotatedPropertyEditor(prototypeObject!!)
                editorPanelContainer!!.removeAll()
                editorPanelContainer!!.add(editorPanel)
                updateEditorPanel()
                syncPanelToTriangle()
                objectChangedTask.run()
            } catch (exception: Exception) {
                exception.printStackTrace()
            }

            // Can't go back to null once leaving it
            dropDown!!.removeItem(SimbrainConstants.NULL_STRING)

            // TODO: Remove or at least simplify
            // Maybe move to some utility class.  Like Simbrain.pack().
            if (parent == null) {
                parent = SwingUtilities.getWindowAncestor(this)
            }
            if (parent != null) {
                parent!!.pack()
            }
        }
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
        if (editorPanel!!.widgets == null) {
            return
        }

        // In the case where the object has no properties at all to edit, and
        // the type editor is just being used to set a type, just remove
        // everything from the panel, and hide the detail triangle.
        if (editorPanel!!.widgets.size == 0) {
            detailTriangle!!.isVisible = false
            editorPanel!!.removeAll()
        } else {
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
            parent = SwingUtilities.getWindowAncestor(this@ObjectTypeEditor)
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
        get() = if (dropDown!!.selectedItem === SimbrainConstants.NULL_STRING) {
            null
        } else {
            if (isPrototypeMode) {
                prototypeObject
            } else {
                objectList[0]
            }
        }

    /**
     * Commit any changes made.
     */
    fun commitChanges() {
        if (dropDown!!.selectedItem === SimbrainConstants.NULL_STRING) {
            return
        }
        if (isPrototypeMode) {
            // Sync prototype object to editor panel
            editorPanel!!.commitChanges(mutableListOf(prototypeObject!!))
            // TODO: The object type change happens in the ape. Not sure why it has to happen there
            // for (EditableObject o : objectList) {
            //    o = prototypeObject.copy();
            //}
        } else {
            editorPanel!!.commitChanges(objectList)
        }
    }

    /**
     * If disabled then the combo box is enabled.  To get rid of the detail triangle it can be set to invisible
     * but that is not needed in the one use case for this so far.
     */
    override fun setEnabled(enabled: Boolean) {
        dropDown!!.isEnabled = enabled
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

    companion object {
        /**
         * Provides external access to object type editors for separate use. The method name containing the type list
         * must be specified.
         */
        fun createEditor(
            objects: List<CopyableObject>, typeListMethod: String?,
            label: String, showDetails: Boolean
        ): ObjectTypeEditor {
            val typeMap: BiMap<String, KClass<*>> = getTypeMap(
                objects[0]::class, typeListMethod
            )
            return ObjectTypeEditor(
                objects, typeMap, label, showDetails, null
            )
        }

        /**
         * Create the editor.
         *
         * @param objects the objects to edit
         * @param tm      type map
         * @param label   label to use for display
         * @param showDetails if true open with the detail triangle open; else open with it closed.
         * @return the editor object
         */
        fun createEditor(
            objects: List<CopyableObject>, tm: BiMap<String, KClass<*>>,
            label: String, showDetails: Boolean
        ): ObjectTypeEditor {
            return ObjectTypeEditor(objects, tm, label, showDetails, null)
        }
    }
}