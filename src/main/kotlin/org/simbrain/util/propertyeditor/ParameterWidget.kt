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

import org.simbrain.util.*
import org.simbrain.util.widgets.*
import smile.math.matrix.Matrix
import java.awt.Color
import java.lang.reflect.InvocationTargetException
import java.util.function.Function
import java.util.stream.Collectors
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.SwingUtilities
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.functions
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.jvm.jvmErasure

/**
 * A wrapper class for a [Parameter] and an associated GUI widget
 * (JComponent).
 *
 * @author O. J. Coleman
 */
class ParameterWidget(
    /**
     * Parent property editor.
     */
    private val parent: AnnotatedPropertyEditor1,
    /**
     * The parameter for this widget.
     */
    val parameter: Parameter
) : Comparable<ParameterWidget> {

    /**
     * The GUI element for this widget.
     */
    val component: JComponent?

    /**
     * If true, then a custom value has been used to initialize the widget.
     */
    var isCustomInitialValue = false

    /**
     * Construct a parameter widget from a parameter, which in turn represents a
     * field.s
     */
    init {
        component = makeWidget()
        checkConditionalVisibility()
        setInitialValue()
    }

    /**
     * Check if this component should be visible or not, based on
     * [UserParameter.conditionalVisibilityMethod].
     */
    fun checkConditionalVisibility() {
        val conditionalVisibilityMethod: String = parameter.annotation.conditionalVisibilityMethod
        if (conditionalVisibilityMethod.isNotEmpty()) {
            try {
                val method = parent.editedObjects[0].javaClass.getMethod(conditionalVisibilityMethod)
                val visible = method.invoke(parent.editedObjects[0]) as Boolean
                component!!.isVisible = visible
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun checkConditionalEnabling(widgetValues: Map<String, Any?>) {
        val conditionalEnablingMethod: String = parameter.annotation.conditionalEnablingMethod
        if (conditionalEnablingMethod.isNotEmpty()) {
            try {
                val method = parent.editedObjects[0].javaClass.getMethod(conditionalEnablingMethod)
                val o = parent.editedObjects[0]
                if (method.returnType == Function::class.java) {
                    val checker = method.invoke(o) as Function<Map<String, Any?>, Boolean>
                    component!!.isEnabled = checker.apply(widgetValues)
                } else {
                    val checker = method.invoke(o) as (Map<String, Any?>) -> Boolean
                    component!!.isEnabled = checker(widgetValues)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Set the initial value of this widget if the [UserParameter.initialValueMethod]
     * is set.
     */
    private fun setInitialValue() {
        val initialValueMethod: String = parameter.annotation.initialValueMethod
        if (!initialValueMethod.isEmpty()) {
            // TODO: type check, or generalize to any object type
            try {
                val method = parent.editedObjects[0].javaClass.getDeclaredMethod(initialValueMethod)
                val initValue = method.invoke(parent.editedObjects[0]) as String
                (component as TextWithNull?)!!.text = initValue
                isCustomInitialValue = true
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Create an appropriate widget for this parameter.
     */
    private fun makeWidget(): JComponent {
        if (parameter.isObjectType) {
            // Assumes the type list is contained in the class corresponding to the parameter.
            // Example: NeuronUpdateRule maintains a list of types  of neuronupdaterules.
            val methodName: String = parameter.annotation.typeListMethod
            val typeMap = getTypeMap(
                parameter.type.jvmErasure, methodName
            )
            val ote = ObjectTypeEditor.createEditor(
                editableObjects as List<CopyableObject>, typeMap,
                parameter.annotation.label, parameter.annotation.showDetails
            )
            ote.dropDown!!.addActionListener { SwingUtilities.invokeLater { parent.onWidgetChanged() } }
            return ote
        }

        // Embedded objects are converted into separate property editors
        if (parameter.isEmbeddedObject) {
            @Suppress("UNCHECKED_CAST")
            return EmbeddedObjectEditor(
                editableObjects as List<CopyableObject>,
                parameter.annotation.label,
                parameter.annotation.showDetails
            )
        }
        if (parameter.isDisplayOnly) {
            return JLabel()
        }
        if (parameter.isBoolean) {
            val ynn = YesNoNull()
            ynn.addActionListener { e ->
                SwingUtilities.invokeLater {
                    parent.onWidgetChanged()
                }
            }
            return ynn
        }
        if (parameter.isColor) {
            return ColorSelector()
        }
        if (parameter.isDoubleArray) {
            return MatrixWidget()
        }
        if (parameter.isMatrix) {
            return MatrixWidget()
        }
        if (parameter.isIntArray) {
            return IntArrayWidget1()
        }
        if (parameter.isEnum) {
            try {
                val clazz = parameter.type
                val method = clazz.jvmErasure.functions.find { it.name == "values" }
                // TODO: Not sure the null argument is correct below.
                val enumValues = method!!.call() as Array<Any>
                return ChoicesWithNull(enumValues).also {
                    it.addActionListener { e ->
                        SwingUtilities.invokeLater {
                            parent.onWidgetChanged()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (parameter.isNumeric) {

            // For when preference values are implemented later
            // if(!parameter.getAnnotation().preferenceKey().isEmpty()) {
            //     System.out.println(SimbrainPreferences.getDouble(parameter.getAnnotation().preferenceKey()));
            // }
            val spinnerModel: SpinnerNumberModelWithNull
            val minValue: Double? = if (parameter.hasMinValue()) parameter.annotation.minimumValue else null
            val maxValue: Double? = if (parameter.hasMaxValue()) parameter.annotation.maximumValue else null

            // Step size if bounds are missing
            val stepSize: Double = parameter.annotation.increment

            // If desired, an "auto-increment" field could be added to userparameter.
            // Code for this is in revision 564596
            spinnerModel = if (parameter.isNumericInteger) {
                val step = Math.max(1.0, stepSize).toInt()
                SpinnerNumberModelWithNull(0, minValue?.toInt(), maxValue?.toInt(), step)
            } else {
                SpinnerNumberModelWithNull(0.0, minValue, maxValue, stepSize)
            }
            val setNull = Runnable { widgetValue = null }
            return NumericWidget(
                parent.editedObjects,
                parameter,
                spinnerModel,
                setNull
            )
        }
        if (parameter.isString) {
            return TextWithNull()
        }
        throw IllegalArgumentException(
            "You have annotated a field type (" +
                    parameter.type.jvmErasure.qualifiedName + ") that is not yet supported"
        )
    }

    // if (parameter.hasDefaultValue()) {
    //     tips.add("Default: " + parameter.getDefaultValue());
    // }

    /**
     * Returns tooltip text for this parameter.
     */
    val toolTipText: String
        get() {
            val anot = parameter.annotation
            val tips: MutableList<String> = ArrayList()
            if (anot.description.isEmpty()) {
                tips.add(anot.label)
            } else {
                tips.add(anot.description)
            }
            // if (parameter.hasDefaultValue()) {
            //     tips.add("Default: " + parameter.getDefaultValue());
            // }
            if (parameter.hasMinValue()) {
                tips.add(
                    "Minimum: " + if (parameter.isNumericInteger) "" + anot.minimumValue
                        .toInt() else "" + anot.minimumValue
                )
            }
            if (parameter.hasMaxValue()) {
                tips.add(
                    "Maximum: " + if (parameter.isNumericInteger) "" + anot.maximumValue
                        .toInt() else "" + anot.maximumValue
                )
            }
            return if (tips.isEmpty()) {
                ""
            } else {
                "<html>${tips.joinToString("<br />")}</html>"
            }
        }

    var widgetValue: Any?
        get() = if (parameter.isDisplayOnly) {
            (component as JLabel).text
        } else if (parameter.isString) {
            if ((component as TextWithNull?)!!.isNull()) null else component!!.text
        } else if (parameter.isNumeric) {
            if ((component as NumericWidget?)!!.isNull) null else when (parameter.type.jvmErasure) {
                Double::class -> component!!.value
                Float::class -> (component!!.value as Double).toFloat()
                Int::class -> component!!.value
                Long::class -> component!!.value
                Short::class -> component!!.value
                else -> throw IllegalArgumentException("Invalid numeric type ${parameter.type.jvmErasure.qualifiedName}")
            }
        } else if (parameter.isBoolean) {
            if ((component as YesNoNull?)!!.isNull) null else component!!.isSelected
        } else if (parameter.isColor) {
            (component as ColorSelector?)!!.value
        } else if (parameter.isDoubleArray) {
            (component as MatrixWidget?)!!.values.transpose().toDoubleArray()
        } else if (parameter.isIntArray) {
            (component as IntArrayWidget1?)!!.values
        } else if (parameter.isMatrix) {
            (component as MatrixWidget?)!!.values.let {
                if (it.nrow() == 1) {
                    it.transpose()
                } else {
                    it
                }
            }
        } else if (parameter.annotation.isObjectType) {
            (component as ObjectTypeEditor?)!!.value
        } else if (parameter.annotation.isEmbeddedObject) {
            (component as EmbeddedObjectEditor).value
        } else if (parameter.isEnum) {
            if ((component as ChoicesWithNull?)!!.isNull) null else component!!.selectedItem
        } else {
            throw IllegalArgumentException("Trying to retrieve a value from an unsupported widget type")
        }
        set(value) {
            if (parameter.isDisplayOnly) {
                // non-editable widgets are represented with JLabels
                (component as JLabel).text = value?.toString() ?: SimbrainConstants.NULL_STRING
            } else if (parameter.isBoolean) {
                if (value == null) {
                    (component as YesNoNull).setNull()
                } else {
                    (component as YesNoNull).isSelected = (value as Boolean?)!!
                }
            } else if (parameter.isColor) {
                (component as ColorSelector?)!!.value = value as Color?
            } else if (parameter.isDoubleArray) {
                (component as MatrixWidget?)!!.values = (value as DoubleArray).toMatrix().transpose()
            } else if (parameter.isIntArray) {
                (component as IntArrayWidget1?)!!.values = value as IntArray?
            } else if (parameter.isMatrix) {
                (component as MatrixWidget?)!!.values = (value as Matrix).let {
                    if (it.ncol() == 1) {
                        it.transpose()
                    } else {
                        it
                    }
                }
            } else if (parameter.isNumeric) {
                (component as NumericWidget?)!!.value = value
            } else if (parameter.annotation.isObjectType) {
                // No action. ObjectTypeEditor handles its own init
            } else if (parameter.isEnum) {
                if (value == null) {
                    (component as ChoicesWithNull?)!!.setNull()
                } else {
                    (component as ChoicesWithNull?)!!.selectedItem = value
                }
            } else if (parameter.isString) {
                if (value == null) {
                    (component as TextWithNull?)!!.setNull()
                } else {
                    (component as TextWithNull?)!!.text = value.toString()
                }
            }
        }

    /**
     * Impose ordering by [UserParameter.order] and then field name.
     */
    override fun compareTo(other: ParameterWidget): Int {
        return parameter.compareTo(other.parameter)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is ParameterWidget) {
            parameter == other.parameter
        } else false
    }

    override fun hashCode(): Int {
        return parameter.hashCode()
    }

    /**
     * Convenience method to get the label for this parameter.
     *
     * @return the label
     */
    val label: String
        get() = parameter.annotation.label

    /**
     * If true the widget is representing fields with different values.
     * Generally some version of "..." is displayed.
     */
    val isInconsistent: Boolean
        get() {
            if (parameter.isBoolean) {
                return (component as YesNoNull?)!!.isNull
            }
            if (parameter.isNumeric) {
                val theVal = (component as NumericWidget?)!!.value ?: return true
            }
            if (parameter.isString) {
                if ((component as TextWithNull?)!!.isNull()) {
                    return true
                }
            }
            if (parameter.annotation.isObjectType) {
                if ((component as ObjectTypeEditor?)!!.isInconsistent) {
                    return true
                }
            }
            if (parameter.isEnum) {
                if ((component as ChoicesWithNull?)!!.isNull) {
                    return true
                }
            }
            return false
        }

    /**
     * Return a list of objects associated with this field. Example: a list of neuronupdaterules associated with the
     * updaterule field, one object for each neuron in the list of edited objects.
     */
    private val editableObjects: List<EditableObject>
        private get() = parent.editedObjects.stream()
            .map { o: EditableObject? -> parameter.getFieldValue(o) as EditableObject }
            .collect(Collectors.toList())

    companion object {
        /**
         * Returns the map from names to types for use in combo boxes. (for example "Linear" -> LinearRule::class)
         */
        @JvmStatic
        fun getTypeMap(c: KClass<*>, methodName: String?): BiMap<String, KClass<*>> {
            val typeMap = BiMap<String, KClass<*>>()

            // getting the methodName function (usually named `getTypes`)
            val functions = c.staticFunctions.toMutableList()
            c.companionObject?.functions?.let { functions.addAll(it) }
            val m = functions.firstOrNull { it.name == methodName } ?: throw IllegalArgumentException(
                "Could not find method $methodName in class ${c.qualifiedName}"
            )

            // calling the getTypes function which returns a list of classes
            val types = if (m.parameters.isEmpty()) {
                m.call() as List<Class<*>>
            } else {
                m.call(c.companionObjectInstance) as List<Class<*>>
            }

            // constructing the map from names to types.
            types.map { it.kotlin.callNoArgConstructor() as EditableObject }
                .forEach { typeMap[it.name] = it::class }
            return typeMap
        }
    }
}