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
package org.simbrain.network.core

import org.simbrain.custom_sims.simulations.AllostaticUpdateRule
import org.simbrain.network.neuron_update_rules.*
import org.simbrain.network.neuron_update_rules.activity_generators.LogisticRule
import org.simbrain.network.neuron_update_rules.activity_generators.RandomNeuronRule
import org.simbrain.network.neuron_update_rules.activity_generators.SinusoidalRule
import org.simbrain.network.neuron_update_rules.activity_generators.StochasticRule
import org.simbrain.network.updaterules.*
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.Utils
import org.simbrain.util.propertyeditor.CopyableObject
import java.util.*
import java.util.function.Supplier

/**
 * A rule for updating a neuron.
 *
 * @author jyoshimi
 */
abstract class NeuronUpdateRule<out DS : ScalarDataHolder, out DM : MatrixDataHolder> : CopyableObject {
    /**
     * Provides the network level time step.
     */
    @JvmField
    protected var timeStepSupplier: Supplier<Double>? = null

    /**
     * Defines the update rule as it applies to scalar data.
     *
     * TODO: Finish doing this for all update rules.
     *
     * @param neuron a reference to a neuron with its parameters and access to parent network (and thus time step, etc).
     * @param data a scalar data holder that can hold data that must be updated with this rule.
     */
    abstract fun apply(neuron: Neuron, data: @UnsafeVariance DS)

    /**
     * Override to return an appropriate data holder for a given rule.
     */
    open fun createScalarData(): DS {
        return DEFAULT_SCALAR_DATA as DS
    }

    /**
     * Override to define a neural update rule for Neuron Arrays
     *
     * NOTE: Only a few of these have been done.
     *
     * @param layer reference to a layer and its matrix-valued data (inputs, activations).
     * @param dataHolder a holder for mutable data used in matrix versions of an update rule
     */
    open fun apply(layer: Layer, dataHolder: @UnsafeVariance DM) {}

    /**
     * Override to return an appropriate data holder for a given rule.
     */
    open fun createMatrixData(size: Int): DM {
        return DEFAULT_MATRIX_DATA as DM
    }

    /**
     * Returns a name for this update rule.  Used in combo boxes in the GUI.
     *
     * @return the description.
     */
    abstract override val name: String
    override fun copy(): NeuronUpdateRule<*, *> {
        return deepCopy()
    }

    /**
     * Returns the type of time update (discrete or continuous) associated with this neuron.
     *
     * @return the time type
     */
    abstract val timeType: Network.TimeType

    /**
     * Returns a deep copy of the update rule.
     *
     * @return Duplicated update rule
     */
    abstract fun deepCopy(): NeuronUpdateRule<*, *>

    /**
     * Increment a neuron by increment.
     *
     * @param n neuron
     */
    fun incrementActivation(n: Neuron) {
        n.forceSetActivation(n.activation + n.getIncrement())
    }

    /**
     * Decrement a neuron by increment.
     *
     * @param n neuron
     */
    fun decrementActivation(n: Neuron) {
        n.forceSetActivation(n.activation - n.getIncrement())
    }

    /**
     * Increment a neuron by increment, respecting neuron specific constraints. Intended to be overriden.
     *
     * @param n neuron to be incremented
     */
    open fun contextualIncrement(n: Neuron) {
        incrementActivation(n)
        n.clip()
    }

    /**
     * Decrement a neuron by increment, respecting neuron specific constraints. Intended to be overriden.
     *
     * @param n neuron
     */
    open fun contextualDecrement(n: Neuron) {
        decrementActivation(n)
        n.clip()
    }

