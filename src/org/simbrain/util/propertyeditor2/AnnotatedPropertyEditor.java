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
package org.simbrain.util.propertyeditor2;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.Parameter;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.UserParameter;
import org.simbrain.util.widgets.ParameterWidget;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * A panel for editing collections of objects based on {@link UserParameter}
 * annotations. Each annotated field is represented by an appropriate java
 * JComponent (often a text field but also special drop downs for booleans,
 * etc), via the {@link ParameterWidget} class. When all the objects in the
 * edited collection have the same value, it is shown in the widget.  When they
 * have different values it a null value "..." is shown.  Null values are
 * ignored when the panel is closed, and any values in the panel are written to
 * it.
 * <p>
 * To use simply initialize with a single object or list of objects to edit,
 * which contain the {@link UserParameter} annotation. When ready to write the
 * values of the panel to the underlying objects call commitChanges.
 * <p>
 * The class can also be used in "apply" mode: the editor values can be used to
 * set the values of a set of compatible objects using fillFieldValues(List<EditableObject>)
 * and commitChanges(List<EditableObject>)
 * <p>
 * You can also use the editor to build a more customized panel but using the
 * property editor as a holder that can then return JComponents for specific
 * items which you then lay out by hand.  For an example see the Neuron Dialog
 * classes.
 * <p>
 *
 * @author Jeff Yoshimi
 * @author Oliver Coleman
 */
public class AnnotatedPropertyEditor extends JPanel {

    // TODO: Make constructors private and use creation methods
    // TODO: Handle single object editing separately from editing a list
    // TODO: Allow conditions to be introduced where one component disables /enables another
    // TODO: Deal explicitly with empty list case using "null window"
    // TODO: Use a collection instead of a list of editable objects?

    /**
     * The widgets to display / adjust annotated fields.
     */
    protected Set<ParameterWidget> widgets;

    /**
     * The objects whose annotated fields will be edited using the editor.
     */
    private List<? extends EditableObject> editedObjects = Collections.EMPTY_LIST;

    /**
     * Main item panel.
     */
    private LabelledItemPanel itemPanel = new LabelledItemPanel();

    /**
     * Construct with one object.
     *
     * @param toEdit the object to edit
     */
    public AnnotatedPropertyEditor(EditableObject toEdit) {
        this(Collections.singletonList(toEdit));
    }

    /**
     * Construct with a list of objects.
     *
     * @param objects
     */
    public AnnotatedPropertyEditor(List<? extends EditableObject> objects) {

        if (objects.isEmpty()) {
            return;
        }
        this.editedObjects = objects;
        setLayout(new BorderLayout());
        add(itemPanel, BorderLayout.CENTER);
        initPanel();
        fillFieldValues(editedObjects);
    }

    /**
     * Initialize the editor. Use the first editable object in the object list
     * to initialize a set of widgets (JComponents) for editing, based on their
     * classes.
     */
    protected void initPanel() {

        if (editedObjects.isEmpty()) {
            return;
        }

        // TODO: This only checks whether all objects are the same as the first object in the list. Maybe that is enough...
        boolean objectsSameType = editedObjects.stream().allMatch(m -> m.getClass().equals(editedObjects.get(0).getClass()));
        if (!objectsSameType) {
            throw new IllegalArgumentException("Edited objects must be of the same type as each other");
        }

        // Create a list of widgets
        widgets = new TreeSet<>();

        for (Parameter param : Parameter.getParameters(editedObjects.get(0).getClass())) {

            if (param.isMultiState()) {

                // ObjectTypeEditors require special initialization

                // Create a list of objects corresponding to the field associated with the parameter
                // E.g. a list of neuronupdaterules objects within a list of neuron objects
                List objectList = new ArrayList();
                for (Object o : editedObjects) {
                    objectList.add(param.getFieldValue(o));
                }

                ParameterWidget pw = new ParameterWidget(param, objectList);


//                // Use this list of objects to complete the initalization of the
//                // ObjectTypeEditor.
//                ((ObjectTypeEditor) pw.component).setObjects(subList);
                widgets.add(pw);
            } else {
                ParameterWidget pw = new ParameterWidget(param);
                widgets.add(pw);
            }

        }

        // Add parameter widgets after collecting list of params so they're in
        // the right order.
        for (ParameterWidget pw : widgets) {
            if (pw.parameter.isMultiState()) {
                itemPanel.addSpanningItem(pw.component);
            } else {
                JLabel label = new JLabel(pw.parameter.annotation.label());
                label.setToolTipText(pw.getToolTipText());
                itemPanel.addItemLabel(label, pw.component);
            }
        }
    }

