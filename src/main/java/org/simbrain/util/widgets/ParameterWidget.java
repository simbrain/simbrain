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
package org.simbrain.util.widgets;

import org.simbrain.util.*;
import org.simbrain.util.propertyeditor.CopyableObject;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.util.propertyeditor.NumericWidget;
import org.simbrain.util.propertyeditor.ObjectTypeEditor;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A wrapper class for a {@link Parameter} and an associated GUI widget
 * (JComponent).
 *
 * @author O. J. Coleman
 */
public class ParameterWidget implements Comparable<ParameterWidget> {

    /**
     * The parameter for this widget.
     */
    private final Parameter parameter;

    /**
     * The GUI element for this widget.
     */
    private final JComponent component;

    /**
     * The list of objects this parameter is editing.
     */
    private List<? extends EditableObject> editableObjects;

    /**
     * List of edited objects. Only used in conjunction with {@link
     * ObjectTypeEditor}, which must be initialized with edited objects.
     */
    private List<CopyableObject> objectTypeList;

    /**
     * If true, then a custom value has been used to initialize the widget.
     */
    public boolean customInitialValue = false;

    /**
     * Construct a parameter widget from a parameter, which in turn represents a
     * field.
     * @param parameter the parameter object
     * @param editableObjects objects to edit
     */
    public ParameterWidget(Parameter parameter, List<? extends EditableObject> editableObjects) {
        this.parameter = parameter;
        this.editableObjects = editableObjects;
        if (parameter.isObjectType()) {
            // ObjectTypeEditors require special initialization

            // Create a list of objects corresponding to the field associated with the parameter
            // E.g. a list of neuronupdaterules objects within a list of neuron objects
            objectTypeList = new ArrayList<>();
            for (Object o : editableObjects) {
                objectTypeList.add((CopyableObject) parameter.getFieldValue(o));
            }
        }
        component = makeWidget();
        checkConditionalEnabling();
        setInitialValue();
    }