    open val randomValue: Double
        /**
         * Returns a random value between the upper and lower bounds of this neuron. Update rules that require special
         * randomization should override this method.
         *
         * @return the random value.
         */
        get() = if (this is BoundedUpdateRule) {
            ((this as BoundedUpdateRule).upperBound - (this as BoundedUpdateRule).lowerBound) * Math.random() + (this as BoundedUpdateRule).lowerBound
        } else {
            2 * Math.random() - 1
        }
    open val graphicalLowerBound: Double
        /**
         * Returns a value for lower bound to be used in computing the saturation of neuron nodes. Override this to produce
         * nicer graphics, and fine tune based on display of neurons in common use cases for a given neuron type.
         *
         * @return the graphical lower bound
         */
        get() = if (this is BoundedUpdateRule) {
            (this as BoundedUpdateRule).lowerBound
        } else {
            (-1).toDouble()
        }
    open val graphicalUpperBound: Double
        /**
         * Returns a value for upper bound to be used in computing the saturation of neuron nodes. Override this to produce
         * nicer graphics, and fine tune based on display of neurons in common use cases for a given neuron type.
         *
         * @return the graphical upper bound
         */
        get() = if (this is BoundedUpdateRule) {
            (this as BoundedUpdateRule).upperBound
        } else {
            1.0
        }

    /**
     * Set activation to 0; override for other "clearing" behavior (e.g. setting other variables to 0. Called in Gui
     * when "clear" button pressed.
     *
     * @param neuron reference to parent neuron
     */
    open fun clear(neuron: Neuron) {
        neuron.forceSetActivation(0.0)
    }

    /**
     * Returns string for tool tip or short description. Override to provide custom information.
     *
     * @param neuron reference to parent neuron
     * @return tool tip text
     */
    open fun getToolTipText(neuron: Neuron): String? {
        return neuron.id + ".  Location: (" + neuron.x.toInt() + "," + neuron.y.toInt() + "). Activation: " + Utils.round(
            neuron.activation,
            MAX_DIGITS
        )
    }

    open val isSpikingRule: Boolean
        get() = false

    open fun getGraphicalValue(n: Neuron): Double {
        return n.activation
    }

    fun getNeuronArrayTypeMap() = neuronArrayUpdateRules

    override fun getTypeList() = allUpdateRules

    companion object {

        /**
         * Rules for drop-down list used by [org.simbrain.util.propertyeditor.ObjectTypeEditor] to set the update rule
         * on a neuron.
         */
        var types = allUpdateRules

        /**
         * The maximum number of digits to display in the tool tip.
         */
        private const val MAX_DIGITS = 9

        /**
         * Default data holder for scalar data.
         */
        private val DEFAULT_SCALAR_DATA: ScalarDataHolder = EmptyScalarData

        /**
         * Default data holder for matrix data.
         */
        private val DEFAULT_MATRIX_DATA: MatrixDataHolder = EmptyMatrixData

        /**
         * Rules in this list use a custom zero point.
         * Currently this is set to be between upper and lower bounds.  In the future more
         * options could be added and this could become an enum.
         *
         *
         * Add an update type to this list if a neuron should use a custom zero point
         */
        private val usesCustomZeroPoint = HashSet<Class<*>>(
            Arrays.asList(
                IntegrateAndFireRule::class.java, AdExIFRule::class.java, IzhikevichRule::class.java
            )
        )

        /**
         * Checks if the provided rule uses a custom zero point or not.
         */
        fun usesCustomZeroPoint(rule: NeuronUpdateRule<*, *>): Boolean {
            return usesCustomZeroPoint.contains(rule.javaClass)
        }
    }
}

val allUpdateRules = listOf(
    AdditiveRule::class.java,
    AdExIFRule::class.java,
    AllostaticUpdateRule::class.java,
    BinaryRule::class.java,
    ContinuousSigmoidalRule::class.java,
    DecayRule::class.java,
    FitzhughNagumo::class.java,
    IACRule::class.java,
    IntegrateAndFireRule::class.java,
    IzhikevichRule::class.java,
    KuramotoRule::class.java,
    LinearRule::class.java,
    LogisticRule::class.java,
    MorrisLecarRule::class.java,
    NakaRushtonRule::class.java,
    ProductRule::class.java,
    RandomNeuronRule::class.java,
    SigmoidalRule::class.java,
    SinusoidalRule::class.java,
    SpikingThresholdRule::class.java,
    StochasticRule::class.java,
    ThreeValueRule::class.java,
    TimedAccumulatorRule::class.java
)

val neuronArrayUpdateRules = listOf(
    AdExIFRule::class.java,
    BinaryRule::class.java,
    DecayRule::class.java,
    FitzhughNagumo::class.java,
    LinearRule::class.java,
    NakaRushtonRule::class.java,
    SigmoidalRule::class.java,
    SpikingNeuronUpdateRule::class.java
)