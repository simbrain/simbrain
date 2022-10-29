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

import org.simbrain.util.UserParameter
import java.awt.Color
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.math.BigDecimal
import java.util.*
import java.util.regex.Pattern

/**
 * Wrapper for [UserParameter] annotations containing various utility
 * methods.
 *
 * @author O. J. Coleman
 * @author Jeff Yoshimi
 */
class Parameter : Comparable<Parameter> {

    val annotation: UserParameter

    /**
     * The Field for this Parameter, when  [UserParameter] annotates a field
     */
    private var theField: Field? = null

    /**
     * The Getter for this Parameter, when [UserParameter] annotates a
     * method. The getter is what should be annotated.
     */
    private var getter: Method? = null

    /**
     * The Setter for this Parameter, when [UserParameter] annotates a
     * method OR when [UserParameter.useSetter] is set to true;
     *
     * NOTE: The setter should not be annotated, but should be inferred
     * using the standard naming conventions. E.g. if `double
     * neuron.getActivation()` is annotated then Parameter assumes that
     * the `neuron.setActivation(double)` exists as well.
     */
    private var setter: Method? = null

    /**
     * Construct a parameter object from a field.
     *
     * @param field the field
     */
    constructor(field: Field) {
        this.theField = field
        annotation = field.getAnnotation(UserParameter::class.java)
        if (annotation.useSetter) {
            val cappedName = field.name.substring(0, 1).uppercase(Locale.getDefault()) + field.name.substring(1)
            val setterName = "set$cappedName"
            setter = try {
                field.declaringClass.getDeclaredMethod(setterName, field.type)
            } catch (e: NoSuchMethodException) {
                throw RuntimeException("Field " + field.name + " does not have a setter named " + setterName)
            }
        }
    }

    /**
     * Construct a parameter object from a method (a getter).
     *
     * @param getter the method
     */
    constructor(getter: Method) {
        this.getter = getter
        annotation = getter.getAnnotation(UserParameter::class.java)

        // No setter needed if annotation is not editable
        if (!annotation.editable) {
            return
        }
        var setterName = ""
        setterName = if (getter.name.startsWith("is")) {
            "set" + getter.name.substring(2)
        } else if (getter.name.startsWith("get")) {
            "set" + getter.name.substring(3)
        } else {
            throw RuntimeException("The getter must begin with 'is' or 'get'. " + getter.name + " is what was provided.")
        }

        // Assume setter is named as one would expect given the getter, with a "set" in place of a "get" or "is"
        val retType = getter.returnType
        setter = try {
            getter.declaringClass.getDeclaredMethod(setterName, retType)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(
                "Class " + getter.name + " has a getter (" + getter.name + "), but no " +
                        "corresponding setter (" + setterName + ")"
            )
        }
    }

    /**
     * Returns the type of the object represented by this parameter. For a field
     * annotation it's the field's type. For a method annotation it's the type of the
     * object returned by the setter (and passed in to the setter).
     *
     * @return the type of the represented objet
     */
    val type: Class<*>
        get() = if (isFieldAnnotation) {
            theField!!.type
        } else {
            getter!!.returnType
        }

    val isEditable: Boolean
        get() = annotation.editable

    /**
     * If this parameter type does not return a value (as with embedded objects) return false
     */
    val hasValue: Boolean
        get() = !isEmbeddedObject

    /**
     * Returns true if this annotation is field based, false otherwise
     */
    val isFieldAnnotation
        get() = theField?.javaClass != null

    /**
     * Abstract over methods and fields.
     */
    private val name
        get() = if (isFieldAnnotation) theField!!.name else getter!!.name

    /**
     * Returns true if this is an annotation for an object type field to be
     * edited by an [org.simbrain.util.propertyeditor.ObjectTypeEditor].
     */
    val isObjectType: Boolean
        get() = annotation.isObjectType

    /**
     * Returns true if this is an annotation for an embedded object with its own annotations.
     */
    val isEmbeddedObject: Boolean
        get() = annotation.isEmbeddedObject

    /**
     * Returns true iff the type of the field is numeric (integer or
     * floating-point).
     */
    val isNumeric: Boolean
        get() = isNumericFloat || isNumericInteger
    val isNumericFloat: Boolean
        get() = floatTypes.contains(type)
    val isNumericInteger: Boolean
        get() = integerTypes.contains(type)

    /**
     * Returns true if the type of field or method is an enum
     */
    val isEnum: Boolean
        get() = type.isEnum ?: false

