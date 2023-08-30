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

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.*
import org.simbrain.util.widgets.EditablePanel
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.*
import kotlin.reflect.full.functions

/**
 * Annotated property editor (or APE) is panel for editing collections of
 * objects based on [UserParameter] annotations. Each annotated field is
 * represented by an appropriate java JComponent (often a text field but also
 * special drop downs for booleans, etc), via the [ParameterWidget] class.
 * When all the objects in the edited collection have the same value, it is
 * shown in the widget.  When they have different values a null value "..."
 * is shown.  Null values are ignored when the panel is closed, and any values
 * in the panel are written to it.
 *
 * To use simply initialize with a single object or list of objects to edit. These
 * objects must instantiate [EditableObject]. The fields that should be editable
 * are annotated with the [UserParameter] annotation.
 *
 * Note that annotated interfaces must be in Kotlin.
 *
 * Object types are created by updating a prototype object, then copying it. Thus objects using the object type
 * editor must instantiate [CopyableObject].
 *
 * You can also use the editor to build a more customized panel but using the
 * property editor as a holder that can then return JComponents for specific
 * items which you then lay out by hand.  For an example see the Neuron Dialog
 * classes.
 *
 * @author Jeff Yoshimi
 * @author Oliver Coleman
 */
class AnnotatedPropertyEditor1(objects: List<EditableObject>) : EditablePanel() {
    // TODO: Deal explicitly with empty list case using "null window"
    // TODO: Use a collection instead of a list of editable objects?
    /**
     * The widgets to display / adjust annotated fields.
     */
    @JvmField
    var widgets: MutableSet<ParameterWidget> = mutableSetOf()

    /**
     * The objects whose annotated fields will be edited using the editor.
     */
    var editedObjects: List<EditableObject> = listOf()

    /**
     * The main panel, which is a tabbedPane if tabs are used, and a LabelledItemPanel otherwise.
     */
    private var mainPanel: JComponent? = null
    private val tabPanels: MutableMap<String, LabelledItemPanel> = TreeMap()

    /**
     * Construct with one object.
     *
     * @param toEdit the object to edit
     */
    constructor(toEdit: EditableObject) : this(listOf<EditableObject>(toEdit)) {
        // TODO: Possibly treat this as a special case, since it does not
        // require any consistency checks. This would make it
        // possible, for example, to use a regular checkbox
        // If so, put it in a special creation method, like
        // createSingleObjectEditor, and document that it doesn't handle
        // consistency checks.
    }

    /**
     * Construct with a list of objects.
     */
    init {
        editedObjects = objects
        layout = BorderLayout()
        initPanel()
        fillFieldValues(editedObjects)
        onWidgetChanged()
        add(mainPanel, BorderLayout.CENTER)
    }

    /**
     * Call when widget changed to updated conditional enabling.
     */
    fun onWidgetChanged() {
        val labelValueMap = widgets
            .filter { it.parameter.hasValue }
            .associate { it.label to it.widgetValue }

        widgets.filter { it.parameter.annotation.conditionalEnablingMethod.isNotEmpty() }
            .forEach { it.checkConditionalEnabling(labelValueMap) }

        // if refresh source is specified, then update the widget based on the value of the specified source
        // only works for embedded objects
        widgets.filter { it.parameter.annotation.refreshSource.isNotEmpty() }
            .forEach {
                val (objectName, functionName) = it.parameter.refreshSource.split(".")
                val sourceObject = (widgets.first { w -> w.parameter.name == objectName }.component as ObjectTypeEditor).prototypeObject
                sourceObject?.let { so ->
                    val result = so::class.functions.first { fn -> fn.name == functionName }.call(so) as CopyableObject
                    (it.component as EmbeddedObjectEditor).refreshPrototype(result)
                }
            }
    }

