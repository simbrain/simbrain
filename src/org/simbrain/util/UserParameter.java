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
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.propertyeditor2.CopyableObject;

import java.lang.annotation.*;

/**
 * Annotation for user-configurable parameter fields that provides for
 * specifying meta-data such as label, description, and validation criteria.
 * This information may be used by dialog builders to construct input fields.
 *
 * The annotation can be used on fields, or on methods of interfaces.
 *
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
     * Description for the parameter.
     */
    String description() default "";

    /**
     * Whether the parameter is optional. Note that if a defaultValue is
     * specified then this will be used even if optional is set.
     */
    boolean optional() default false;

    /**
     * A default value for the parameter.
     */
    String defaultValue() default "";

    /**
     * For numeric types, a minimum value, inclusive. Optional.
     */
    double minimumValue() default Double.NaN;

    /**
     * For numeric types, a maximum value, inclusive. Optional.
     */
    double maximumValue() default Double.NaN;

    /**
     * Regular expression to validate (String) values against. This is only
     * applied to parameters that are provided as strings. Optional.
     */
    String regexValidation() default "";

    /**
     * The probability distribution to use when generating random values for
     * this parameter. Optional. NOT IMPLEMENTED YET.
     */
    Class<? extends ProbabilityDistribution> probDistribution() default UniformDistribution.class;

    /**
     * The standard deviation to use when generating random values for this
     * parameter. Optional. NOT IMPLEMENTED YET.
     */
    double probStdDev() default 1.0;

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
     * a {@link org.simbrain.util.propertyeditor2.ObjectTypeEditor}. Assumes
     * the relevant object is an abstract class and the user wants to specify
     * the type of that class: examples include NeuronUpdateRules and their types,
     * SynapseUpdateRules and their types, etc.
     */
    boolean isObjectType() default false;

    /**
     * Method name for static method returning the type map for an {@link org.simbrain.util.propertyeditor2.ObjectTypeEditor},
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

}