    /**
     * Returns true if the type of the field or method is boolean.
     */
    val isBoolean: Boolean
        get() = type == java.lang.Boolean.TYPE || type == Boolean::class.java

    /**
     * Returns true if the type of the field or method is Color.
     */
    val isColor: Boolean
        get() = type == Color::class.java

    /**
     * Returns true if the type of the field or method is double[].
     */
    val isDoubleArray: Boolean
        get() = type == DoubleArray::class.java

    /**
     * Returns true if the type of the field or method is double[].
     */
    val isIntArray: Boolean
        get() = type == IntArray::class.java

    /**
     * Returns true iff the type of the field or method is String.
     */
    val isString: Boolean
        get() = type == String::class.java

    /**
     * Returns true iff the UserParameter defines a minimum value.
     */
    fun hasMinValue(): Boolean {
        return !java.lang.Double.isNaN(annotation.minimumValue)
    }

    /**
     * Returns true iff the UserParameter defines a maximum value.
     */
    fun hasMaxValue(): Boolean {
        return !java.lang.Double.isNaN(annotation.maximumValue)
    }

    /**
     * Get the value for this parameter on an object instance.
     *
     * @param instance The object containing the parameter field to get the
     * value of.
     * @throws RuntimeException     If a Java reflection API error occurs.
     * @throws NullPointerException If *object* is null.
     */
    fun getFieldValue(instance: Any?): Any? {
        setAccessible(true)
        return if (isFieldAnnotation) {
            theField!![instance]
        } else {
            getter!!.invoke(instance)
        }
    }

    /**
     * Set the value for this parameter on an object instance.
     *
     * @param object The object containing the parameter field to set.
     * @param value  The value to assign to the parameter field on
     * *object*. May not be null.
     * @throws IllegalArgumentException If an error occurs setting the value
     * (for example it doesn't meet a
     * validation criterion).
     * @throws RuntimeException         If a Java reflection API error occurs.
     * @throws NullPointerException     If *object* or *value* are
     * null.
     */
    fun setFieldValue(theObject: Any, initVal: Any) {
        // println("object = ${theObject?.javaClass?.simpleName}, field = ${theField?.name}, value = $initVal")
        val value = interpretValue(initVal)
        validateValue(value)
        setAccessible(true)
        if (annotation.useSetter) {
            setter!!.invoke(theObject, value)
        } else if (isFieldAnnotation) {
            theField!![theObject] = value
        } else {
            setter!!.invoke(theObject, value)
        }
    }

    /**
     * Attempt to convert the given value to the type of this parameter field.
     *
     * @param theVal The value to convert/interpret.
     * @throws IllegalArgumentException If the given value cannot be converted
     * to the field type.
     * @throws RuntimeException         If a Java reflection API error occurs.
     * @throws NullPointerException     If *value* is null.
     */
    fun interpretValue(theVal: Any): Any {

        var retVal = theVal
        // Get the parameter type.
        var paramType = type
        val valueType: Class<*> = retVal.javaClass
        if (paramType.isPrimitive) {
            return theVal
        }
        if (paramType.isAssignableFrom(valueType)) {
            return theVal
        }

        // Convert the given value to the field type.
        // Find a constructor taking a single argument of the type given.
        var constructor = paramType.getConstructor(valueType)
        retVal = constructor.newInstance(theVal)

        return retVal
    }

    /**
     * Validate the given value.
     *
     * @param value The value to validate. Must be of a type that matches the
     * field type.
     * @return An error message or null if the value is valid.
     * @throws NullPointerException If *value* is null.
     */
    private fun validateValue(initVal: Any?): String? {
        // Convert if necessary (this is mostly a no-op if the value is already of the same type as the field).
        val value = interpretValue(initVal!!)

        // Validate against regex if applicable.
        if (annotation.regexValidation.trim { it <= ' ' } != "" && value is String) {
            val p = Pattern.compile(annotation.regexValidation.trim { it <= ' ' })
            val m = p.matcher(value as String?)
            if (!m.matches()) {
                return "Value is invalid, it must match the regular expression /" + p.pattern() + "/."
            }
        }

        // If this is a numeric type, test against the min and max values if specified.
        if (isNumeric) {
            val numericValue = value as Number?
            require(!(hasMinValue() && numericValue!!.toDouble() < annotation.minimumValue)) { "Value is lower than minimum allowed." }
            require(!(hasMaxValue() && numericValue!!.toDouble() > annotation.maximumValue)) { "Value is greater than maximum allowed." }
        }
        return null
    }

