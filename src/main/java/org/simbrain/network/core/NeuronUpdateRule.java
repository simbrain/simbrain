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
import org.simbrain.network.neuron_update_rules.*;
import org.simbrain.network.neuron_update_rules.activity_generators.LogisticRule;
import org.simbrain.network.neuron_update_rules.activity_generators.RandomNeuronRule;
import org.simbrain.network.neuron_update_rules.activity_generators.SinusoidalRule;
import org.simbrain.network.neuron_update_rules.activity_generators.StochasticRule;
import org.simbrain.network.neuron_update_rules.interfaces.BoundedUpdateRule;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.CopyableObject;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * A rule for updating a neuron.
 *
 * @author jyoshimi
 */
public abstract class NeuronUpdateRule implements CopyableObject, AttributeContainer {

    /**
     * Rules for drop-down list used by {@link org.simbrain.util.propertyeditor.ObjectTypeEditor} to set the update rule
     * on a neuron.
     */
    public static List<Class> RULE_LIST = Arrays.asList(
            LogisticRule.class, StochasticRule.class,
            SinusoidalRule.class, RandomNeuronRule.class,
            AdExIFRule.class, BinaryRule.class, DecayRule.class, FitzhughNagumo.class, IACRule.class,
            IntegrateAndFireRule.class, IzhikevichRule.class, KuramotoRule.class,
            LinearRule.class, MorrisLecarRule.class, NakaRushtonRule.class,
            ProductRule.class, ContinuousSigmoidalRule.class, SigmoidalRule.class,
            SpikingThresholdRule.class, ThreeValueRule.class, TimedAccumulatorRule.class);

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
     * Rules in this list use a custom zero point.
     * Currently this is set to be between upper and lower bounds.  In the future more
     * options could be added and this could become an enum.
     * <p>
     * Add an update type to this list if a neuron should use a custom zero point
     */
    private static HashSet<Class> usesCustomZeroPoint =
            new HashSet<>(Arrays.asList(IntegrateAndFireRule.class, AdExIFRule.class, IzhikevichRule.class));

    /**
     * Returns the type of time update (discrete or continuous) associated with this neuron.
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
     *
     * @param n neuron
     */
    public final void incrementActivation(Neuron n) {
        n.forceSetActivation(n.getActivation() + n.getIncrement());
    }

    /**
     * Decrement a neuron by increment.
     *
     * @param n neuron
     */
    public final void decrementActivation(Neuron n) {
        n.forceSetActivation(n.getActivation() - n.getIncrement());
    }

    /**
     * Increment a neuron by increment, respecting neuron specific constraints. Intended to be overriden.
     *
     * @param n neuron to be incremented
     */
    public void contextualIncrement(Neuron n) {
        incrementActivation(n);
    }

    /**
     * Decrement a neuron by increment, respecting neuron specific constraints. Intended to be overriden.
     *
     * @param n neuron
     */
    public void contextualDecrement(Neuron n) {
        decrementActivation(n);
    }

    /**
     * Returns a random value between the upper and lower bounds of this neuron. Update rules that require special
     * randomization should override this method.
     *
     * @return the random value.
     */
    public double getRandomValue() {
        if (this instanceof BoundedUpdateRule) {
            return (((BoundedUpdateRule) this).getUpperBound() - ((BoundedUpdateRule) this).getLowerBound()) * Math.random() + ((BoundedUpdateRule) this).getLowerBound();
        } else {
            return 2 * Math.random() - 1;
        }

    }

    /**
     * Returns a value for lower bound to be used in computing the saturation of neuron nodes. Override this to produce
     * nicer graphics, and fine tune based on display of neurons in common use cases for a given neuron type.
     *
     * @return the graphical lower bound
     */
    public double getGraphicalLowerBound() {
        if (this instanceof BoundedUpdateRule) {
            return ((BoundedUpdateRule) this).getLowerBound();
        } else {
            return -1;
        }
    }

    /**
     * Returns a value for upper bound to be used in computing the saturation of neuron nodes. Override this to produce
     * nicer graphics, and fine tune based on display of neurons in common use cases for a given neuron type.
     *
     * @return the graphical upper bound
     */
    public double getGraphicalUpperBound() {
        if (this instanceof BoundedUpdateRule) {
            return ((BoundedUpdateRule) this).getUpperBound();
        } else {
            return 1;
        }
    }

    /**
     * Set activation to 0; override for other "clearing" behavior (e.g. setting other variables to 0. Called in Gui
     * when "clear" button pressed.
     *
     * @param neuron reference to parent neuron
     */
    public void clear(final Neuron neuron) {
        neuron.forceSetActivation(0);
    }

    /**
     * Returns a name for this update rule.  Used in combo boxes in the GUI.
     *
     * @return the description.
     */
    public abstract String getName();

    /**
     * Returns string for tool tip or short description. Override to provide custom information.
     *
     * @param neuron reference to parent neuron
     * @return tool tip text
     */
    public String getToolTipText(final Neuron neuron) {
        return neuron.getId() + ".  Location: (" + (int) neuron.getX() + "," + (int) neuron.getY() + "). Activation: " + Utils.round(neuron.getActivation(), MAX_DIGITS);
    }

    public boolean isSpikingNeuron() {
        return false;
    }

    @Override
    public EditableObject copy() {
        return deepCopy();
    }

    public double getGraphicalValue(Neuron n) {
        return n.getActivation();
    }

    /**
     * Checks if the provided rule uses a custom zero point or not.
     */
    public static boolean usesCustomZeroPoint(NeuronUpdateRule rule) {
        return usesCustomZeroPoint.contains(rule.getClass());
    }
}
