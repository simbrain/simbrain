/**
 * Rate-coded Leabra point neuron with feedforward-feedback (FFFB) layer inhibition, for neuron arrays. Follows
 * emer/leabra (https://github.com/emer/leabra) in simplified form: the noisy XX1 function is replaced by plain XX1
 * and activation is computed from excitatory conductance relative to threshold. The layer's inputs are treated as
 * raw excitatory conductance. [org.simbrain.network.trainers.LeabraTrainer] supplies these, scaled by projection.
 */
package org.simbrain.network.updaterules

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.APETabOder
import org.simbrain.util.propertyeditor.HiddenTypeOption
import kotlin.math.max

@HiddenTypeOption
@APETabOder("Main", "Conductances", "Inhibition")
class LeabraRule : NeuronUpdateRule<EmptyScalarData, LeabraMatrixData>() {

    @UserParameter(label = "Threshold", description = "Membrane potential threshold for activation.", increment = .05, order = 10)
    var threshold = 0.5

    @UserParameter(label = "Gain", description = "Gain of the XX1 activation function.", minimumValue = 0.0, order = 20)
    var gain = 100.0

    @UserParameter(label = "Membrane time constant", description = "Integration time constant, in cycles, for membrane potential and activation.", minimumValue = 1.0, order = 30)
    var vmTau = 3.3

    @UserParameter(label = "Conductance time constant", description = "Integration time constant, in cycles, for excitatory conductance.", minimumValue = 1.0, order = 40)
    var geTau = 1.4

    @UserParameter(label = "Initial membrane potential", description = "Membrane potential after a reset.", order = 50)
    var initVm = 0.4

    @UserParameter(label = "Excitatory max conductance", description = "Scales excitatory conductance.", order = 10, tab = "Conductances")
    var gbarE = 1.0

    @UserParameter(label = "Leak conductance", description = "Constant leak conductance.", order = 20, tab = "Conductances")
    var gbarL = 0.1

    @UserParameter(label = "Inhibitory max conductance", description = "Scales inhibitory conductance.", order = 30, tab = "Conductances")
    var gbarI = 1.0

    @UserParameter(label = "Excitatory reversal", description = "Equilibrium potential of excitatory channels.", order = 40, tab = "Conductances")
    var erevE = 1.0

    @UserParameter(label = "Leak reversal", description = "Equilibrium potential of leak channels.", order = 50, tab = "Conductances")
    var erevL = 0.3

    @UserParameter(label = "Inhibitory reversal", description = "Equilibrium potential of inhibitory channels.", order = 60, tab = "Conductances")
    var erevI = 0.25

    @UserParameter(label = "Inhibition gain", description = "Overall strength of layer inhibition. Higher values give sparser activity.", minimumValue = 0.0, increment = .1, order = 10, tab = "Inhibition")
    var inhibitionGain = 1.8

    @UserParameter(label = "Feedforward", description = "Weight of feedforward inhibition, driven by average excitatory conductance.", minimumValue = 0.0, increment = .1, order = 20, tab = "Inhibition")
    var feedforward = 1.0

    @UserParameter(label = "Feedforward offset", description = "Average conductance below which there is no feedforward inhibition.", minimumValue = 0.0, increment = .01, order = 30, tab = "Inhibition")
    var feedforwardOffset = 0.1

    @UserParameter(label = "Feedback", description = "Weight of feedback inhibition, driven by average activation.", minimumValue = 0.0, increment = .1, order = 40, tab = "Inhibition")
    var feedback = 1.0

    @UserParameter(label = "Feedback time constant", description = "Integration time constant, in cycles, for feedback inhibition.", minimumValue = 1.0, order = 50, tab = "Inhibition")
    var feedbackTau = 1.4