    /**
     * Initialize the editor. Use the first editable object in the object list
     * to initialize a set of widgets (JComponents) for editing, based on their
     * classes.
     */
    protected fun initPanel() {
        if (editedObjects.isEmpty()) {
            return
        }

        // If any two objects are different, then one will match the first element in the list and the other won't.
        val objectsSameType =
            editedObjects.stream().allMatch { m: EditableObject? -> m!!.javaClass == editedObjects[0]!!.javaClass }
        require(objectsSameType) { "Edited objects must be of the same type as each other" }

        // Create a list of widgets
        widgets = TreeSet()
        val parameters = Parameter.getParameters(
            editedObjects.first()::class
        )
        // parameters.forEach{p -> widgets.add(ParameterWidget(this, p)) }

        // If there are no tab annotations, do not create tab bar
        val numTabAnnotations = parameters.count { p -> p.annotation.tab.isNotEmpty() }
        mainPanel = if (numTabAnnotations == 0) {
            LabelledItemPanel()
        } else {
            JTabbedPane()
        }

        // Add parameter widgets after collecting list of params so they're in
        // the right order.
        for (pw in widgets) {
            // handle widget types that don't use a label
            if (pw.parameter.isObjectType || pw.parameter.isEmbeddedObject) {
                if (isTabbedPane) {
                    addItemToTabPanel(pw)
                } else {
                    // Label is redundant for these types because it gets added in a border box
                    (mainPanel as LabelledItemPanel).addItem(pw.component)
                }
            } else {
                if (isTabbedPane) {
                    val label = JLabel(pw.parameter.annotation.label)
                    label.toolTipText = pw.toolTipText
                    addItemToTabPanel(pw, label)
                } else {
                    val label = (mainPanel as LabelledItemPanel).addItem(pw.label, pw.component)
                    label.toolTipText = pw.toolTipText
                }
            }
        }
    }

    private val isTabbedPane: Boolean
        get() = mainPanel is JTabbedPane

    /**
     * Fill all field values for the edited objects.
     */
    override fun fillFieldValues() {
        fillFieldValues(editedObjects)
    }

    // TODO: A problem arises in relation to the type checks below for
    // multi-valued objects. E.g. Uniform and Normal relative to ProbabilityDistribution
    // Those objects are not identical to each other, but to share a common super-class.
    // Not sure how to fix, but will require code in checktypes

    /**
     * Fill the values of the editor panel widgets based on a list of objects.
     * These can be externally provided objects of the same type as those
     * maintained by the dialog (and then used in conjunction with
     * commitChanges(list)).
     *
     * Check for consistency happens here. If the objects are inconsistent, a
     * null value is set.
     *
     * @objectList the objects whose values should be set using this panel. All
     * objects must be of the same type as the objects maintained by this
     * panel.
     */
    fun fillFieldValues(objectList: List<EditableObject?>) {
        if (objectList.isEmpty()) {
            return
        }

        // Check to see if the field values are consistent over all given
        // instances.
        for (pw in getWidgets()!!) {

            // When using a custom initial value then don't do the consistency check.
            // (The objects themselves have not yet been set so custom initial values automatically trigger
            // inconsistency here).
            if (pw.isCustomInitialValue && objectList.size == 1) {
                continue
            }
            if (pw.parameter.isEmbeddedObject) {
                (pw.component as EmbeddedObjectEditor).fillFieldValues()
                continue
            }
            var consistent = true
            var refValue = pw.parameter.getFieldValue(objectList[0])
            if (pw.parameter.isObjectType) {
                refValue = refValue!!.javaClass
            }
            for (i in 1 until objectList.size) {
                val obj: Any? = objectList[i]
                var objValue = pw.parameter.getFieldValue(obj)
                if (pw.parameter.isObjectType) {
                    objValue = objValue!!.javaClass
                }
                // System.out.println("ref value:" + refValue + " == object value:" + objValue + "\n");
                if (refValue == null && objValue != null || refValue != null && refValue != objValue) {
                    consistent = false
                    break
                }
            }

            // Null values below are passed on to the JComponents, which are
            // assumed to be able to handle some kind of null state representing
            // inconsistent objects.  So e.g. ObjectTypeEditor should be put in
            // a null state by this call.
            if (!consistent) {
                pw.widgetValue = null
            } else {
                pw.widgetValue = refValue
                if (pw.parameter.isObjectType) {
                    (pw.component as ObjectTypeEditor).fillFieldValues()
                }
            }
        }
    }
    //TODO: Below not currently throwing an exception, while still testing.
    // But once everything is working better make it throw an exception!
    // Also again we are not checking all to all, but all to one.

