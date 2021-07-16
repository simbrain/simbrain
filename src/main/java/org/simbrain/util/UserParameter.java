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

import org.simbrain.util.math.ProbabilityDistribution;

import java.lang.annotation.*;

/**
 * Annotation for user-configurable parameter fields that provides for
 * specifying meta-data such as label, description, and validation criteria.
 * This information may be used by dialog builders to construct input fields.
 * <br>
 * The annotation can be used on fields, or on methods of interfaces.
 * <br>
 * When used in a method, it should be used on a getter, and the interface that
 * contains the getter should have an appropriately named corresponding setter.
 * E.g. <code>getLowerBound</code> will try to find a setter named
 * <code>setUpperBound</code> or <code>isClipped</code> will be associated with
 * <code>setClipped</code>.
 *
 * @author O. J. Coleman
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserParameter {

    /**
     * Label for the parameter.
     */
    String label();

    /**
     * Description for the parameter. Displayed in the corresponding widget's
     * tooltip.
     */
    String description() default "";

    /**
     * An optional parameter which prevents the dialog from accepting values
     * smaller than this.  Distinct from model code which enforces upper or lower
     * bounds.
     */
    double minimumValue() default Double.NaN;

    /**
     * An optional parameter which prevents the dialog from accepting values
     * larger than this. Distinct from model code which enforces upper or lower
     * bounds.
     */
    double maximumValue() default Double.NaN;

    /**
     * Name of a method which returns a String, which is the initial value for the String widget.
     * Note: Currently only supported for setting a String field for a single object.
     */
    // TODO: Possibly generalize to objects of any type. Currently only works with String based fields.
    // TODO: Think about multi-object case.
    String initialValueMethod() default "";

    /**
     * Amount that the {@link org.simbrain.util.widgets.SpinnerNumberModelWithNull}
     * changes when clicked up or down. Note that it is up to the user of the
     * annotation to set this so that it makes sense.
     */
    double increment() default 1;

    /**
     * If set to true, then when setting a parameter value the setter is invoked.
     * Assumes that the setter is named set + fieldName, where the first letter
     *  of field name is capitalized. E.g. activation -> setActivation.
     */
    boolean useSetter() default false;

    /**
     * A string to indicate a user preference stored in {@link SimbrainPreferences}.
     * If this optional field is set a restore defaults button will appear in the
     * property editor which, if pressed, will restore a fields value using
     * this key.
     *
     * TODO: Note that "restore defaults" has not yet been implemented.
     */
    String preferenceKey() default "";

    /**
     * The probability distribution to use when generating random values for
     * this parameter. For options see {@link ProbabilityDistribution#getBuilder(String)}
     */
    String probDist() default "";

    /**
     * The default "first parameter" to use when when opening the randomizer panel for this field.
     * Corresponds to the mean of a normal distribution. For other cases see
     * {@link ProbabilityDistribution#getBuilder(String, double, double)}
     */
    double probParam1() default 0;

    /**
     * The default "second parameter" to use when when opening the randomizer panel for this field.
     * Corresponds to the standard deviation of a normal distribution. For other cases see
     * {@link ProbabilityDistribution#getBuilder(String, double, double)}
     */
    double probParam2() default 1.0;

    /**
     * Used to determine the order of parameters when displayed to a user.
     * Optional. If two parameters have the same order value then they will be
     * ordered according to the field name. Lower numbers are higher up in the panel.
     */
    int order() default 0;

    /**
     * The name of the tab this parameter associated with.
     *
     * @return the label of the tab
     */
    String tab() default "Main";

    /**
     * Whether the parameter represents an object to be edited by
     * a {@link org.simbrain.util.propertyeditor.ObjectTypeEditor}. Assumes
     * the relevant object is an abstract class and the user wants to specify
     * the type of that class: examples include NeuronUpdateRules and their types,
     * SynapseUpdateRules and their types, etc.
     */
    boolean isObjectType() default false;

    /**
     * For object type editors, whether the detail triangle should be present when opening
     * the editor
     */
    boolean showDetails() default true;

    /**
     * Method name for static method returning the type map for an {@link org.simbrain.util.propertyeditor.ObjectTypeEditor},
     * e.g. "getTypeMap".  Defaults to "getTypes". This method should be contained in the
     * class whose types are being edited. For example {@link ProbabilityDistribution} has a {@code getTypes()} method that
     * returns a list of the types of its subclasses.
     *
     * @return the name of the method that returns types.  The default is usually fine.
     */
    String typeListMethod() default "getTypes";

    /**
     * Set to false to make this a "display" type.
     */
    boolean editable() default true;

    /**
     * Name of a method whose value determines whether this component is visible or not. Only called once when the
     * editor is opened.
     */
    String conditionalVisibilityMethod() default "";

    /**
     * Name of another widget (based on its {@link #description() UserParameter} whose state determines if this widget
     * is enabled or not. This changes dynamically.  Currently works on booleans only.
     */
    String condtionalEnablingWidget() default "";

    /**
     * Regular expression to validate (String) values against. This is only
     * applied to parameters that are provided as strings. Optional.
     */
    String regexValidation() default "";

     /**
     * Returns true if the annotated field implements {@link org.simbrain.network.util.DataHolder}, in
     * which case it is treated as an embedded property editor.
     */
    boolean isDataHolder() default false;
}