    /**
     * If this component should be disabled (based on the {@link UserParameter#conditionalEnablingMethod()}
     * annotation, disable it.
     */
    private void checkConditionalEnabling() {
        String conditionalEnableMethod = parameter.getAnnotation().conditionalEnablingMethod();
        if (!conditionalEnableMethod.isEmpty()) {
            try {
                Method method = editableObjects.get(0).getClass().
                        getDeclaredMethod(conditionalEnableMethod);
                Boolean enabled  = (Boolean) method.invoke(editableObjects.get(0));
                component.setEnabled(enabled);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }    }

    /**
     * Set the initial value of this widget if the {@link UserParameter#initialValueMethod()}
     * is set.
     */
    private void setInitialValue() {
        String initialValueMethod = parameter.getAnnotation().initialValueMethod();
        if (!initialValueMethod.isEmpty()) {
            // TODO: type check, or generalize to any object type
            try {
                Method method = editableObjects.get(0).getClass().
                        getDeclaredMethod(initialValueMethod);
                String initValue = (String) method.invoke(editableObjects.get(0));
                ((TextWithNull) component).setText(initValue);
                customInitialValue = true;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Create an appropriate widget for this parameter.
     */
    protected JComponent makeWidget() {

        if (parameter.isObjectType()) {
            // Assumes the type list is contained in
            // the class corresponding to the type.  E.g. NeuronUpdateRule maintains a list of types  of neuronupdaterules, and
            // ProbabilityDistribution maintains a list of types of prob distributions
            String methodName = parameter.getAnnotation().typeListMethod();
            BiMap<String, Class> typeMap = getTypeMap(parameter.getType(), methodName);
            return ObjectTypeEditor.createEditor(objectTypeList, typeMap,
                parameter.getAnnotation().label(), parameter.getAnnotation().showDetails());
        }

        if (!parameter.isEditable()) {
            return new JLabel();
        }

        if (parameter.isBoolean()) {
            return new YesNoNull();
        }

        if (parameter.isColor()) {
            return new ColorSelector();
        }

        if (parameter.isDoubleArray()) {
            DoubleArrayWidget doubleArrayWidget = new DoubleArrayWidget();
            return new DoubleArrayWidget();
        }

        if (parameter.isEnum()) {
            try {
                Class<?> clazz = parameter.getType();
                Method method = clazz.getDeclaredMethod("values");
                // TODO: Not sure the null argument is correct below.
                Object[] enumValues = (Object[]) method.invoke(null);
                ChoicesWithNull comboBox = new ChoicesWithNull(enumValues);
                return comboBox;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }

        if (parameter.isNumeric()) {

            // For when preference values are implemented later
            // if(!parameter.getAnnotation().preferenceKey().isEmpty()) {
            //     System.out.println(SimbrainPreferences.getDouble(parameter.getAnnotation().preferenceKey()));
            // }

            SpinnerNumberModelWithNull spinnerModel;

            Double minValue = parameter.hasMinValue() ? parameter.getAnnotation().minimumValue() : null;
            Double maxValue = parameter.hasMaxValue() ? parameter.getAnnotation().maximumValue() : null;

            // Step size if bounds are missing
            double stepSize = parameter.getAnnotation().increment();

            // If desired, an "auto-increment" field could be added to userparameter.
            // Code for this is in revision 564596

            if (parameter.isNumericInteger()) {
                int step = (int) Math.max(1, stepSize);
                spinnerModel = new SpinnerNumberModelWithNull((Integer) 0, minValue == null ? null : minValue.intValue(), maxValue == null ? null : maxValue.intValue(), step);
            } else {
                spinnerModel = new SpinnerNumberModelWithNull((Double) 0.0, minValue, maxValue, stepSize);
            }

            Runnable setNull = () -> setWidgetValue(null);

            NumericWidget ret = new NumericWidget(editableObjects, parameter, spinnerModel, setNull);
            return ret;
        }

        if (parameter.isString()) {
            return new TextWithNull();
        }

        throw new IllegalArgumentException("You have annotated a field type that is not yet supported");
    }

    /**
     * Takes a class and method name and returns a type map.
     */
    private BiMap<String, Class> getTypeMap(Class c, String methodName) {
        BiMap<String, Class> typeMap = new BiMap<>();
        try {
            Method m = c.getDeclaredMethod(methodName);
            List<Class> types = (List<Class>) m.invoke(null);
            for (Class type : types) {
                try {
                    EditableObject inst = (EditableObject) type.getDeclaredConstructor().newInstance();
                    typeMap.put(inst.getName(), type);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            return typeMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns tooltip text for this parameter.
     */
    public String getToolTipText() {
        UserParameter anot = parameter.getAnnotation();
        List<String> tips = new ArrayList<>();
        if (anot.description().isEmpty()) {
            tips.add(anot.label());
        } else {
            tips.add(anot.description());
        }
        // if (parameter.hasDefaultValue()) {
        //     tips.add("Default: " + parameter.getDefaultValue());
        // }
        if (parameter.hasMinValue()) {
            tips.add("Minimum: " + (parameter.isNumericInteger() ? "" + ((int) anot.minimumValue()) : "" + anot.minimumValue()));
        }
        if (parameter.hasMaxValue()) {
            tips.add("Maximum: " + (parameter.isNumericInteger() ? "" + ((int) anot.maximumValue()) : "" + anot.maximumValue()));
        }
        if (tips.isEmpty()) {
            return "";
        }
        return "<html>" + tips.stream().collect(Collectors.joining("<br/>")) + "</html>";
    }

    /**
     * Set the value of the widget. If value is null then the "null" state of
     * the widget is displayed.
     */
    public void setWidgetValue(Object value) {
        if (!parameter.isEditable()) {

            ((JLabel) component).setText(value == null ? SimbrainConstants.NULL_STRING : value.toString());

        } else if (parameter.isBoolean()) {
            if (value == null) {
                ((YesNoNull) component).setNull();
            } else {
                ((YesNoNull) component).setSelected((Boolean) value);
            }
        } else if (parameter.isColor()) {
            ((ColorSelector) component).setValue((Color) value);
        } else if (parameter.isDoubleArray()) {
            ((DoubleArrayWidget) component).setValues((double[]) value);
        } else if (parameter.isNumeric()) {
            ((NumericWidget) component).setValue(value);
        } else if (parameter.getAnnotation().isObjectType()) {
            // No action. ObjectTypeEditor handles its own init
        } else if (parameter.isEnum()) {
            if (value == null) {
                ((ChoicesWithNull) component).setNull();
            } else {
                ((ChoicesWithNull) component).setSelectedItem(value);
            }
        } else if (parameter.isString()){
            if (value == null) {
                ((TextWithNull) component).setNull();
            } else {
                ((TextWithNull) component).setText(value.toString());
            }
        }
    }

    /**
     * Get the value of the widget.
     */
    public Object getWidgetValue() {

        if (!parameter.isEditable()) {
            // Should not happen
            return null;
        }

        if (parameter.isBoolean()) {
            return ((YesNoNull) component).isNull() ? null : ((YesNoNull) component).isSelected();
        }

        if (parameter.isColor()) {
            return ((ColorSelector) component).getValue();
        }

        if (parameter.isDoubleArray()) {
            return ((DoubleArrayWidget) component).getValues();
        }

        if (parameter.isNumeric()) {
            return ((NumericWidget) component).getValue();
        }
        if (parameter.getAnnotation().isObjectType()) {
            return ((ObjectTypeEditor) component).getValue();
        }
        if (parameter.isEnum()) {
            return ((ChoicesWithNull) component).isNull() ? null : ((ChoicesWithNull) component).getSelectedItem();
        }
        if (parameter.isString()) {
            return ((TextWithNull) component).isNull() ? null : ((TextWithNull) component).getText();
        }

        throw new IllegalArgumentException("Trying to retrieve a value from an unsupported widget type");

    }


    /**
     * Impose ordering by {@link UserParameter#order()} and then field name.
     */
    @Override
    public int compareTo(ParameterWidget other) {
        return this.parameter.compareTo(other.parameter);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ParameterWidget) {
            return parameter.equals(((ParameterWidget) other).parameter);
        }
        return false;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public JComponent getComponent() {
        return component;
    }

    @Override
    public int hashCode() {
        return parameter.hashCode();
    }

    /**
     * Convenience method to get the label for this parameter.
     *
     * @return the label
     */
    public String getLabel() {
        return parameter.getAnnotation().label();
    }

    /**
     * If true the widget is representing fields with different values.
     * Generally some version of "..." is displayed.
     */
    public boolean isInconsistent() {
        if (parameter.isBoolean()) {
            return ((YesNoNull) component).isNull();
        }
        if (parameter.isNumeric()) {
            Object val = ((NumericWidget) component).getValue();
            if (val == null) {
                return true;
            }
        }
        if (parameter.isString()) {
            if (((TextWithNull) component).isNull()) {
                return true;
            }
        }
        if (parameter.getAnnotation().isObjectType()) {
            if (((ObjectTypeEditor) component).isInconsistent()) {
                return true;
            }
        }

        return false;
    }

    public boolean isCustomInitialValue() {
        return customInitialValue;
    }
}