    /**
     * Check whether objects are the same type as each other and as the objects
     * maintained by the panel.
     */
    private fun checkTypes(objectsToCheck: List<EditableObject>): Boolean {
        // Check that the objects given are of the same type
        if (objectsToCheck.isEmpty()) {
            return false
        }
        val objectsSameType = objectsToCheck.stream()
            .allMatch { m: EditableObject? -> m!!.javaClass == objectsToCheck[0]!!.javaClass }
        val objectsSameTypeAsInternal = objectsToCheck[0]!!.javaClass == editedObjects[0].javaClass
        if (!objectsSameType || !objectsSameTypeAsInternal) {
            val exceptionString = ("Objects of type " + objectsToCheck[0]!!.javaClass
                    + " do not match edited object of type" + editedObjects[0].javaClass)
            System.err.println(exceptionString)
            return false
        }
        return true
    }

    /**
     * Commit changes on the internal object or list of objects.
     */
    override fun commitChanges(): Boolean {
        commitChanges(editedObjects)
        return true
    }

    /**
     * Apply the values of the editor panel to a list of objects.
     *
     * @objectsToEdit the objects whose values should be set using this panel.
     * All objects must be of the same type as the objects maintained by this
     * panel.
     */
    fun commitChanges(objectsToEdit: List<EditableObject>) {
        if (!checkTypes(objectsToEdit)) {
            return
        }

        // Commit each widget's value to all objects in list
        for (pw in widgets) {
            if (pw.parameter.isDisplayOnly) {
                continue
            }
            val widgetValue = pw.widgetValue ?: continue

            if (pw.parameter.isEmbeddedObject) {
                (pw.component as EmbeddedObjectEditor).commitChanges()
                // if the value has changed (prototype mode) it must be written back to the parent object
                if (pw.component.isPrototypeMode) {
                    for (o in objectsToEdit) {
                        pw.parameter.setFieldValue(o, (widgetValue as CopyableObject).copy())
                    }
                }
                continue
            }
            if (pw.parameter.isObjectType) {
                (pw.component as ObjectTypeEditor).commitChanges()
                // if the value has changed (prototype mode) it must be written back to the parent object
                if (pw.component.isPrototypeMode) {
                    for (o in objectsToEdit) {
                        pw.parameter.setFieldValue(o, (widgetValue as CopyableObject).copy())
                    }
                }
                continue
            }

            // If the widget is in "..." mode don't do anything with it
            if (!pw.isInconsistent) {
                for (o in objectsToEdit) {
                    pw.parameter.setFieldValue(o, widgetValue)
                }
            }
        }
        objectsToEdit.forEach { o -> o.onCommit() }
    }

    /**
     * Returns a string describing what object or objects are being edited
     */
    val titleString: String
        get() {
            return if (editedObjects.size == 1) {
                "Edit ${editedObjects[0].name}"
            } else {
                "Edit " + editedObjects.size + " " + editedObjects[0].javaClass.simpleName + "s"
            }
        }

    /**
     * Extension of Standard Dialog for Editor Panel
     */
    inner class EditorDialog : StandardDialog() {
        override fun closeDialogOk() {
            commitChanges()
            super.closeDialogOk()
            dispose()
        }
    }

    /**
     * Returns an dialog containing this property editor.
     *
     * @return parentDialog parent dialog
     */
    val dialog: EditorDialog
        get() {
            val ret: EditorDialog = EditorDialog()
            ret.title = titleString
            ret.contentPane = this
            return ret
        }

    /**
     * Returns the first object in the list of objects, which should be the only
     * object in the list for the case of editing a single object.
     *
     * @return the edited object
     */
    val editedObject: EditableObject?
        get() = if (editedObjects.isEmpty()) null else editedObjects[0]

    /**
     * Returns the widgets, which can then be used to populate custom panels, in
     * which case the AnnotatedPropertyEditor1 is used as a container for holding
     * field editors but editor itself is not displayed.
     *
     * @return the set of widgets representing user parameters
     */
    fun getWidgets(): Set<ParameterWidget>? {
        return widgets
    }

