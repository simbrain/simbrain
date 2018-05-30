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

import org.simbrain.util.Parameter;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor2.CopyableObject;
import org.simbrain.util.propertyeditor2.ObjectTypeEditor;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A wrapper class for a {@link Parameter} and an associated GUI widget (JComponent).
 *
 * @author O. J. Coleman
 */
public class ParameterWidget implements Comparable<ParameterWidget> {

    /**
     * The parameter for this widget.
     */
    public final Parameter parameter;

    /**
     * The GUI element for this widget.
     */
    public final JComponent component;

    /**
     * List of edited objects. Used in conjunction with {@link ObjectTypeEditor},
     * which must be initialized with edited objects.
     */
    private List<CopyableObject> editedObjects;

    /**
     * Construct an parameter widget from a parameter, which in turn represents a field.
     *
     * @param param the parameter object
     */
    public ParameterWidget(Parameter param) {
        parameter = param;
        component = makeWidget();
    }

    /**
     * Construct a widget with a parameter object and a list of objects to edit.
     *
     * @param param parameter to wrap
     * @param editedObjects objects to edit
     */
    public ParameterWidget(Parameter param, List<CopyableObject> editedObjects ) {
        parameter = param;
        this.editedObjects = editedObjects;
        component = makeWidget();
    }

    /**
     * Create an appropriate widget for this parameter.
     */
    protected JComponent makeWidget() {

        if (parameter.isObjectType()) {
                return ObjectTypeEditor.createEditor(editedObjects, parameter.getTypeMap(), parameter.annotation.label());
        }

        if (parameter.isBoolean()) {
            return new YesNoNull();
        }

        if (parameter.isNumeric()) {
            Number defaultValue = (Number) parameter.getDefaultValue();

            SpinnerNumberModelWithNull spinnerModel;

            Double minValue = parameter.hasMinValue() ? parameter.annotation.minimumValue() : null;
            Double maxValue = parameter.hasMaxValue() ? parameter.annotation.maximumValue() : null;
            double range;
            double stepSize = 0.1;

            // If there's a min and/or max value then we can base the spinner step size on it.
            if (minValue != null || maxValue != null || defaultValue != null) {
                if (minValue != null && maxValue != null) {
                    range = maxValue.doubleValue() - minValue.doubleValue();
                } else if (minValue != null) {
                    range = Math.abs(minValue.doubleValue());
                } else if (maxValue != null) {
                    range = Math.abs(maxValue.doubleValue());
                } else {
                    range = Math.abs(defaultValue.doubleValue());
                }

                double initialStep = range / 100; // aiming for about 100 steps between min and max.
                double magnitude = Math.pow(10, Math.floor(Math.log10(initialStep)));
                double msd = initialStep / magnitude;
                if (msd > 5) {
                    msd = 10;
                } else if (msd > 2) {
                    msd = 5;
                } else if (msd > 1) {
                    msd = 2;
                }
                stepSize = msd * magnitude;
            }

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
     * Returns tooltip text for this parameter.
     */
    public String getToolTipText() {
        UserParameter anot = parameter.annotation;
        List<String> tips = new ArrayList<>();
        if (!"".equals(anot.description())) {
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
     * Set the value of the widget. If value is null then the "null" state of the
     * widget is displayed.
     */
    public void setWidgetValue(Object value) {
        if (parameter.isBoolean()) {
            if (value == null) {
                ((YesNoNull) component).setNull();
            } else {
                ((YesNoNull) component).setSelected((Boolean) value);
            }
        } else if (parameter.isNumeric()) {
            ((JNumberSpinnerWithNull) component).setValue(value);
        } else if (parameter.annotation.isObjectType()) {
            // No action. ObjectTypeEditor handles its own init
        } else {
            ((JTextField) component).setText(value == null ? "" : value.toString());
        }
    }

    /**
     * Get the value of the widget.
     */
    public Object getWidgetValue() {
        if (parameter.isBoolean()) {
            return ((YesNoNull) component).isNull() ? null : ((YesNoNull) component).isSelected();
        }
        if (parameter.isNumeric()) {
            return ((JNumberSpinnerWithNull) component).getValue();
        }
        if (parameter.annotation.isObjectType()) {
            return ((ObjectTypeEditor)component).getValue();
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

    @Override
    public int hashCode() {
        return parameter.hashCode();
    }

    /**
     * Convenience method to get a label for this parameter.
     *
     * @return the label
     */
    public String getLabel() {
        return parameter.annotation.label();
    }
}
