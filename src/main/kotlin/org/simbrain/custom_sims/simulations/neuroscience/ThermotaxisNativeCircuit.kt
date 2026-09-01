/**
 * Builds the fitted thermotaxis steering circuit from native Simbrain components: an
 * [AfdThermoreceptorRule] sensory neuron, [ContinuousSigmoidalRule] interneurons and motor neurons with
 * squash-time output biases, a [SinusoidalRule] CPG, thirteen chemical synapses, and one [GapJunction].
 * Under Simbrain's buffered update this reproduces [ThermotaxisModel] with `bufferedSemantics = true`
 * step for step; see ThermotaxisNetworkParityTest. The CPG's phase constant of one time step compensates
 * for network time incrementing after model updates.
 */
package org.simbrain.custom_sims.simulations.neuroscience

import org.simbrain.network.core.GapJunction
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.updaterules.AfdScalarData
import org.simbrain.network.updaterules.AfdThermoreceptorRule
import org.simbrain.network.updaterules.ContinuousSigmoidalData
import org.simbrain.network.updaterules.ContinuousSigmoidalRule
import org.simbrain.network.updaterules.activity_generators.SinusoidalRule
import kotlin.math.PI
import kotlin.math.exp

internal class ThermotaxisNativeCircuit(
    val network: Network,
    val afd: Neuron,
    val aib: Neuron,
    val aiy: Neuron,
    val aiz: Neuron,
    val dmn: Neuron,
    val vmn: Neuron,
    val cpg: Neuron,
    val gapJunction: GapJunction,
    val synapses: Map<String, Synapse>,
    private val biases: DoubleArray
) {

    val interneurons = listOf(aib, aiy, aiz, dmn, vmn)

    val neurons = listOf(afd, aib, aiy, aiz, dmn, vmn, cpg)

    /**
     * Returns every neuron to its initial condition: cleared state holders, network time zero, and the
     * resting activations the parity semantics assume (sigmoid of each bias at zero state).
     */
    fun reset() {
        neurons.forEach { it.clear() }
        afd.activation = 1.0 / (1.0 + kotlin.math.exp(-AFD_BIAS))
        interneurons.forEachIndexed { index, neuron ->
            neuron.activation = 1.0 / (1.0 + kotlin.math.exp(-biases[index]))
        }
        network.resetTime()
    }

    /**
     * Advances the circuit one 0.1 s step: delivers the temperature sample, updates the network, and
     * reads back the same quantities [ThermotaxisModel.step] reports.
     */
    fun step(temperature: Double): ThermotaxisStep {
        afd.setTemperatureInput(temperature)
        network.update()
        val states = DoubleArray(5) { (interneurons[it].dataHolder as ContinuousSigmoidalData).netActivation }
        val outputs = DoubleArray(5) { interneurons[it].activation }
        return ThermotaxisStep(
            afdState = (afd.dataHolder as AfdScalarData).state,
            states = states,
            outputs = outputs,
            cpgOutput = cpg.activation,
            curvature = NEUROMUSCULAR_WEIGHT * (outputs[3] - outputs[4])
        )
    }

    companion object {

        fun build(
            network: Network = Network(),
            weights: ThermotaxisWeights = ThermotaxisWeights(),
            biases: DoubleArray = fittedBiases
        ): ThermotaxisNativeCircuit {
            network.timeStep = 0.1

            fun sigmoid(value: Double) = 1.0 / (1.0 + exp(-value))

            val afd = Neuron(AfdThermoreceptorRule()).apply {
                label = "AFD"
                activation = sigmoid(0.0 + AFD_BIAS)
            }
            fun interneuron(label: String, bias: Double) = Neuron(
                ContinuousSigmoidalRule().apply {
                    timeConstant = 1.0
                    leakConstant = 1.0
                    slope = 0.25
                    outputBias = bias
                }
            ).apply {
                this.label = label
                activation = sigmoid(0.0 + bias)
            }

            val aib = interneuron("AIB", biases[0])
            val aiy = interneuron("AIY", biases[1])
            val aiz = interneuron("AIZ", biases[2])
            val dmn = interneuron("DMN", biases[3])
            val vmn = interneuron("VMN", biases[4])
            val cpg = Neuron(SinusoidalRule().apply {
                frequency = 2.0 * PI / OSCILLATOR_PERIOD
                phase = 2.0 * PI * 0.1 / OSCILLATOR_PERIOD
            }).apply {
                label = "CPG"
                activation = 0.0
            }
            network.addNetworkModelsAsync(afd, aib, aiy, aiz, dmn, vmn, cpg)

            val gapJunction = GapJunction(afd, aib, weights.afdToAibGap)
            network.addNetworkModelsAsync(gapJunction)

            val synapses = linkedMapOf(
                "afdToAiy" to Synapse(afd, aiy, weights.afdToAiy),
                "aibToAiy" to Synapse(aib, aiy, weights.aibToAiy),
                "aibToDmn" to Synapse(aib, dmn, weights.aibToDmn),
                "aiyToAiz" to Synapse(aiy, aiz, weights.aiyToAiz),
                "aizToAib" to Synapse(aiz, aib, weights.aizToAib),
                "aizToDmn" to Synapse(aiz, dmn, weights.aizToDmn),
                "aizToVmn" to Synapse(aiz, vmn, weights.aizToVmn),
                "dmnToDmn" to Synapse(dmn, dmn, weights.dmnToDmn),
                "dmnToVmn" to Synapse(dmn, vmn, weights.dmnToVmn),
                "vmnToDmn" to Synapse(vmn, dmn, weights.vmnToDmn),
                "vmnToVmn" to Synapse(vmn, vmn, weights.vmnToVmn),
                "cpgToDmn" to Synapse(cpg, dmn, weights.cpgToDmn),
                "cpgToVmn" to Synapse(cpg, vmn, weights.cpgToVmn)
            )
            synapses.values.forEach { network.addNetworkModelsAsync(it) }

            return ThermotaxisNativeCircuit(network, afd, aib, aiy, aiz, dmn, vmn, cpg, gapJunction, synapses, biases)
        }
    }
}
