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
package org.simbrain.network.core;

import org.simbrain.network.synapse_update_rules.*;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor2.CopyableObject;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.workspace.AttributeContainer;

import java.util.Arrays;
import java.util.List;

/**
 * A rule for updating a synapse. A learning rule.
 *
 * @author jyoshimi
 */
public abstract class SynapseUpdateRule implements CopyableObject, AttributeContainer {

    /**
     * Rules for drop-down list used by {@link org.simbrain.util.propertyeditor2.ObjectTypeEditor}
     * to set the learning rule on a synapse.
     */
    public static List<Class> RULE_LIST = Arrays.asList(StaticSynapseRule.class,
        HebbianRule.class, HebbianCPCARule.class, HebbianThresholdRule.class,
        OjaRule.class, PfisterGerstner2006Rule.class, ShortTermPlasticityRule.class,
        STDPRule.class, SubtractiveNormalizationRule.class);

    /**
     * Called via reflection using {@link UserParameter#typeListMethod()}.
     */
    public static List<Class> getTypes() {
        return RULE_LIST;
    }

    /**
     * The maximum number of digits to display in the tool tip.
     */
    private static final int MAX_DIGITS = 9;

    /**
     * Initialize the update rule and make necessary changes to the parent
     * synapse.
     *
     * @param synapse parent synapse
     */
    public abstract void init(Synapse synapse);

    /**
     * Apply the update rule.
     *
     * @param synapse parent synapse
     */
    public abstract void update(Synapse synapse);

    /**
     * Returns a deep copy of the update rule.
     *
     * @return Duplicated update rule
     */
    public abstract SynapseUpdateRule deepCopy();

    /**
     * Returns a name for this learning rule. Used in combo boxes in
     * the GUI.
     *
     * @return the description.
     */
    public abstract String getName();

    /**
     * Returns string for tool tip or short description. Override to provide
     * custom information.
     *
     * @param synapse reference to parent synapse
     * @return tool tip text
     */
    public String getToolTipText(final Synapse synapse) {
        return "(" + synapse.getId() + ") Strength: " + Utils.round(synapse.getStrength(), MAX_DIGITS);
    }

    @Override
    public EditableObject copy() {
        return deepCopy();
    }


}
