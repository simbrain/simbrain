package org.simbrain.custom_sims.simulations.sorn

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.SpikingThresholdRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.NormalDistribution

/**
 * An implementation of the specific type of threshold neuron used in Lazar,
 * Pipa, & Triesch (2009).
 *
 * @author Zoë Tosi
 */
class SORNNeuronRule : SpikingThresholdRule(), NoisyUpdateRule {
    /** The noise generating randomizer.  */
    override var noiseGenerator: ProbabilityDistribution = NormalDistribution(0.0, .05)

    /** Whether or not to add noise to the inputs .  */
    override var addNoise: Boolean = false

    /** The target rate.  */
    private var hIP = 0.01

    /** The learning rate for homeostatic plasticity.  */
    @JvmField
    var etaIP: Double = 0.001

    /** The maximum value the threshold is allowed to take on.  */
    @JvmField
    var maxThreshold: Double = 1.0

    @JvmField
    var refractoryPeriod: Double = 0.0

    override fun deepCopy(): SORNNeuronRule {
        val snr = SORNNeuronRule()
        snr.addNoise = addNoise
        snr.noiseGenerator = noiseGenerator
        snr.etaIP = etaIP
        snr.hIP = hIP
        snr.maxThreshold = maxThreshold
        snr.threshold = threshold
        snr.refractoryPeriod = refractoryPeriod
        return snr
    }

    context(Network)
    fun apply(neuron: Neuron, data: EmptyScalarData) {
        // Synaptic Normalization
        SORN.normalizeExcitatoryFanIn(neuron)
        // Sum inputs including noise and applied (external) inputs
        val input = (neuron.input
                + (if (addNoise) noiseGenerator.sampleDouble() else 0.0))
        // TODO: There used to be "applied input here" but it is no longer used
        // Check that we're not still in the refractory period
        val outOfRef: Boolean = (time > neuron.lastSpikeTime + refractoryPeriod)
        // We fire a spike if input exceeds threshold and we're
        // not in the refractory period
        val spk = outOfRef && (input >= threshold)
        neuron.isSpike = spk
        neuron.activation = 2 * (input - threshold)
        plasticUpdate(neuron)
    }

    /**
     * Homeostatic plasticity of the default SORN network. {@inheritDoc}
     */
    fun plasticUpdate(neuron: Neuron) {
        threshold = threshold + (etaIP * ((if (neuron.isSpike) 1 else 0) - hIP))
        //        if (getThreshold() > maxThreshold) {
        //            setThreshold(maxThreshold);
        //        }
    }
}
