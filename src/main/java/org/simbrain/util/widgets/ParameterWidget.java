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
import org.simbrain.util.propertyeditor.*;

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
     * Parent property editor.
     */
    private AnnotatedPropertyEditor parent;

    /**
     * If true, then a custom value has been used to initialize the widget.
     */
    public boolean customInitialValue = false;

    /**
     * Construct a parameter widget from a parameter, which in turn represents a
     * field.s
     */
    public ParameterWidget(AnnotatedPropertyEditor ape, Parameter parameter) {
        parent = ape;
        this.parameter = parameter;
        component = makeWidget();
        checkConditionalVisibility();
        checkConditionalEnabling();
        setInitialValue();
    }

    /**
     * Check if this component should be visible or not, based on
     * {@link UserParameter#conditionalVisibilityMethod()}.
     */
    public void checkConditionalVisibility() {

        String conditionalVisibilityMethod = parameter.getAnnotation().conditionalVisibilityMethod();
        if (!conditionalVisibilityMethod.isEmpty()) {
            try {
                Method method = parent.getEditedObjects().get(0).getClass().
                        getMethod(conditionalVisibilityMethod);
                Boolean visible = (Boolean) method.invoke(parent.getEditedObjects().get(0));
                component.setVisible(visible);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Check if this component should be enabled or not, based on
     * {@link UserParameter#conditionalEnablingMethod()} ()}.
     */
    public void checkConditionalEnabling() {

        String conditionalEnablingMethod = parameter.getAnnotation().conditionalEnablingMethod();
        if (!conditionalEnablingMethod.isEmpty()) {
            try {
                Method method = parent.getEditedObjects().get(0).getClass().
                        getMethod(conditionalEnablingMethod);
                Boolean enabled = (Boolean) method.invoke(parent.getEditedObjects().get(0));
                component.setEnabled(enabled);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check conditional enabling based on another widget, specified in terms of its
     * {@link UserParameter#description()}. If the other widget is a drop down box then if it is set to "yes"
     * this widget is enabled, else it is disabled.
     */
    public void checkConditionalEnablingWidget() {
        String otherWidgetName = parameter.getAnnotation().condtionalEnablingWidget();
        if (!otherWidgetName.isEmpty()) {
            ParameterWidget otherWidget = parent.getWidget(otherWidgetName);
            if (otherWidget != null) {
                if (otherWidget.component instanceof YesNoNull) {
                    component.setEnabled(((YesNoNull) otherWidget.component).isSelected());
                }
            }
        }
    }

    /**
     * Set the initial value of this widget if the {@link UserParameter#initialValueMethod()}
     * is set.
     */
    private void setInitialValue() {
        String initialValueMethod = parameter.getAnnotation().initialValueMethod();
        if (!initialValueMethod.isEmpty()) {
            // TODO: type check, or generalize to any object type
            try {
                Method method = parent.getEditedObjects().get(0).getClass().
                        getDeclaredMethod(initialValueMethod);
                String initValue = (String) method.invoke(parent.getEditedObjects().get(0));
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
            // Assumes the type list is contained in the class corresponding to the parameter.
            // Example: NeuronUpdateRule maintains a list of types  of neuronupdaterules.
            String methodName = parameter.getAnnotation().typeListMethod();
            BiMap<String, Class> typeMap = getTypeMap(parameter.getType(), methodName);
            return ObjectTypeEditor.createEditor((List<CopyableObject>) getEditableObjects(), typeMap,
                    parameter.getAnnotation().label(), parameter.getAnnotation().showDetails());
        }

        // Embedded objects are converted into separate property editors
        if (parameter.isEmbeddedObject()) {
            var panel = new JPanel();
            panel.setBorder(BorderFactory.createLineBorder(Color.black));
            // TODO: Add detail triangle
            return new AnnotatedPropertyEditor(getEditableObjects());
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
            return new DoubleArrayWidget();
        }

        if (parameter.isIntArray()) {
            return new IntArrayWidget();
        }

        if (parameter.isEnum()) {
            try {
                Class<?> clazz = parameter.getType();
                Method method = clazz.getDeclaredMethod("values");
                // TODO: Not sure the null argument is correct below.
                Object[] enumValues = (Object[]) method.invoke(null);
                return new ChoicesWithNull(enumValues);
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
                spinnerModel = new SpinnerNumberModelWithNull(0, minValue == null ? null : minValue.intValue(), maxValue == null ? null : maxValue.intValue(), step);
            } else {
                spinnerModel = new SpinnerNumberModelWithNull(0.0, minValue, maxValue, stepSize);
            }

            Runnable setNull = () -> setWidgetValue(null);

            return new NumericWidget(parent.getEditedObjects(), parameter, spinnerModel, setNull);
        }

        if (parameter.isString()) {
            return new TextWithNull();
        }

        throw new IllegalArgumentException("You have annotated a field type (" +
                parameter.getType().getCanonicalName() + ") that is not yet supported");
    }

    /**
     * Takes a class and method name and returns a type map.
     */
    public static BiMap<String, Class> getTypeMap(Class c, String methodName) {
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
        } else if (parameter.isIntArray()) {
            ((IntArrayWidget) component).setValues((int[]) value);
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
        } else if (parameter.isString()) {
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
            throw new IllegalArgumentException("Trying to edit a non-editable object");
        } else if (parameter.isString()) {
            return ((TextWithNull) component).isNull() ? null : ((TextWithNull) component).getText();
        } else if (parameter.isNumeric()) {
            return ((NumericWidget) component).getValue();
        } else if (parameter.isBoolean()) {
            return ((YesNoNull) component).isNull() ? null : ((YesNoNull) component).isSelected();
        } else if (parameter.isColor()) {
            return ((ColorSelector) component).getValue();
        } else if (parameter.isDoubleArray()) {
            return ((DoubleArrayWidget) component).getValues();
        } else if (parameter.isIntArray()) {
            return ((IntArrayWidget) component).getValues();
        } else if (parameter.getAnnotation().isObjectType()) {
            return ((ObjectTypeEditor) component).getValue();
        } else if (parameter.isEnum()) {
            return ((ChoicesWithNull) component).isNull() ? null : ((ChoicesWithNull) component).getSelectedItem();
        } else {
            throw new IllegalArgumentException("Trying to retrieve a value from an unsupported widget type");
        }
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
        if (parameter.isEnum()) {
            if (((ChoicesWithNull) component).isNull()) {
                return true;
            }
        }

        return false;
    }

    public boolean isCustomInitialValue() {
        return customInitialValue;
    }


    /**
     * Return a list of objects associated with this field. Example: a list of neuronupdaterules associated with the
     * updaterule field, one object for each neuron in the list of edited objects.
     */
    private List<? extends EditableObject> getEditableObjects() {
        return  parent.getEditedObjects().stream()
                .map(o -> (EditableObject) parameter.getFieldValue(o))
                .collect(Collectors.toList());
    }

}
