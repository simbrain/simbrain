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

import org.simbrain.util.BiMap;
import org.simbrain.util.Parameter;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor2.CopyableObject;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.util.propertyeditor2.ObjectTypeEditor;

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
     * List of edited objects. Used in conjunction with {@link
     * ObjectTypeEditor}, which must be initialized with edited objects.
     */
    private List<CopyableObject> editedObjects;

    /**
     * Construct a parameter widget from a parameter, which in turn represents a
     * field.
     *
     * @param param the parameter object
     */
    public ParameterWidget(Parameter param) {
        parameter = param;
        component = makeWidget();
    }

    /**
     * Construct a widget with a parameter object and a list of objects to
     * edit.
     *
     * @param param         parameter to wrap
     * @param editedObjects objects to edit
     */
    public ParameterWidget(Parameter param, List<CopyableObject> editedObjects) {
        parameter = param;
        this.editedObjects = editedObjects;
        component = makeWidget();
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
            return ObjectTypeEditor.createEditor(editedObjects, typeMap, parameter.getAnnotation().label());
        }

        if(!parameter.isEditable()) {
            return new JLabel();
        }

        if (parameter.isBoolean()) {
            return new YesNoNull();
        }

        if (parameter.isColor()) {
            return new ColorSelector();
        }

        if (parameter.isEnum()) {
            try {
                Class<?> clazz = parameter.getType();
                Method method = clazz.getDeclaredMethod("values");
                Object[] enumValues = (Object[]) method.invoke(parameter.getDefaultValue());
                ChoicesWithNull comboBox = new ChoicesWithNull(enumValues);
                return comboBox;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }

        if (parameter.isNumeric()) {
            Number defaultValue = (Number) parameter.getDefaultValue();

            SpinnerNumberModelWithNull spinnerModel;

            Double minValue = parameter.hasMinValue() ? parameter.getAnnotation().minimumValue() : null;
            Double maxValue = parameter.hasMaxValue() ? parameter.getAnnotation().maximumValue() : null;

            // Step size if bounds are missing
            double stepSize = 1;

            // If bounds are given divide the spinner can step numsteps from top to bottom
            if (minValue != null && maxValue != null) {
                int numSteps = 100;
                stepSize = Math.abs(minValue - maxValue) / numSteps;
            }

            // TODO: Evaluate the original code below (which produced some Nans).  The code above works well enough for now
            // double range;
//            // If there's a min and/or max value then we can base the spinner step size on it.
//            if (minValue != null || maxValue != null || defaultValue != null) {
//                if (minValue != null && maxValue != null) {
//                    range = maxValue.doubleValue() - minValue.doubleValue();
//                } else if (minValue != null) {
//                    range = Math.abs(minValue.doubleValue());
//                } else if (maxValue != null) {
//                    range = Math.abs(maxValue.doubleValue());
//                } else {
//                    range = Math.abs(defaultValue.doubleValue());
//                }
//
//                double initialStep = range / 100; // aiming for about 100 steps between min and max.
//                double magnitude = Math.pow(10, Math.floor(Math.log10(initialStep)));
//                double msd = initialStep / magnitude;
//                if (msd > 5) {
//                    msd = 10;
//                } else if (msd > 2) {
//                    msd = 5;
//                } else if (msd > 1) {
//                    msd = 2;
//                }
//                stepSize = msd * magnitude;
//            }

            if (parameter.isNumericInteger()) {
                int step = (int) Math.max(1, stepSize);
                spinnerModel = new SpinnerNumberModelWithNull((Integer) defaultValue, minValue == null ? null : minValue.intValue(), maxValue == null ? null : maxValue.intValue(), step);
            } else {
                spinnerModel = new SpinnerNumberModelWithNull((Double) defaultValue, minValue, maxValue, stepSize);
            }


            return new JNumberSpinnerWithNull(spinnerModel);
        }

        return new JTextField();
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
        if(anot.description().isEmpty()) {
            tips.add(anot.label());
        } else {
            tips.add(anot.description());
        }
        if (parameter.hasDefaultValue()) {
            tips.add("Default: " + parameter.getDefaultValue());
        }
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
        if(!parameter.isEditable()) {

            ((JLabel) component).setText(value == null ? SimbrainConstants.NULL_STRING : value.toString());

        } else if (parameter.isBoolean()) {
            if (value == null) {
                ((YesNoNull) component).setNull();
            } else {
                ((YesNoNull) component).setSelected((Boolean) value);
            }
        } else if (parameter.isColor()) {
            ((ColorSelector) component).setValue((Color) value);
        } else if (parameter.isNumeric()) {
            ((JNumberSpinnerWithNull) component).setValue(value);
        } else if (parameter.getAnnotation().isObjectType()) {
            // No action. ObjectTypeEditor handles its own init
        } else if (parameter.isEnum()) {
            if(value == null) {
                ((ChoicesWithNull) component).setNull();
            } else {
                ((ChoicesWithNull) component).setSelectedItem(value);
            }
        } else {
            ((JTextField) component).setText(value == null ? SimbrainConstants.NULL_STRING : value.toString());
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

        if (parameter.isNumeric()) {
            return ((JNumberSpinnerWithNull) component).getValue();
        }
        if (parameter.getAnnotation().isObjectType()) {
            return ((ObjectTypeEditor) component).getValue();
        }
        if (parameter.isEnum()) {
            return ((ChoicesWithNull) component).isNull() ? null : ((ChoicesWithNull) component).getSelectedItem();
        }

        return ((JTextField) component).getText();
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
            Object val = ((JNumberSpinnerWithNull) component).getValue();
            if (val == null) {
                return true;
            }
        }
        if (parameter.isString()) {
            if (((JTextField) component).getText().equalsIgnoreCase(SimbrainConstants.NULL_STRING)) {
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
}