    /**
     * Fill all field values for the edited objects.
     */
    public void fillFieldValues() {
        fillFieldValues(editedObjects);
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
     * <p>
     * Check for consistency happens here. If the objects are inconsistent, a
     * null value is set.
     *
     * @objectList the objects whose values should be set using this panel. All
     * objects must be of the same type as the objects maintained by this
     * panel.
     */
    public void fillFieldValues(List<? extends EditableObject> objectList) {

        boolean consistent = true;

        if (objectList.isEmpty()) {
            return;
        }

//        if (!checkTypes(objectList)) {
//            // TODO: Without this, can't change randomizers, but CAN change neuron type.
//            // This is a HACK to get it to work...
//            for (ParameterWidget pw : widgets) {
//                if (pw.parameter.isMultiState()) {
//                    ((ObjectTypeEditor) pw.component).setNull();
//                }
//            }
//            return;
//        }

        // Check to see if the field values are consistent over all given
        // instances.
        for (ParameterWidget pw : widgets) {

            Object refValue = pw.parameter.getFieldValue(objectList.get(0));
            if (pw.parameter.isMultiState()) {
                refValue = refValue.getClass();
            }

            for (int i = 1; i < objectList.size(); i++) {
                Object obj = objectList.get(i);
                Object objValue = pw.parameter.getFieldValue(obj);
                if (pw.parameter.isMultiState()) {
                    objValue = objValue.getClass();
                }
                // System.out.println("ref value:" + refValue + " == object value:" + objValue + "\n");
                if ((refValue == null && objValue != null) || (refValue != null && !refValue.equals(objValue))) {
                    consistent = false;
                    break;
                }
            }

            // Null values below are passed on to the JComponents, which are
            // assumed to be able to handle some kind of null state representing
            // inconsistent objects.  So e.g. ObjectTypeEditor should be put in
            // a null state by this call.
            if (!consistent) {
                pw.setWidgetValue(null);
            } else {
                pw.setWidgetValue(refValue);
                if (pw.parameter.isMultiState()) {
                    ((ObjectTypeEditor) pw.component).fillFieldValues();
                }

            }
        }
    }

    /**
     * Fill widgets with default values given in the annotations.
     */
    public void fillDefaultValues() {
        for (ParameterWidget pw : widgets) {
            pw.setWidgetValue(pw.parameter.getDefaultValue());
        }
    }

    //TODO: Below not currently throwing an exception, while still testing.
    // But once everything is working better make it throw an exception!
    // Also again we are not checking all to all, but all to one.

    /**
     * Check whether objects are the same type as each other and as the objects
     * maintained by the panel.
     */
    private boolean checkTypes(List<? extends EditableObject> objectsToCheck) {
        // Check that the objects given are of the same type
        if (objectsToCheck.isEmpty()) {
            return false;
        }
        boolean objectsSameType = objectsToCheck.stream().allMatch(m -> m.getClass().equals(objectsToCheck.get(0).getClass()));
        boolean objectsSameTypeAsInternal = objectsToCheck.get(0).getClass().equals(editedObjects.get(0).getClass());
        if (!objectsSameType || !objectsSameTypeAsInternal) {
            String exceptionString = "Objects of type " + objectsToCheck.get(0).getClass()
                + " do not match edited object of type" + editedObjects.get(0).getClass();
            System.err.println(exceptionString);
            return false;
        }
        return true;
    }

    /**
     * Commit changes on the internal object or list of objects.
     */
    public void commitChanges() {
        commitChanges(editedObjects);
    }

    /**
     * Apply the values of the editor panel to a list of objects.
     *
     * @objectsToEdit the objects whose values should be set using this panel.
     * All objects must be of the same type as the objects maintained by this
     * panel.
     */
    public void commitChanges(List<? extends EditableObject> objectsToEdit) {

        if (!checkTypes(objectsToEdit)) {
            return;
        }

        // Commit each widgets value to all objects in list
        for (ParameterWidget pw : widgets) {

            Object widgetValue = pw.getWidgetValue();
            if (widgetValue == null) {
                // Don't save widgets in inconsistent state.
                // System.out.println("null widget, not saving");
                break;
            }

            if (pw.parameter.isMultiState()) {

                ((ObjectTypeEditor) pw.component).commitChanges();

                // TODO: Can this be migrated to the object type editor?
                // Only overrwrite objects if combo box has changed
                if (((ObjectTypeEditor) pw.component).isPrototypeMode()) {
                    // Reset the types of all the objects to copies of the displayed object
                    for (EditableObject o : objectsToEdit) {
                        pw.parameter.setFieldValue(o, ((CopyableObject) widgetValue).copy());
                        // System.out.println("Rewriting object " + o + "," + widgetValue);
                    }
                }
                continue;
            }

            // Ignore unspecified values.
            // If the value isn't null and it's either not a String or not an
            // empty string.
            if ((!(widgetValue instanceof String) || !((String) widgetValue).equals(""))) {
                for (Object o : objectsToEdit) {
                    pw.parameter.setFieldValue(o, widgetValue);
                }
            }

        }

    }

    /**
     * Returns an action for showing a property dialog for the provided
     * objects.
     *
     * @param object object the object whose properties should be displayed
     * @return the action
     */
    public static AbstractAction getPropertiesDialogAction(final EditableObject object) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.png"));
                putValue(NAME, "Show properties...");
                putValue(SHORT_DESCRIPTION, "Show properties");
            }

