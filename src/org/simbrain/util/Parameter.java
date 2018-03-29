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
package org.simbrain.util;

import org.simbrain.util.propertyeditor2.ObjectTypeEditor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper for {@link UserParameter} annotations containing various utility methods.
 *
 * @author O. J. Coleman
 */
public class Parameter implements Comparable<Parameter> {

    //TODO: Make these private or protected

    /**
     * TheAnnotation for this Parameter.
     */
    public final UserParameter annotation;

    /**
     * The Field for this Parameter.
     */
    public final Field field;


    /**
     * Construct a parameter object from a field.
     *
     * @param field the field
     */
    public Parameter(Field field) {
        this.field = field;
        annotation = field.getAnnotation(UserParameter.class);
    }

    /**
     * Returns true if this is an annotation for a multi-state object.
     */
    public boolean isMultiState() {
        return annotation.isMultiState();
    }

    /**
     * Uses reflection to get the type map for a multi-state object.
     */
    public BiMap<String, Class>  getTypeMap() {
        if (annotation == null) {
            return null;
        }
        String className = annotation.typeMapClass();
        String methodName = annotation.typeMapMethod();
        try {
            Class c = Class.forName(className);
            Method m = c.getDeclaredMethod(methodName);
            BiMap<String, Class> typeMap = (BiMap<String, Class>) m.invoke(null, null);
            return typeMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns true iff the type of the field is numeric (integer or floating-point).
     */
    public boolean isNumeric() {
        return isNumericFloat() || isNumericInteger();
    }

    public boolean isNumericFloat() {
        return floatTypes.contains(field.getType());
    }

    public boolean isNumericInteger() {
        return integerTypes.contains(field.getType());
    }

    /**
     * Returns true iff the type of the field is boolean.
     */
    public boolean isBoolean() {
        return field.getType().equals(Boolean.TYPE) || field.getType().equals(Boolean.class);
    }

    /**
     * Returns true iff the type of the field is String.
     */
    public boolean isString() {
        return field.getType().equals(String.class);
    }

    /**
     * Returns true iff the UserParameter defines a default value.
     */
    public boolean hasDefaultValue() {
        return !"".equals(annotation.defaultValue());
    }

    /**
     * Returns true iff the UserParameter defines a minimum value.
     */
    public boolean hasMinValue() {
        return !Double.isNaN(annotation.minimumValue());
    }

    /**
     * Returns true iff the UserParameter defines a maximum value.
     */
    public boolean hasMaxValue() {
        return !Double.isNaN(annotation.maximumValue());
    }

    /**
     * Returns the default value for this parameter, or null if none is specified.
     */
    public Object getDefaultValue() {
        if (!annotation.defaultValue().trim().equals("")) {
            try {
                return interpretValue(annotation.defaultValue());
            } catch (Exception e) {
                String message = "The type of parameter field " + field.getDeclaringClass().getSimpleName() + "." + field.getName() + " is " + field.getType().getSimpleName() + " but it looks like the default value specified, '" + annotation.defaultValue() + "', cannot be interpreted as such.";
                System.err.println(message);
            }
        }

        if (isNumeric()) {
            // Respect/use bounds to determine default numeric value.
            double val = 0;
            if (hasMinValue() && hasMaxValue()) {
                val = (annotation.maximumValue() - annotation.minimumValue()) / 2;
            } else {
                if (hasMinValue() && val < annotation.minimumValue())
                    val = annotation.minimumValue();
                else if (hasMaxValue() && val > annotation.maximumValue())
                    val = annotation.maximumValue();
            }

            if (isNumericFloat())
                return val;
            if (isNumericInteger())
                return (int) Math.round(val);
        }

        if (isBoolean())
            return false;

        if (isString())
            return "";

        return null;
    }


    /**
     * Get the value for this parameter on an object instance.
     *
     * @param instance The object containing the parameter field to get the value of.
     * @throws RuntimeException     If a Java reflection API error occurs.
     * @throws NullPointerException If <em>object</em> is null.
     */
    public Object getFieldValue(Object instance) {
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("IllegalAccessException: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("IllegalArgumentException: " + e.getMessage(), e);
        } catch (SecurityException e) {
            throw new RuntimeException("SecurityException: " + e.getMessage(), e);
        }
    }


    /**
     * Set the value for this parameter on an object instance.
     *
     * @param object The object containing the parameter field to set.
     * @param value  The value to assign to the parameter field on <em>object</em>. May not be null.
     * @throws IllegalArgumentException If an error occurs setting the value (for example it doesn't meet a validation criterion).
     * @throws RuntimeException         If a Java reflection API error occurs.
     * @throws NullPointerException     If <em>object</em> or <em>value</em> are null.
     */
    public void setFieldValue(Object object, Object value) {
        value = interpretValue(value);

        String validationError = validateValue(value);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        try {
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong setting the value: " + e.getMessage(), e);
        }
    }


    /**
     * Attempt to convert the given value to the type of this parameter field.
     *
     * @param value The value to convert/interpret.
     * @throws IllegalArgumentException If the given value cannot be converted to the field type.
     * @throws RuntimeException         If a Java reflection API error occurs.
     * @throws NullPointerException     If <em>value</em> is null.
     */
    public Object interpretValue(Object value) {
        // This probably shouldn't happen, but it's better to let the caller
        // deal with it (or throw an exception for debugging purposes).
        if (value == null)
            return null;

        // Get the parameter field type.
        Class<?> fieldType = field.getType();
        Class<?> valueType = value.getClass();

        if (fieldType.isPrimitive()) {
            // Convert to wrapped type so we can compare with given value,
            // and use the constructor from the wrapper to convert from string if necessary.
            fieldType = primitiveWrappers.get(fieldType);
        }

        if (fieldType.isAssignableFrom(valueType))
            return value;

        // Convert the given value to the field type.
        try {
            // Find a constructor taking a single argument of the type given.
            Constructor<?> constructor = fieldType.getConstructor(valueType);
            if (constructor == null) {
                // Otherwise try to find a string constructor and convert given value to String.
                // This works around cases such as being given a Double when a Float is required,
                // and int/float if possible, etc.
                constructor = fieldType.getConstructor(valueType);
                if (constructor == null) {
                    throw new IllegalArgumentException("Value does not match the parameter field type.");
                }
                value = value.toString();
            }

            value = constructor.newInstance(value);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException("Something went wrong while interpreting the given value: " + e.getMessage(), e);
        }

        return value;
    }


    /**
     * Validate the given value.
     *
     * @param value The value to validate. Must be of a type that matches the field type.
     * @return An error message or null if the value is valid.
     * @throws NullPointerException If <em>value</em> is null.
     */
    public String validateValue(Object value) {
        // Convert if necessary (this is mostly a no-op if the value is already of the same type as the field).
        value = interpretValue(value);

        // Validate against regex if applicable.
        if (!annotation.regexValidation().trim().equals("") && value instanceof String) {
            Pattern p = Pattern.compile(annotation.regexValidation().trim());
            Matcher m = p.matcher((String) value);
            if (!m.matches()) {
                return "Value is invalid, it must match the regular expression /" + p.pattern() + "/.";
            }
        }

        // If this is a numeric type, test against the min and max values if specified.
        if (isNumeric()) {
            Number numericValue = (Number) value;
            if (hasMinValue() && numericValue.doubleValue() < annotation.minimumValue()) {
                throw new IllegalArgumentException("Value is lower than minimum allowed.");
            }
            if (hasMaxValue() && numericValue.doubleValue() > annotation.maximumValue()) {
                throw new IllegalArgumentException("Value is greater than maximum allowed.");
            }
        }

        return null;
    }


    // Static cache of annotated fields for a class.
    // Avoids multiple runs of expensive reflection code.
    private static Map<Class<?>, Set<Parameter>> classParameters = new HashMap<>();

    /**
     * Get the available {@link Parameter}s ({@link UserParameter} annotated Fields) defined in the specified class.
     * The available Parameters are statically cached.
     *
     * @return The available Parameters, sorted according to {@link UserParameter#weight()}.
     */
    public static Set<Parameter> getParameters(final Class<?> paramClass) {
        //System.out.println("Parameter.getParameters");
        if (!classParameters.containsKey(paramClass)) {
            Set<Parameter> params = new TreeSet<>();
            Set<String> fieldNames = new HashSet<>();

            // Get all super-classes too so that we can set their fields.
            for (Class<?> clazz : getSuperClasses(paramClass)) {
                //System.out.println("paramClass = [" + paramClass + "]");
                for (Field f : clazz.getDeclaredFields()) {
                    //System.out.println("declaredField = [" + f + "]");
                    if (f.isAnnotationPresent(UserParameter.class)) {
                        if (fieldNames.contains(f.getName())) {
                            // TODO Make a special exception? Probably not, it's only for developers when making update rules etc.
                            throw new RuntimeException("A field with the same name, '" + f.getName() + "', is declared in a super-class of " + paramClass.getName());
                        }

                        fieldNames.add(f.getName());
                        params.add(new Parameter(f));
                    }
                }
            }
            classParameters.put(paramClass, Collections.unmodifiableSet(params));
        }

        return classParameters.get(paramClass);
    }


    /**
     * Gets a list containing this class and all its super-classes up to the parent ConfigurableBase. The list is
     * ordered from super to this class.
     */
    protected static List<Class<?>> getSuperClasses(final Class<?> clazz) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        Class<?> superClass = clazz;
        while (!superClass.equals(Object.class)) {
            classes.add(superClass);
            superClass = classes.get(classes.size() - 1).getSuperclass();
        }

        Collections.reverse(classes);
        return classes;
    }


    /**
     * Mapping from primitive types to the corresponding wrapper types.
     */
    protected static final Map<Class<?>, Class<?>> primitiveWrappers = new HashMap<>();

    static {
        primitiveWrappers.put(Boolean.TYPE, Boolean.class);
        primitiveWrappers.put(Byte.TYPE, Byte.class);
        primitiveWrappers.put(Character.TYPE, Character.class);
        primitiveWrappers.put(Short.TYPE, Short.class);
        primitiveWrappers.put(Integer.TYPE, Integer.class);
        primitiveWrappers.put(Long.TYPE, Long.class);
        primitiveWrappers.put(Double.TYPE, Double.class);
        primitiveWrappers.put(Float.TYPE, Float.class);
        primitiveWrappers.put(Void.TYPE, Void.TYPE);
    }


    /**
     * Set of all floating-point types.
     */
    protected static final Set<Class<?>> floatTypes = new HashSet<>();

    static {
        floatTypes.add(Double.TYPE);
        floatTypes.add(Double.class);
        floatTypes.add(Float.TYPE);
        floatTypes.add(Float.class);
        floatTypes.add(BigDecimal.class);
    }

    /**
     * Set of all integer types.
     */
    protected static final Set<Class<?>> integerTypes = new HashSet<>();

    static {
        integerTypes.add(Byte.TYPE);
        integerTypes.add(Byte.class);
        integerTypes.add(Short.TYPE);
        integerTypes.add(Short.class);
        integerTypes.add(Integer.TYPE);
        integerTypes.add(Integer.class);
        integerTypes.add(Long.class);
        integerTypes.add(Long.class);
    }


    /**
     * Impose ordering by {@link UserParameter#order()} and then field name.
     */
    @Override
    public int compareTo(Parameter other) {
        int result = Integer.compare(this.annotation.order(), other.annotation.order());
        if (result != 0)
            return result;
        return this.field.getName().compareTo(other.field.getName());
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Parameter) {
            return field.equals(((Parameter) other).field);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return field.hashCode();
    }

    @Override
    public String toString() {
        return "Parameter " + annotation.label();
    }
}
