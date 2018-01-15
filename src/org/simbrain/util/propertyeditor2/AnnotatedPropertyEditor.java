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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.Parameter;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.UserParameter;
import org.simbrain.util.widgets.ParameterWidget;

/**
 * A panel for editing objects based on objects. Draws on annotations. Handles
 * inconsistent cases with null widgets.
 * 
 * To use simply initialize with a single object or list of objects to edit,
 * which contain the {@link UserParameter} annotation. When ready to write the
 * values of the panel to the underlying objects call commitChanges.
 * 
 * In the multiple object case, inconsistencies are handled by displaying "null"
 * values, like "..." in a text field, to indicate that the edited objects
 * currently have inconsistent states. Closing the dialog in that case without
 * changing the value will not change the values of the fields.
 *
 * @author Jeff Yoshimi
 * @author Oliver Coleman
 */
public class AnnotatedPropertyEditor extends JPanel {

    /**
     * The available parameters, as a map from Parameter to input gui component.
     */
    protected Set<ParameterWidget> widgets;

    // TODO: Use a collection instead?

    /**
     * The objects whose annotated fields will be edited using the editor.
     * Passing in a single object to edit is fine.
     */
    private List<? extends EditableObject> editedObjects;

    /** Main item panel. */
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
        this.editedObjects = objects;
        this.setLayout(new BorderLayout());
        add(itemPanel, BorderLayout.CENTER);
        initWidgets();
        fillFieldValues(editedObjects);
    }

    /**
     * Initialize the editor. Use first editable object in the list to
     * initialize a set of widgets (JComponents) for editing, based on their
     * class. Then set the values on these widgets using the values of the
     * editable objects. If they have inconsistent values use a null value.
     */
    private void initWidgets() {

        // Can be implemented to "null" state.
        if (editedObjects.isEmpty()) {
            return;
        }

        // Check that the objects given are of the same type
        boolean objectsSameType = editedObjects.stream().allMatch(
                m -> m.getClass().equals(editedObjects.get(0).getClass()));
        if (!objectsSameType) {
            throw new IllegalArgumentException(
                    "Edited objects must be of the same type as each other");
        }

        widgets = new TreeSet<>();
        for (Parameter param : Parameter
                .getParameters(editedObjects.get(0).getClass())) {
            widgets.add(new ParameterWidget(param));
        }

        // Add parameter widgets after collecting list of params so they're in
        // the right order.
        for (ParameterWidget pw : widgets) {
            JLabel label = new JLabel(pw.parameter.annotation.label());
            label.setToolTipText(pw.getToolTipText());
            itemPanel.addItemLabel(label, pw.component);
        }
    }

    /**
     * Fill the values of the editor panel widgets based on a list of objects.
     * These can be externally provided objects of the same type as those
     * maintained by the dialog (and then used in conjunction with
     * commitChanges(list)).
     * 
     * @objectsToEdit the objects whose values should be set using this panel.
     *                All objects must be of the same type as the objects
     *                maintained by this panel.
     */
    public void fillFieldValues(List<? extends EditableObject> objectsToFillFieldValues) {

        checkTypes(objectsToFillFieldValues);

        // Check to see if the field values are consistent over all given
        // instances.
        for (ParameterWidget pw : widgets) {
            boolean consistent = true;
            Object refValue = pw.parameter.getFieldValue(objectsToFillFieldValues.get(0));
            for (int i = 1; i < objectsToFillFieldValues.size(); i++) {
                Object obj = objectsToFillFieldValues.get(i);
                Object objValue = pw.parameter.getFieldValue(obj);
                if ((refValue == null && objValue != null)
                        || (refValue != null && !refValue.equals(objValue))) {
                    consistent = false;
                    break;
                }
            }

            if (!consistent) {
                pw.setWidgetValue(null);
            } else {
                pw.setWidgetValue(refValue);
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

    // TODO: Evaluate boolean returns from commmit as in EditablePanel

    /**
     * Commit changes on the internal object or list of objects
     */
    public void commitChanges() {
        commitChanges(editedObjects);
    }

    /**
     * Apply the values of the editor panel to a list of objects.
     * 
     * @objectsToEdit the objects whose values should be set using this panel.
     *                All objects must be of the same type as the objects
     *                maintained by this panel.
     */
    public void commitChanges(List<? extends EditableObject> objectsToEdit) {

        checkTypes(objectsToEdit);

        for (ParameterWidget pw : widgets) {

            Object value = pw.getWidgetValue();

            // Ignore unspecified values.
            // If the value isn't null and it's either not a String or not an
            // empty string.
            if (value != null && (!(value instanceof String)
                    || !((String) value).equals(""))) {
                for (Object o : objectsToEdit) {
                    pw.parameter.setFieldValue(o, value);
                }
            }
        }

        // Re-initialize. Allows updating cached values calculated from
        // parameters.
        for (Object o : objectsToEdit) {
            // s.getLearningRule().init(s);
            // TODO! If used add an init field to EditedObjects
        }
    }

    /**
     * Throw exception if objects are not the same type as each other
     * and as the objects maintained by the panel.
     */
    private void checkTypes(List<? extends EditableObject> objectsToCheck) {
        // Check that the objects given are of the same type
        if (objectsToCheck.isEmpty()) {
            return;
        }
        boolean objectsSameType = objectsToCheck.stream().allMatch(
                m -> m.getClass().equals(objectsToCheck.get(0).getClass()));
        boolean objectsSameTypeAsInternal = objectsToCheck.get(0).getClass()
                .equals(editedObjects.get(0).getClass());
        if (!objectsSameType || !objectsSameTypeAsInternal) {
            throw new IllegalArgumentException(
                    "Committed objects must be of the same type as each other and"
                            + "as the objects handled by this editor");
        }
    }

    /**
     * Returns an action for showing a property dialog for the provided objects.
     *
     * @param object object the object whose properties should be displayed
     * @return the action
     */
    public static AbstractAction getPropertiesDialogAction(
            final EditableObject object) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.png"));
                putValue(NAME, "Show properties...");
                putValue(SHORT_DESCRIPTION, "Show properties");
            }

            public void actionPerformed(ActionEvent arg0) {
                AnnotatedPropertyEditor editor = new AnnotatedPropertyEditor(
                        object);
                JDialog dialog = editor.getDialog();
                dialog.setModal(true);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }

        };
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
     * Extension of Standard Dialog for Editor Panel
     */
    private class EditorDialog extends StandardDialog {

        @Override
        protected void closeDialogOk() {
            AnnotatedPropertyEditor.this.commitChanges();
            dispose();
        }
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

}
