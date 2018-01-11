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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
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
import org.simbrain.util.widgets.ParameterWidget;

/**
 * A panel for editing objects based on objects.  Based on work by Oliver Coleman.
 *
 * @author Jeff Yoshimi
 */
public class AnnotatedPropertyEditor<T> extends JPanel {

    /**
     * The available parameters, as a map from Parameter to input gui component.
     */
    protected Set<ParameterWidget> params;

    /**
     * The object whose public fields will be edited using the
     * ReflectivePropertyEditor
     */
    private Object editedObject;

    /** Main item panel. */
    private LabelledItemPanel itemPanel = new LabelledItemPanel();

    /**
     * No-arg constructor. Mostly for internal use.
     */
    public AnnotatedPropertyEditor() {
        this.setBackground(Color.red);
    }

    /**
     * Construct the panel.
     *
     * @param toEdit the object to edit
     */
    public AnnotatedPropertyEditor(final Object toEdit) {
        this.setBackground(Color.red);
        this.setObjectToEdit(toEdit);
    }

    public void setObjectToEdit(Object toEdit) {
        if (this.editedObject != null) {
            throw new IllegalStateException(
                    "Multiple calls to SynapseRuleUserParamPanel.setRule(SynapseUpdateRule) are not allowed.");
        }

        this.editedObject = toEdit;

        params = new TreeSet<>();
        for (Parameter param : Parameter
                .getParameters(toEdit.getClass())) {
            params.add(new ParameterWidget(param));
        }

        // Add parameter widgets after collecting list of params so they're in
        // the right order.
        for (ParameterWidget pw : params) {
            JLabel label = new JLabel(pw.parameter.annotation.label());
            label.setToolTipText(pw.getToolTipText());
            itemPanel.addItemLabel(label, pw.component);
        }
    }
    
    //TODO: Evaluate following methods
    
    // Rename to copy?
    public AnnotatedPropertyEditor<T> deepCopy() {
        AnnotatedPropertyEditor<T> copy;
        try {
            copy = this.getClass().getConstructor().newInstance();
            // If a (sub-class) constructor didn't call
            // SynapseRuleUserParamPanel(SynapseUpdateRule)
            if (copy.editedObject == null) {
                copy.setObjectToEdit(this.editedObject);
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
        // order,
        // unless something very odd is going on with the loaded classes.
        Iterator<ParameterWidget> thisParamsItr = params.iterator();
        Iterator<ParameterWidget> copyParamsItr = copy.params.iterator();
        while (thisParamsItr.hasNext()) {
            ParameterWidget thisPW = thisParamsItr.next();
            ParameterWidget copyPW = copyParamsItr.next();
            assert (thisPW.equals(copyPW));
            copyPW.setWidgetValue(thisPW.getWidgetValue());
        }
        return copy;
    }
    
    public void fillFieldValues(List<T> objectList) {
        Object refObject = objectList.get(0);

        for (ParameterWidget pw : params) {
            // Check to see if the field values are consistent over all given
            // instances.
            boolean consistent = true;
            Object refValue = pw.parameter.getFieldValue(refObject);
            for (int i = 1; i < objectList.size(); i++) {
                Object obj = objectList.get(i);
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
    
    public void fillDefaultValues() {
        for (ParameterWidget pw : params) {
            pw.setWidgetValue(pw.parameter.getDefaultValue());
        }
    }

    /**
     * Commit changes on edited object. 
     */
    public void commit() {
        commitChanges(editedObject);        
    }

    public void commitChanges(Object object) {
        if (!editedObject.getClass()
                .equals(object.getClass())) {
            //TODO
            //object.setLearningRule(toEdit.deepCopy());
        }
        writeValuesToRules(Collections.singletonList(object));
    }

    protected void writeValuesToRules(Collection<Object> objects) {
        for (ParameterWidget pw : params) {
            Object value = pw.getWidgetValue();

            // Ignore unspecified values.
            // If the value isn't null and it's either not a String or not an
            // empty string.
            if (value != null && (!(value instanceof String)
                    || !((String) value).equals(""))) {
                for (Object o : objects) {
                    pw.parameter.setFieldValue(o, value);
                }
            }
        }
        // Re-initialise. Allows updating cached values calculated from
        // parameters.
        for (Object o : objects) {
            //s.getLearningRule().init(s); 
            // TODO!
        }
    }
    
    public void setReplace(boolean replace) {
        // TODO Auto-generated method stub
    }

    /**
     * Returns an action for showing a property dialog for the provided objects.
     *
     * @param object object the object whose properties should be displayed
     * @return the action
     */
    public static AbstractAction getPropertiesDialogAction(final Object object) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.png"));
                putValue(NAME, "Show properties...");
                putValue(SHORT_DESCRIPTION, "Show properties");
            }

            public void actionPerformed(ActionEvent arg0) {
                AnnotatedPropertyEditor editor = new AnnotatedPropertyEditor();
                editor.setObjectToEdit(object);
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
        ret.setContentPane(itemPanel);
        return ret;
    }

    /**
     * Extension of Standard Dialog for Editor Panel
     */
    private class EditorDialog extends StandardDialog {

        @Override
        protected void closeDialogOk() {
            AnnotatedPropertyEditor.this.commit();
            dispose();
        }
    }

    /**
     * @return the toEdit
     */
    public Object getEditedObject() {
        return editedObject;
    }



}