    /**
     * Returns a widget with a provided label, or null if none found. Used in
     * building custom panels (see [.getWidgets]).
     *
     * @param label the label to use for searching
     * @return matching widget, or null if none found
     */
    fun getWidget(label: String): ParameterWidget? {
        return widgets.firstOrNull{it.label.equals(label, ignoreCase = true)}
    }

    private fun ParameterWidget.getTabPane(): LabelledItemPanel {
        val tabName = parameter.annotation.tab.ifEmpty { "Main" }
        addTabPanel(tabName)
        return tabPanels[tabName]!!
    }

    /**
     * Add a ParameterWidget to its corresponding tab panel.
     *
     * @param pw the ParameterWidget to add
     */
    private fun addItemToTabPanel(pw: ParameterWidget, label: JLabel? = null) {
        if (label != null) {
            pw.getTabPane().addItemLabel(label, pw.component)
        } else {
            pw.getTabPane().addItem(pw.component)
        }
    }

    /**
     * Creates content panel with a specified name.
     */
    private fun addTabPanel(tabName: String) {
        if (!tabPanels.containsKey(tabName)) {
            val newLabelledItemPanel = LabelledItemPanel()
            tabPanels[tabName] = newLabelledItemPanel
            (mainPanel as JTabbedPane?)!!.addTab(tabName, newLabelledItemPanel)
        }
    }

    /**
     * Add an item to the main panel or, if tabs, the first tab.
     */
    fun addItem(item: JComponent?) {
        if (item == null) {
            return
        }
        if (isTabbedPane) {
            tabPanels.values.iterator().next().addItem(item)
        } else {
            (mainPanel as LabelledItemPanel?)!!.addItem(item)
        }
    }

    fun removeItem(item: JComponent?) {
        if (item != null) {
            if (isTabbedPane) {
                tabPanels.values.iterator().next().remove(item)
            } else {
                mainPanel!!.remove(item)
            }
        }
    }

    /**
     * Returns the tabbed pain or null if there is none.
     */
    fun getTabbedPane(): JTabbedPane? {
        return if (isTabbedPane) {
            mainPanel as JTabbedPane?
        } else null
    }

    /**
     * Open or close all the detail triangles in any [ObjectTypeEditor] widgets this editor contains.
     *
     * @param open if true, open the detail triangles; else close them
     */
    fun setDetailTrianglesOpen(open: Boolean) {
        widgets!!.stream()
            .map { obj: ParameterWidget -> obj.component }
            .filter { obj: JComponent? -> ObjectTypeEditor::class.java.isInstance(obj) }
            .map { obj: JComponent? -> ObjectTypeEditor::class.java.cast(obj) }
            .forEach { oe: ObjectTypeEditor -> oe.setDetailTriangleOpen(open) }
    }

    companion object {
        /**
         * Returns an action for showing a property dialog for the provided
         * objects.
         *
         * @param object object the object whose properties should be displayed
         * @return the action
         */
        fun getPropertiesDialogAction(`object`: EditableObject): AbstractAction {
            return object : AbstractAction() {
                // Initialize
                init {
                    putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Prefs.png"))
                    putValue(NAME, "Show properties...")
                    putValue(SHORT_DESCRIPTION, "Show properties")
                }

                override fun actionPerformed(arg0: ActionEvent) {
                    val editor = AnnotatedPropertyEditor1(`object`)
                    val dialog: JDialog = editor.dialog
                    dialog.isModal = true
                    dialog.pack()
                    dialog.setLocationRelativeTo(null)
                    dialog.isVisible = true
                }
            }
        }

        /**
         * Utility to create and return a property editor on an editable object.
         */
        fun getDialog(eo: EditableObject): StandardDialog {
            val ape = AnnotatedPropertyEditor1(eo)
            return ape.dialog
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val net = Network()
            val neurons = listOf(Neuron(net), Neuron(net))
            val ape = AnnotatedPropertyEditor1(neurons)
            ape.displayInDialog().also {
                it.title = ape.titleString
            }
        }
    }
}