    /**
     * Impose ordering by [UserParameter.order] and then field name.
     */
    override fun compareTo(other: Parameter): Int {
        val result = Integer.compare(annotation.order, other.annotation.order)
        return if (result != 0) {
            result
        } else name.compareTo(other.name)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Parameter) {
            if (other.isFieldAnnotation) {
                theField == other.theField
            } else {
                getter == other.getter
            }
        } else false
    }

    /**
     * Make sure we can access fields and private getters.
     */
    private fun setAccessible(newVal: Boolean) {
        if (isFieldAnnotation) {
            theField!!.isAccessible = newVal
        } else {
            getter!!.isAccessible = newVal
        }
    }

    override fun hashCode(): Int {
        return if (isFieldAnnotation) {
            theField.hashCode()
        } else {
            getter.hashCode()
        }
    }

    override fun toString(): String {
        return "Parameter " + annotation.label
    }

    companion object {
        /**
         * Static cache of annotated fields for a class.
         * Avoids multiple runs of expensive reflection code.
         */
        private val classParameters: MutableMap<Class<*>, Set<Parameter>> = HashMap()

        /**
         * Get the available [Parameter]s ([UserParameter] annotated
         * Fields) defined in the specified class. The available Parameters are
         * statically cached.
         *
         * @return The available Parameters, sorted according to [ ][UserParameter.order]()}.
         */
        fun getParameters(paramClass: Class<*>): Set<Parameter> {
            if (!classParameters.containsKey(paramClass)) {
                val params: MutableSet<Parameter> = TreeSet()
                val fieldAndMethodNames: MutableSet<String> = HashSet()

                // Get all parent classes, which may also contain annotations
                for (clazz in getParentClasses(paramClass)) {
                    // System.out.println("paramClass = [" + paramClass + "]");
                    for (f in clazz!!.declaredFields) {
                        //System.out.println("declaredField = [" + f + "]");
                        if (f.isAnnotationPresent(UserParameter::class.java)) {
                            if (fieldAndMethodNames.contains(f.name)) {
                                // TODO Make a special exception? Probably not, it's only for developers when making update rules etc.
                                throw RuntimeException("A field with the same name, '" + f.name + "', is declared in a super-class of " + paramClass.name)
                            }
                            fieldAndMethodNames.add(f.name)
                            params.add(Parameter(f))
                        }
                    }
                    for (m in clazz.methods) {
                        if (m.isAnnotationPresent(UserParameter::class.java)) {
                            if (fieldAndMethodNames.contains(m.name)) {
                                throw RuntimeException("A method with the same name, '" + m.name + "', is declared in a super-class of " + paramClass.name)
                            }
                            fieldAndMethodNames.add(m.name)
                            params.add(Parameter(m))
                        }
                    }
                }
                classParameters[paramClass] = Collections.unmodifiableSet(params)
            }
            return classParameters[paramClass]!!
        }

        /**
         * Gets a list containing this class and all its superclasses and interfaecs up to the
         * parent ConfigurableBase. The list is ordered from super to this class.
         */
        protected fun getParentClasses(clazz: Class<*>): List<Class<*>?> {
            val classes: MutableList<Class<*>?> = ArrayList()
            var parentClass = clazz
            // Add superclasses
            while (parentClass != Any::class.java) {
                classes.add(parentClass)
                parentClass = classes[classes.size - 1]!!.superclass
            }
            // Add interfaces
            classes.addAll(Arrays.asList(*clazz.interfaces))
            classes.reverse()
            return classes
        }

        /**
         * Set of all floating-point types.
         */
        protected val floatTypes: MutableSet<Class<*>> = HashSet()

        init {
            floatTypes.add(java.lang.Double.TYPE)
            floatTypes.add(Double::class.java)
            floatTypes.add(java.lang.Float.TYPE)
            floatTypes.add(Float::class.java)
            floatTypes.add(BigDecimal::class.java)
        }

        /**
         * Set of all integer types.
         */
        protected val integerTypes: MutableSet<Class<*>> = HashSet()

        init {
            integerTypes.add(java.lang.Byte.TYPE)
            integerTypes.add(Byte::class.java)
            integerTypes.add(java.lang.Short.TYPE)
            integerTypes.add(Short::class.java)
            integerTypes.add(Integer.TYPE)
            integerTypes.add(Int::class.java)
            integerTypes.add(java.lang.Long.TYPE)
            integerTypes.add(Long::class.java)
        }
    }
}