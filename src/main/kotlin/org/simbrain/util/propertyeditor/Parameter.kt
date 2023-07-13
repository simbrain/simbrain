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
import smile.math.matrix.Matrix
import java.awt.Color
import java.math.BigDecimal
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

/**
 * Wrapper for [UserParameter] annotations containing various utility
 * methods.
 *
 * @author O. J. Coleman
 * @author Jeff Yoshimi
 */
class Parameter(property: KProperty1<*, *>) : Comparable<Parameter> {

    val annotation: UserParameter

    private var property: KProperty1<*, *>? = property

    /**
     * Returns the type of the object represented by this parameter. For a field
     * annotation it's the field's type. For a method annotation it's the type of the
     * object returned by the setter (and passed in to the setter).
     *
     * @return the type of the represented objet
     */
    val type: KType
        get() = property!!.returnType

    val isEditable: Boolean
        get() = annotation.editable

    /**
     * If this parameter type does not return a value (as with embedded objects) return false
     */
    val hasValue: Boolean
        get() = !isEmbeddedObject

    /**
     * Abstract over methods and fields.
     */
    val name
        get() = property!!.name

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
        get() = floatTypes.contains(type.jvmErasure)
    val isNumericInteger: Boolean
        get() = integerTypes.contains(type.jvmErasure)

    /**
     * Returns true if the type of field or method is an enum
     */
    val isEnum: Boolean
        get() = type.jvmErasure.isSubclassOf(Enum::class)

    /**
     * Returns true if the type of the field or method is boolean.
     */
    val isBoolean: Boolean
        get() = type.jvmErasure == Boolean::class

    /**
     * Returns true if the type of the field or method is Color.
     */
    val isColor: Boolean
        get() = type.jvmErasure == Color::class

    /**
     * Returns true if the type of the field or method is double[].
     */
    val isDoubleArray: Boolean
        get() = type.jvmErasure == DoubleArray::class

    /**
     * Returns true if the type of the field or method is double[].
     */
    val isIntArray: Boolean
        get() = type.jvmErasure == IntArray::class

    /**
     * Returns true if the type of the field or method is double[].
     */
    val isMatrix: Boolean
        get() = type.jvmErasure == Matrix::class

    /**
     * Returns true iff the type of the field or method is String.
     */
    val isString: Boolean
        get() = type.jvmErasure == String::class

    val useSetter: Boolean
        get() = annotation.useSetter

    val refreshSource: String
        get() = annotation.refreshSource

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
        return try {
            val isAccessible = property!!.isAccessible
            property!!.isAccessible = true
            val result = property!!.getter.call(instance)
            property!!.isAccessible = isAccessible
            result
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            null
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
        validateValue(initVal)
        property?.let {
            fun setKotlinProperty() {
                if (it is KMutableProperty<*>) {
                    val isAccessible = it.isAccessible
                    it.isAccessible = true
                    try {
                        it.setter.call(theObject, initVal)
                    } catch (e: IllegalArgumentException) {
                        throw IllegalArgumentException("Error setting value for parameter $name: expected type ${it.setter.valueParameters.first().type}, got ${initVal::class.simpleName}")
                    }
                    it.isAccessible = isAccessible
                }
            }
            if (useSetter) {
                val setter = theObject::class.memberFunctions.firstOrNull { it.name == "set${name.capitalize()}" }
                setter?.let { s ->
                    try {
                        s.call(theObject, initVal)
                    } catch (e: IllegalArgumentException) {
                        throw IllegalArgumentException("Error setting value for parameter $name: expected type ${s.valueParameters.first().type}, got ${initVal::class.simpleName}")
                    }
                } ?: setKotlinProperty()
            } else {
                setKotlinProperty()
            }
        }
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

        // If this is a numeric type, test against the min and max values if specified.
        if (isNumeric) {
            val numericValue = initVal as Number
            require(!(hasMinValue() && numericValue.toDouble() < annotation.minimumValue)) { "Value is lower than minimum allowed." }
            require(!(hasMaxValue() && numericValue.toDouble() > annotation.maximumValue)) { "Value is greater than maximum allowed." }
        }
        return null
    }

    /**
     * Impose ordering by [UserParameter.order] and then field name.
     */
    override fun compareTo(other: Parameter): Int {
        val result = annotation.order.compareTo(other.annotation.order)
        return if (result != 0) {
            result
        } else {
            name.compareTo(other.name)
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
        private val classParameters: MutableMap<KClass<*>, Set<Parameter>> = HashMap()

        /**
         * Get the available [Parameter]s ([UserParameter] annotated
         * Fields) defined in the specified class. The available Parameters are
         * statically cached.
         *
         * @return The available Parameters, sorted according to [ ][UserParameter.order]()}.
         */
        fun getParameters(paramClass: KClass<*>): Set<Parameter> {
            return classParameters.getOrPut(paramClass) {
                val params = (paramClass.allSuperclasses + paramClass).flatMap { clazz ->
                    clazz.declaredMemberProperties.filter { it.hasAnnotation<UserParameter>() }
                        .map { Parameter(it) }
                }
                Collections.unmodifiableSet(params.toSet())
            }
        }

        /**
         * Set of all floating-point types.
         */
        protected val floatTypes: MutableSet<KClass<out Any>> = mutableSetOf(
            Double::class,
            Float::class,
            BigDecimal::class
        )

        /**
         * Set of all integer types.
         */
        protected val integerTypes: MutableSet<KClass<out Any>> = mutableSetOf(
            Byte::class,
            Short::class,
            Int::class,
            Long::class
        )
    }

    init {
        annotation = property.findAnnotations(UserParameter::class).first()
    }
}