    context(Network)
    override fun apply(layer: Layer, dataHolder: LeabraMatrixData) {
        val geRaw = layer.inputs
        val acts = layer.activations
        val n = layer.size
        val ge = dataHolder.ge
        val vm = dataHolder.vm

        for (i in 0 until n) {
            ge[i] += (geRaw[i, 0] - ge[i]) / geTau
        }

        val avgGe = ge.average()
        val avgAct = (0 until n).sumOf { acts[it, 0] } / n
        val feedforwardInhibition = feedforward * max(avgGe - feedforwardOffset, 0.0)
        dataHolder.feedbackInhibition += (feedback * avgAct - dataHolder.feedbackInhibition) / feedbackTau
        val gi = inhibitionGain * (feedforwardInhibition + dataHolder.feedbackInhibition)
        dataHolder.gi = gi

        // Excitatory conductance at which the membrane sits exactly at threshold, given current inhibition and leak
        val geThreshold = (gbarI * gi * (erevI - threshold) + gbarL * (erevL - threshold)) / (threshold - erevE)

        for (i in 0 until n) {
            val geTotal = gbarE * ge[i]
            val netCurrent = geTotal * (erevE - vm[i]) + gbarL * (erevL - vm[i]) + gbarI * gi * (erevI - vm[i])
            vm[i] = (vm[i] + netCurrent / vmTau).coerceIn(0.0, 2.0)
            val x = gain * (geTotal - geThreshold)
            val target = if (x > 0) x / (x + 1) else 0.0
            acts[i, 0] = acts[i, 0] + (target - acts[i, 0]) / vmTau
        }
    }

    /**
     * Return the layer to its resting state.
     */
    fun reset(layer: Layer, dataHolder: LeabraMatrixData) {
        dataHolder.initVm = initVm
        dataHolder.clear()
        layer.activations.fill(0.0)
    }

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        throw UnsupportedOperationException("LeabraRule uses layer-level inhibition and only supports neuron arrays")
    }

    override fun createMatrixData(size: Int) = LeabraMatrixData(size, initVm)

    override val timeType = Network.TimeType.DISCRETE

    override val graphicalLowerBound get() = 0.0

    override val graphicalUpperBound get() = 1.0

    override val name = "Leabra"

    override fun copy() = LeabraRule().also {
        it.threshold = threshold
        it.gain = gain
        it.vmTau = vmTau
        it.geTau = geTau
        it.initVm = initVm
        it.gbarE = gbarE
        it.gbarL = gbarL
        it.gbarI = gbarI
        it.erevE = erevE
        it.erevL = erevL
        it.erevI = erevI
        it.inhibitionGain = inhibitionGain
        it.feedforward = feedforward
        it.feedforwardOffset = feedforwardOffset
        it.feedback = feedback
        it.feedbackTau = feedbackTau
    }
}

/**
 * Per-layer state for [LeabraRule]. [initVm] is the membrane potential [clear] returns to; [LeabraRule.reset] keeps it
 * in step with the rule's current setting.
 */
class LeabraMatrixData(val size: Int, var initVm: Double = 0.4) : MatrixDataHolder {

    @UserParameter(label = "Excitatory conductance", description = "Time-integrated excitatory conductance of each neuron.")
    var ge = DoubleArray(size)

    @UserParameter(label = "Membrane potential", description = "Membrane potential of each neuron.")
    var vm = DoubleArray(size) { initVm }

    @UserParameter(label = "Feedback inhibition", description = "Running feedback component of layer inhibition.")
    var feedbackInhibition = 0.0

    @UserParameter(label = "Inhibitory conductance", description = "Layer inhibitory conductance on the last cycle.")
    var gi = 0.0

    override fun copy() = LeabraMatrixData(size, initVm).also {
        ge.copyInto(it.ge)
        vm.copyInto(it.vm)
        it.feedbackInhibition = feedbackInhibition
        it.gi = gi
    }

    override fun clear() {
        ge.fill(0.0)
        vm.fill(initVm)
        feedbackInhibition = 0.0
        gi = 0.0
    }
}
