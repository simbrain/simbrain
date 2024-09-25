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

import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.GuiEditable

/**
 * Annotation for user-configurable parameter fields that provides for
 * specifying meta-data such as label, description, and validation criteria.
 * This information may be used by dialog builders to construct input fields.
 *
 * We have migrated to [GuiEditable] but java classes must use annotations and there are a few other cases where
 * GuiEditable can't be used.
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
     * Amount that the [org.simbrain.util.widgets.SpinnerNumberModelWithNull]
     * changes when clicked up or down. Note that it is up to the user of the
     * annotation to set this so that it makes sense.
     */
    val increment: Double = 1.0,
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
     * For object type editors, whether the detail triangle should be present when opening
     * the editor
     */
    val showDetails: Boolean = true,
    /**
     * If true only show the value but don't allow editing.
     */
    val displayOnly: Boolean = false,
    /**
     * Specify the name of a custom get type function when more than one is needed, beyond [CopyableObject.getTypeList]
     */
    val typeMapProvider: String = "",

    val useLegacySetter: Boolean = false,

    val columnMode: Boolean = false,

    val useFileChooser: Boolean = false,
)
