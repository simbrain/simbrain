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
import org.simbrain.network.neuron_update_rules.UpdateRuleEnum;
import org.simbrain.network.updaterules.AdExIFRule;
import org.simbrain.network.updaterules.IntegrateAndFireRule;
import org.simbrain.network.updaterules.IzhikevichRule;
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule;
import org.simbrain.network.util.EmptyMatrixData;
import org.simbrain.network.util.EmptyScalarData;
import org.simbrain.network.util.MatrixDataHolder;
import org.simbrain.network.util.ScalarDataHolder;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.CopyableObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A rule for updating a neuron.
 *
 * @author jyoshimi
 */
public abstract class NeuronUpdateRule<DS extends ScalarDataHolder, DM extends MatrixDataHolder> implements CopyableObject {

    /**
     * Rules for drop-down list used by {@link org.simbrain.util.propertyeditor.ObjectTypeEditor} to set the update rule
     * on a neuron.
     */
    public static List<Class> RULE_LIST = Arrays.stream(UpdateRuleEnum.values())
            .map(UpdateRuleEnum::getRule)
            .collect(Collectors.toList());

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
     * Default data holder for scalar data.
     */
    private static final ScalarDataHolder DEFAULT_SCALAR_DATA = EmptyScalarData.INSTANCE;

    /**
     * Default data holder for matrix data.
     */
    private static final MatrixDataHolder DEFAULT_MATRIX_DATA = EmptyMatrixData.INSTANCE;

    /**
     * Provides the network level time step.
     */
    protected Supplier<Double> timeStepSupplier;

    /**
     * Rules in this list use a custom zero point.
     * Currently this is set to be between upper and lower bounds.  In the future more
     * options could be added and this could become an enum.
     * <p>
     * Add an update type to this list if a neuron should use a custom zero point
     */
    private static final HashSet<Class> usesCustomZeroPoint =
            new HashSet<>(Arrays.asList(IntegrateAndFireRule.class, AdExIFRule.class, IzhikevichRule.class));

    /**
     * Defines the update rule as it applies to scalar data.
     *
     * TODO: Finish doing this for all update rules.
     *
     * @param neuron a reference to a neuron with its parameters and access to parent network (and thus time step, etc).
     * @param data a scalar data holder that can hold data that must be updated with this rule.
     */
    public abstract void apply(Neuron neuron, DS data);

    /**
     * Override to return an appropriate data holder for a given rule.
     */
    public DS createScalarData() {
        return (DS) DEFAULT_SCALAR_DATA;
    }

    /**
     * Override to define a neural update rule for Neuron Arrays
     *
     * NOTE: Only a few of these have been done.
     *
     * @param layer reference to a layer and its matrix-valued data (inputs, activations).
     * @param dataHolder a holder for mutable data used in matrix versions of an update rule
     */
    public void apply(Layer layer, DM dataHolder) {}

    /**
     * Override to return an appropriate data holder for a given rule.
     */
    public DM createMatrixData(int size) {
        return (DM) DEFAULT_MATRIX_DATA;
    }

    /**
     * Returns a name for this update rule.  Used in combo boxes in the GUI.
     *
     * @return the description.
     */
    @Override
    public abstract String getName();

    @Override
    public NeuronUpdateRule copy() {
        return deepCopy();
    }

    /**
     * Returns the type of time update (discrete or continuous) associated with this neuron.
     *
     * @return the time type
     */
    public abstract TimeType getTimeType();

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
        n.clip();
    }

    /**
     * Decrement a neuron by increment, respecting neuron specific constraints. Intended to be overriden.
     *
     * @param n neuron
     */
    public void contextualDecrement(Neuron n) {
        decrementActivation(n);
        n.clip();
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
     * Returns string for tool tip or short description. Override to provide custom information.
     *
     * @param neuron reference to parent neuron
     * @return tool tip text
     */
    public String getToolTipText(final Neuron neuron) {
        return neuron.getId() + ".  Location: (" + (int) neuron.getX() + "," + (int) neuron.getY() + "). Activation: " + Utils.round(neuron.getActivation(), MAX_DIGITS);
    }

    public boolean isSpikingRule() {
        return false;
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