            public void actionPerformed(ActionEvent arg0) {
                AnnotatedPropertyEditor editor = new AnnotatedPropertyEditor(object);
                JDialog dialog = editor.getDialog();
                dialog.setModal(true);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }

        };
    }

    /**
     * Extension of Standard Dialog for Editor Panel
     */
    private class EditorDialog extends StandardDialog {
        @Override
        protected void closeDialogOk() {
            commitChanges();
            dispose();
        }
    }

    /**
     * Returns an dialog containing this property editor.
     *
     * @return parentDialog parent dialog
     */
    public EditorDialog getDialog() {

        final EditorDialog ret = new EditorDialog();
        ret.setContentPane(this);
        return ret;
    }

    /**
     * Returns the first object in the list of objects, which should be the only
     * object in the list for the case of editing a single object.
     *
     * @return the edited object
     */
    public EditableObject getEditedObject() {
        return editedObjects.isEmpty() ? null : editedObjects.get(0);
    }

    /**
     * Returns the list of edited objects.
     *
     * @return the objects being edited
     */
    public List<? extends EditableObject> getEditedObjects() {
        return editedObjects;
    }

    /**
     * Returns the widgets, which can then be used to populate custom panels, in
     * which case the AnnotatedPropertyEditor is used as a container for holding
     * field editors but editor itself is not displayed.
     *
     * @return the set of widgets representing user parameters
     */
    public Set<ParameterWidget> getWidgets() {
        return widgets;
    }

    /**
     * Returns a widget with a provided label, or null if none found. Used in
     * building custom panels (see {@link #getWidgets()}).
     *
     * @param label the label to use for searching
     * @return matching widget, or null if none found
     */
    public ParameterWidget getWidget(String label) {
        for (ParameterWidget w : widgets) {
            if (w.getLabel().equalsIgnoreCase(label)) {
                return w;
            }
        }
        return null;
    }

}
