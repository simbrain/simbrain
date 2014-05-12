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

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.util.Utils;

/**
 * A rule for updating a neuron.
 *
 * @author jyoshimi
 */
public abstract class NeuronUpdateRule {

    /** The maximum number of digits to display in the tool tip. */
    private static final int MAX_DIGITS = 9;

    /** The default increment of a neuron using this rule. */
    public static final double DEFAULT_INCREMENT = 0.1;

    /** Amount by which to increment or decrement neuron. */
    protected double increment = DEFAULT_INCREMENT;

    /**
     * Returns the type of time update (discrete or continuous) associated with
     * this neuron.
     *
     * @return the time type
     */
    public abstract TimeType getTimeType();

    /**
     * Apply the update rule.
     *
     * @param neuron parent neuron
     */
    public abstract void update(Neuron neuron);

    /**
     * Returns a deep copy of the update rule.
     *
     * @return Duplicated update rule
     */
    public abstract NeuronUpdateRule deepCopy();

    /**
     * Increment a neuron by increment.
     */
    public final void incrementActivation(Neuron n) {
        n.forceSetActivation(n.getActivation() + increment);
        n.getNetwork().fireNeuronChanged(n);
    }

    /**
     * Decrement a neuron by increment.
     */
    public final void decrementActivation(Neuron n) {
        n.forceSetActivation(n.getActivation() - increment);
        n.getNetwork().fireNeuronChanged(n);
    }

    /**
     * Increment a neuron by increment, respecting neuron specific constraints.
     * Intended to be overriden.
     */
    public void contextualIncrement(Neuron n) {
        incrementActivation(n);
    }

    /**
     * Decrement a neuron by increment, respecting neuron specific constraints.
     * Intended to be overriden.
     */
    public void contextualDecrement(Neuron n) {
        decrementActivation(n);
    }

    /**
     * Returns a random value between the upper and lower bounds of this neuron.
     * Update rules that require special randomization should override this
     * method.
     *
     * @return the random value.
     */
    public double getRandomValue() {
        return (getCeiling() - getFloor()) * Math.random() + getFloor();
    }

    /**
     * Set activation to 0; override for other "clearing" behavior (e.g. setting
     * other variables to 0. Called in Gui when "clear" button pressed.
     *
     * @param neuron reference to parent neuron
     */
    public void clear(final Neuron neuron) {
        neuron.forceSetActivation(0);
    }

    /**
     * Upper bound for activation.
     *
     * @return the ceiling
     */
    public abstract double getCeiling();

    /**
     * Lower bound for activation.
     *
     * @return the floor
     */
    public abstract double getFloor();

    /**
     * Returns a brief description of this update rule. Used in combo boxes in
     * the GUI.
     *
     * @return the description.
     */
    public abstract String getDescription();

    /**
     * Returns string for tool tip or short description. Override to provide
     * custom information.
     *
     * @param neuron reference to parent neuron
     * @return tool tip text
     */
    public String getToolTipText(final Neuron neuron) {
        return "(" + neuron.getId() + ") Activation: "
                + Utils.round(neuron.getActivation(), MAX_DIGITS);
    }

    /**
     * @return the increment
     */
    public double getIncrement() {
        return increment;
    }

    /**
     * @param increment the increment to set
     */
    public void setIncrement(double increment) {
        this.increment = increment;
    }

    /**
     * @return the defaultIncrement
     */
    public static double getDefaultIncrement() {
        return DEFAULT_INCREMENT;
    }

}
