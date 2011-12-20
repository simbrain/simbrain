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
package org.simbrain.network.interfaces;

import org.simbrain.util.Utils;

/**
 * A rule for updating a synapse.  A learning rule.
 *
 * @author jyoshimi
 */
public abstract class SynapseUpdateRule {

    /** The maximum number of digits to display in the tool tip. */
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
     * Returns a brief description of this update rule. Used in combo boxes in
     * the GUI.
     *
     * @return the description.
     */
    public abstract String getDescription();

    /**
     * Set activation to 0; override for other "clearing" behavior (e.g. setting
     * other variables to 0. Called in Gui when "clear" button pressed.
     *
     * @param synapse reference to parent synapse
     */
    public void clear(final Synapse synapse) {
        synapse.setStrength(0); //TODO: Used?
    }

    /**
     * Returns string for tool tip or short description. Override to provide
     * custom information.
     *
     * @param synapse reference to parent synapse
     * @return tool tip text
     */
    public String getToolTipText(final Synapse synapse) {
        return "(" + synapse.getId() + ") Strength: "
                + Utils.round(synapse.getStrength(), MAX_DIGITS);
    }
}
