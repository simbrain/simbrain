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
package org.simbrain.network.gui;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.simbrain.util.math.ProbDistribution;

/**
 * Annotation for user-configurable parameter fields that provides for specifying meta-data such as label, description,
 * and validation criteria. This information may be used by dialog builders to construct input fields.
 * 
 * @author O. J. Coleman
 */
@Documented
@Target(ElementType.FIELD)
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
	 * this parameter. Optional. 
	 * NOT IMPLEMENTED YET.
	 */
	ProbDistribution probDistribution() default ProbDistribution.UNIFORM;

	/**
	 * The standard deviation to use when generating random values for this 
	 * parameter. Optional. NOT IMPLEMENTED YET.
	 */
	double probStdDev() default 1.0;

	/**
	 * Used to determine the order of parameters when displayed to a user. 
	 * Optional. If two parameters have the same order value then they will 
	 * be ordered according to the field name.
	 */
	int order() default 0;
}
