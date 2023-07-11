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
package org.simbrain.util

/**
 * Annotation for user-configurable parameter fields that provides for
 * specifying meta-data such as label, description, and validation criteria.
 * This information may be used by dialog builders to construct input fields.
 * <br></br>
 * The annotation can be used on fields or getter or setter named using java conventions.
 * <br></br>
 * When used in a method, it should be used on a getter, and the interface that
 * contains the getter should have an appropriately named corresponding setter.
 * E.g. `getLowerBound` will try to find a setter named
 * `setUpperBound` or `isClipped` will be associated with
 * `setClipped`.
 *
 * @author O. J. Coleman
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
annotation class UserParameter(
    /**
     * Label for the parameter.
     */
    val label: String,
    /**
     * Description for the parameter. Displayed in the corresponding widget's
     * tooltip.
     */
    val description: String = "",
    /**
     * An optional parameter which prevents the dialog from accepting values
     * smaller than this.  Distinct from model code which enforces upper or lower
     * bounds.
     */
    val minimumValue: Double = Double.NaN,
    /**
     * An optional parameter which prevents the dialog from accepting values
     * larger than this. Distinct from model code which enforces upper or lower
     * bounds.
     */
    val maximumValue: Double = Double.NaN,
    /**
     * Name of a method which returns a String, which is the initial value for the String widget.
     * Note: Currently only supported for setting a String field for a single object.
     */
    // TODO: Possibly generalize to objects of any type. Currently only works with String based fields.
    // TODO: Think about multi-object case.
    val initialValueMethod: String = "",
    /**
     * Amount that the [org.simbrain.util.widgets.SpinnerNumberModelWithNull]
     * changes when clicked up or down. Note that it is up to the user of the
     * annotation to set this so that it makes sense.
     */
    val increment: Double = 1.0,
    /**
     * If set to true, then when setting a parameter value the setter is invoked.
     * Assumes that the setter is named set + fieldName, where the first letter
     * of field name is capitalized. E.g. activation -> setActivation.
     */
    val useSetter: Boolean = false,
    /**
     * A string to indicate a user preference stored in [SimbrainPreferences].
     * If this optional field is set a restore defaults button will appear in the
     * property editor which, if pressed, will restore a fields value using
     * this key.
     *
     * TODO: Note that "restore defaults" has not yet been implemented.
     */
    val preferenceKey: String = "",
    /**
     * The probability distribution to use when generating random values for
     * this parameter. For options see [ProbabilityDistribution]
     */
    val probDist: String = "",
    /**
     * The default "first parameter" to use when when opening the randomizer panel for this field.
     * Corresponds to the mean of a normal distribution. For other cases see
     * [ProbabilityDistribution]
     */
    val probParam1: Double = 0.0,
    /**
     * The default "second parameter" to use when when opening the randomizer panel for this field.
     * Corresponds to the standard deviation of a normal distribution. For other cases see
     * [ProbabilityDistribution]
     */
    val probParam2: Double = 1.0,
    /**
     * Used to determine the order of parameters when displayed to a user.
     * Optional. If two parameters have the same order value then they will be
     * ordered according to the field name. Lower numbers are higher up in the panel.
     */
    val order: Int = 0,
    /**
     * The name of the tab this parameter associated with. If a single tab annotation is set then the
     * AnnotatedPropertyEditor will be a JTabbedPane. In that case, any field in the EditableObject where the tab
     * annotation is empty will be put in a "Main" tab.
     */
    val tab: String = "",
    /**
     * Whether the parameter represents an object to be edited by
     * a [org.simbrain.util.propertyeditor.ObjectTypeEditor]. Assumes
     * the relevant object is an abstract class and the user wants to specify
     * the type of that class: examples include NeuronUpdateRules and their types,
     * SynapseUpdateRules and their types, etc.
     */
    val isObjectType: Boolean = false,
    /**
     * For object type editors, whether the detail triangle should be present when opening
     * the editor
     */
    val showDetails: Boolean = true,
    /**
     * Method name for static method returning the type map for an [org.simbrain.util.propertyeditor.ObjectTypeEditor],
     * e.g. "getTypeMap".  Defaults to "getTypes". This method should be contained in the
     * class whose types are being edited. For example [ProbabilityDistribution] has a `getTypes()` method that
     * returns a list of the types of its subclasses.
     *
     * @return the name of the method that returns types.  The default is usually fine.
     */
    val typeListMethod: String = "getTypes",
    /**
     * Set to false to make this a "display" type.
     */
    val editable: Boolean = true,
    /**
     * Name of a method whose value determines whether this component is visible or not. Only called once when the
     * editor is opened.
     */
    val conditionalVisibilityMethod: String = "",
    /**
     * Name of a method which returns a lambda which is used to determine whether this component is enabled or not.
     */
    val conditionalEnablingMethod: String = "",
    /**
     * Returns true if the annotated field contains an object with its own annotated fields, allowing for recursie
     * embedding of property editors.
     */
    val isEmbeddedObject: Boolean = false,
    /**
     * Used when the state of this field should be updated based on the state of another field, which must be an enum or
     * object type editor which changed its state.  Specifies a method called by reflection when that state change occurs.
     *
     * The method is specified in the format of `updateRule.createMatrixData`. In this example, when updateRule was changed,
     * the annotated field will be updated to the value provided by `updateRule.createMatrixData()`.
     *
     * If this is used, [isEmbeddedObject] must be true.
     */
    val refreshSource: String = "",
)
