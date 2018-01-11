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

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
 * To use simply initialize with a single object or list of objects to edit, which 
 * contain the {@link UserParameter} annotation. When ready to write the values of 
 * the panel to the underlying objects call commitChanges.
 * 
 * In the multiple object case, inconsistencies are handled by displaying "null" values,
 * like "..." in a text field, to indicate that the edited objects currently have
 * inconsistent states. Closing the dialog in that case without changing the value
 * will not change the values of the fields.
 *
 * @author Oliver Coleman
 * @author Jeff Yoshimi
 */
public class AnnotatedPropertyEditor extends JPanel {

    /**
     * The available parameters, as a map from Parameter to input gui component.
     */
    protected Set<ParameterWidget> widgets;

    /**
     * The objects whose annotated fields will be edited using the
     * editor. Passing in a single object to edit is fine.
     */
    private List<EditableObject> editedObjects;

    /** Main item panel. */
    private LabelledItemPanel itemPanel = new LabelledItemPanel();

    /**
     * No-arg constructor. Mostly for internal use.
     */
    public AnnotatedPropertyEditor() {
        add(itemPanel);
    }
    
    /**
     * Construct with one object.
     *
     * @param toEdit the object to edit
     */
    public AnnotatedPropertyEditor(final EditableObject toEdit) {
        this(Collections.singletonList(toEdit));
    }

    /**
     * Construct with a list of objects.
     *
     * @param objects
     */
    public AnnotatedPropertyEditor(List<EditableObject> objects) {
        this.editedObjects = objects;
        add(itemPanel);
        init();
    }
    
    /**
     * Initialize the editor. Use first editable object in the list to
     * initialize a set of widgets (JComponents) for editing, based on their
     * class. Then set the values on these widgets using the values of the
     * editable objects. If they have inconsistent values use a null value.
     */
    private void init() {
//        if (this.editedObjects != null) {
//            throw new IllegalStateException(
//                    "Multiple calls to setObjectToEdit are not allowed.");
//        }

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
        
        // Check to see if the field values are consistent over all given
        // instances.
        for (ParameterWidget pw : widgets) {
            boolean consistent = true;
            Object refValue = pw.parameter.getFieldValue(editedObjects.get(0));
            for (int i = 1; i < editedObjects.size(); i++) {
                Object obj = editedObjects.get(i);
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

    /**
     * Commit changes on edited object. 
     */
    public void commitChanges() {
        for (ParameterWidget pw : widgets) {

            Object value = pw.getWidgetValue();
            
            // Ignore unspecified values.
            // If the value isn't null and it's either not a String or not an
            // empty string.
            if (value != null && (!(value instanceof String)
                    || !((String) value).equals(""))) {
                for (Object o : editedObjects) {
                    pw.parameter.setFieldValue(o, value);
                }
            }
        }

        // Re-initialize. Allows updating cached values calculated from
        // parameters.
        for (Object o : editedObjects) {
            //s.getLearningRule().init(s); 
            // TODO!  If usd add an init field to EditedObjects
        }
    }
    

    // TODO: Remove? 
    
    /**
     * Provides a copy of this panel.
     * 
     * @return a copy of the panel.
     */
    public AnnotatedPropertyEditor copy() {
        AnnotatedPropertyEditor copy;
        try {
            copy = this.getClass().getConstructor().newInstance();
            // If a (sub-class) constructor didn't call
            // SynapseRuleUserParamPanel(SynapseUpdateRule)
            if (copy.editedObjects == null) {
                copy.editedObjects = new ArrayList(editedObjects);
                copy.init();
            }
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(
                    "The class " + this.getClass().getName()
                            + " must declare a constructor accepting no arguments (or override the deepCopy() method).",
                    e);
        }

        
        // Iterate over both sets of parameters. They should be in the same
        // order, unless something very odd is going on with the loaded classes.
        Iterator<ParameterWidget> thisParamsItr = widgets.iterator();
        Iterator<ParameterWidget> copyParamsItr = copy.widgets.iterator();
        while (thisParamsItr.hasNext()) {
            ParameterWidget thisPW = thisParamsItr.next();
            ParameterWidget copyPW = copyParamsItr.next();
            assert (thisPW.equals(copyPW));
            copyPW.setWidgetValue(thisPW.getWidgetValue());
        }
        return copy;
    }
    
    // TODO: Remove if not used
    /**
     * Returns an action for showing a property dialog for the provided objects.
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
        return editedObjects.get(0);
    }

    /**
     * Returns the list of edited objects.
     * 
     * @return the objects being edited
     */
    public List<EditableObject> getEditedObjects() {
        return editedObjects;
    }




}
