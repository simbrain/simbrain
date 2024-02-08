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

import org.simbrain.network.NetworkModel
import org.simbrain.network.events.SynapseEvents
import org.simbrain.network.learningrules.StaticSynapseRule
import org.simbrain.network.learningrules.SynapseUpdateRule
import org.simbrain.network.spikeresponders.NonResponder
import org.simbrain.network.spikeresponders.SpikeResponder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.UserParameter
import org.simbrain.util.Utils
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import java.util.*

/**
 * Model synapses or weights.
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
class Synapse : NetworkModel, EditableObject, AttributeContainer {

    private var _strength = 1.0

    /**
     * Strength of synapse.
     */
    @UserParameter(
        label = "Strength",
        description = "Weight Strength. If you want a value greater" +
                "than upper bound or less than lower bound you must set those first, and close this dialog.",
        probDist = "Normal",
        probParam1 = .1,
        probParam2 = .5,
        useLegacySetter = true,
        order = 1
    )
    @get:Producible(defaultVisibility = false)
    @set:Consumable(defaultVisibility = false)
    var strength: Double
        get() = _strength
        set(wt) {
            if (!frozen) {
                _strength = clip(source.polarity.clip(wt))
            }
            events.strengthUpdated.fireAndBlock()
        }

    override val name: String
        get() = id!!

    /**
     *  Source neuron to which the synapse is attached.
     */
    var source: Neuron

    /**
     * Target neuron to which the synapse is attached.
     */
    var target: Neuron

    /**
     * Whether this synapse should be visible in the GUI.
     */
    var isVisible: Boolean = true
        set(newVisibility) {
            events.visbilityChanged.fireAndForget(field, newVisibility)
            field = newVisibility
        }

    /**
     * The update method of this synapse, which corresponds to what kind of synapse it is.
     */
    @UserParameter(label = "Learning Rule", order = 100)
    var learningRule = DEFAULT_LEARNING_RULE
        set(newLearningRule) {
            val oldRule = learningRule
            field = newLearningRule.deepCopy()
            // TODO: Needed for calls to SynapseGroup.postOpenInit, which calls
            // SynapseGroup.setAndComformToTemplate. Template synapses don't seem to have
            // change support initialized.
            events.learningRuleUpdated.fireAndForget(oldRule, learningRule)
        }

    /**
     * Only used if source neuron is a spiking neuron.
     */
    @UserParameter(label = "Spike Responder", showDetails = false, order = 200)
    var spikeResponder = DEFAULT_SPIKE_RESPONDER
        set(newResponder) {
            field = newResponder
            spikeResponderData = newResponder.createResponderData()
        }

    /**
     * Post synaptic response. The totality of the output of this synapse; the total contribution of this synapse to the
     * post-synaptic or target neuron. This is computed using a [SpikeResponder] in the case of a spiking
     * pre-synaptic neuron. In the case of a non-spiking node this is the product of the source activation and the
     * weight of a synapse, i.e. one term in a classical weighted input.
     */
    var psr: Double = 0.0

    /**
     * Amount to increment the neuron.
     */
    @UserParameter(label = "Increment", description = "Strength Increment", minimumValue = 0.0, order = 2)
    var increment: Double = 1.0

    /**
     * Upper limit of synapse.
     */
    @UserParameter(label = "Upper bound", description = "Upper bound", minimumValue = 0.0, order = 3)
    var upperBound = DEFAULT_UPPER_BOUND
        set(value) {
            field = value
            events.strengthUpdated.fireAndForget() // to force a graphics update
        }

    /**
     * Lower limit of synapse.
     */
    @UserParameter(label = "Lower bound", description = "Lower bound", maximumValue = 0.0, order = 4)
    var lowerBound = DEFAULT_LOWER_BOUND
        set(value) {
            field = value
            events.strengthUpdated.fireAndForget() // to force a graphics update
        }

    /**
     * Time to delay sending activation to target neuron.
     */
    @UserParameter(label = "Delay", description = "delay", minimumValue = 0.0, order = 5)
    var delay = 0
        set(dly) {
            if (dly < 0 && source != null) {
                return
            }
            field = dly

            if (delay <= 0) {
                delayManager = null

                return
            }

            delayManager = DoubleArray(delay)

            for (i in 0 until delay) {
                delayManager!![i] = 0.0
            }
            dlyPtr = 0
        }

    /**
     * Indicates whether this type of synapse participates in the computation of weighted input.
     */
    @UserParameter(
        label = "Enabled",
        description = "Synapse is enabled. If disabled, it won't pass activation through",
        order = 6
    )
    var isEnabled: Boolean = true

    /**
     * Whether or not this synapse's strength can be changed by any means other than direct
     * user intervention.
     */
    @UserParameter(label = "Frozen", description = "Synapse is frozen (no learning) or not", order = 6)
    var frozen = false
        set(value) {
            field = value
            events.clampChanged.fireAndForget()
        }

    /**
     * Manages synaptic delay
     */
    private var delayManager: DoubleArray? = null

    /**
     * Points to the location in the delay manager that corresponds to the current time.
     */
    private var dlyPtr = 0

    /**
     * The value [.dlyPtr] points to in the delay manager.
     */
    private var dlyVal = 0.0

    /**
     * Data holder for synapse
     */
    @UserParameter(label = "Learning data", order = 100)
    var dataHolder: ScalarDataHolder = learningRule.createScalarData()

    /**
     * Data holder for spiker responder.
     */
    @UserParameter(label = "Spike data", order = 110)
    private var spikeResponderData = spikeResponder.createResponderData()

    /**
     * Support for property change events.
     */
    @Transient
    override val events: SynapseEvents = SynapseEvents()

    /**
     * Construct a synapse using a source and target neuron, defaulting to ClampedSynapse and assuming the parent of the
     * source neuron is the parent of this synapse.
     *
     * @param source source neuron
     * @param target target neuron
     */
    @XStreamConstructor
    constructor(source: Neuron, target: Neuron) {
        this.source = source
        this.target = target
        if (shouldAdd()) {
            source.addToFanOut(this)
            target.addToFanIn(this)
        }
        source.events.locationChanged.on { events.locationChanged.fireAndBlock() }
        target.events.locationChanged.on { events.locationChanged.fireAndBlock() }
    }

    /**
     * Construct a synapse with a specified initial strength.
     *
     * @param source          source neuron
     * @param target          target neuron
     * @param initialStrength initial strength for synapse
     */
    constructor(source: Neuron, target: Neuron, initialStrength: Double) : this(source, target) {
        this.forceSetStrength(initialStrength)
    }

    /**
     * Construct a synapse using a source and target neuron, and a specified learning rule and parent network
     *
     * @param source       source neuron
     * @param target       target neuron
     * @param learningRule update rule for this synapse
     */
    constructor(source: Neuron, target: Neuron, learningRule: SynapseUpdateRule<*, *>): this(source, target) {
        this.learningRule = learningRule
    }

    constructor(
        source: Neuron,
        target: Neuron,
        templateSynapse: Synapse
    ) : this(source, target) {
        learningRule = templateSynapse.learningRule.deepCopy()
        forceSetStrength(templateSynapse.strength)
        upperBound = templateSynapse.upperBound
        lowerBound = templateSynapse.lowerBound
        increment = templateSynapse.increment
        spikeResponder = templateSynapse.spikeResponder
        isEnabled = templateSynapse.isEnabled
        delay = templateSynapse.delay
        frozen = templateSynapse.frozen
    }

    /**
     * Update this synapse using its current learning rule.
     */
    context(Network)
    override fun update() {
        if (frozen) return

        // Update synapse strengths for non-static synapses
        if (learningRule !is StaticSynapseRule) {
            learningRule.apply(this, dataHolder)
        }
    }

    /**
     * Update output of this synapse. If there is no spike responder then "psr" is just weighted input
     * ("connectionist style"). If there is a [SpikeResponder] it is applied to update the post-synaptic response
     * (psr) and psr is used as the output.
     */
    context(Network)
    fun updateOutput() {
        if (!isEnabled) {
            return
        }

        // Update the output of this synapse
        if (spikeResponder is NonResponder) {
            // For "connectionist" case
            psr = source.activation * _strength
        } else {
            // Updates psr for spiking source neurons
            spikeResponder.apply(this, spikeResponderData)
        }

        // Handle delays
        if (delay != 0) {
            dlyVal = dequeue()
            enqueue(psr)
            psr = dlyVal
        }
    }

    val type: String
        /**
         * The name of the learning rule of the synapse; it's "type". Used via reflection for consistency checking in the
         * gui. (Open multiple synapses and if they are of the different types the dialog is different).
         *
         * @return the name of the class of this network.
         */
        get() = learningRule.javaClass.simpleName

    fun forceSetStrength(wt: Double) {
        _strength = wt
        events.strengthUpdated.fireAndBlock()
    }

    /**
     * Increment this weight by increment.
     */
    override fun increment() {
        if (_strength < upperBound) {
            forceSetStrength(_strength + increment)
        }
    }

    /**
     * Decrement this weight by increment.
     */
    override fun decrement() {
        if (_strength > lowerBound) {
            forceSetStrength(_strength - increment)
            _strength -= increment
        }
    }

    /**
     * Increase the absolute value of this weight by increment amount.
     */
    fun reinforce() {
        if (_strength > 0) {
            increment()
        } else if (_strength < 0) {
            decrement()
        } else {
            forceSetStrength(0.0)
        }
    }

    /**
     * Decrease the absolute value of this weight by increment amount.
     */
    fun weaken() {
        if (strength > 0) {
            decrement()
        } else if (strength < 0) {
            increment()
        } else if (strength == 0.0) {
            forceSetStrength(0.0)
        }
    }

    val toolTipText: String
        /**
         * Returns string for tool tip or short description.
         *
         * @return tool tip text
         */
        get() = "Strength: " + Utils.round(this.strength, MAX_DIGITS)

    val symmetricSynapse: Synapse?
        /**
         * Returns symmetric synapse if there is one, null otherwise.
         *
         * @return the symmetric synapse, if any.
         */
        get() = target.fanOut[source]

    context(Network)
    override fun randomize() {
        when (source.polarity) {
            Polarity.EXCITATORY -> forceSetStrength(excitatoryRandomizer.sampleDouble())
            Polarity.INHIBITORY -> forceSetStrength(inhibitoryRandomizer.sampleDouble())
            else -> forceSetStrength(weightRandomizer.sampleDouble())
        }
    }

    /**
     * If weight value is above or below its bounds set it to those bounds.
     */
    fun checkBounds() {
        if (_strength > upperBound) {
            _strength = upperBound
        }

        if (_strength < lowerBound) {
            _strength = lowerBound
        }
    }

    /**
     * Utility function for use in learning rules. If value is above or below the bounds of this synapse set it to those
     * bounds.
     *
     * @param value Value to be checked
     * @return Evaluated value
     */
    fun clip(value: Double): Double {
        var `val` = value
        if (`val` > upperBound) {
            `val` = upperBound
        } else {
            if (`val` < lowerBound) {
                `val` = lowerBound
            }
        }
        return `val`
    }

    /**
     * @return the deque.
     */
    private fun dequeue(): Double {
        if (dlyPtr == delay) {
            dlyPtr = 0
        }
        return delayManager!![dlyPtr++]
    }

    /**
     * Enqueeu.
     *
     * @param val Value to enqueu
     */
    private fun enqueue(`val`: Double) {
        if (dlyPtr == 0) {
            delayManager!![delay - 1] = `val`
        } else {
            delayManager!![dlyPtr - 1] = `val`
        }
    }

    override fun toString(): String {
        return ("$id: Strength = ${SimbrainMath.roundDouble(strength, 3)} Connects ${source.id} to ${target.id}")
    }

    /**
     * Decay this synapse by the indicated percentage. E.g. .5 cuts the strength in half.
     *
     * @param decayPercent decay percent
     */
    fun decay(decayPercent: Double) {
        val decayAmount = decayPercent * strength
        strength -= decayAmount
    }

    override fun postOpenInit() {
        target.addToFanIn(this)
        source.addToFanOut(this)
    }

    //  TODO: Without any indication in the GUI this might be unclear to users.
    /**
     * "Clear" the synapse in the sense of setting post synaptic result to 0 and removing all queued activations from
     * the delay manager. Do NOT set strength to 0, which his a more radical move, that should not be achieved with
     * the same GUI actions as the high level "clear".
     */
    override fun clear() {
        psr = 0.0
        if (delayManager != null) {
            Arrays.fill(delayManager, 0.0)
        }
    }

    override fun toggleClamping() {
        frozen = !frozen
    }

    val length: Double
        /**
         * Returns the length in pixels of the "axon" this synapse is at the end of.
         */
        get() = SimbrainMath.distance(source.location, target.location)

    override fun delete() {
        // Remove references to this synapse from parent neurons
        source.removeFromFanOut(this)
        target.removeFromFanIn(this)
        events.deleted.fireAndBlock(this)
    }

    fun hardClear() {
        clear()
        _strength = 0.0
    }

    override fun shouldAdd(): Boolean {
        return !this.overlapsExistingSynapse()
    }

    companion object {
        /**
         * A default update rule for the synapse.
         */
        private val DEFAULT_LEARNING_RULE: SynapseUpdateRule<*, *> = StaticSynapseRule()

        /**
         * A default spike responder.
         */
        val DEFAULT_SPIKE_RESPONDER: SpikeResponder = NonResponder()

        /**
         * Default upper bound.
         */
        var DEFAULT_UPPER_BOUND: Double = 100.0

        /**
         * Default lower bound.
         */
        var DEFAULT_LOWER_BOUND: Double = -100.0

        // TODO: Conditionally enable based on type of source neuron?
        /**
         * The maximum number of digits to display in the tool tip.
         */
        private const val MAX_DIGITS = 2

    }
}
