package org.simbrain.network.connections

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.getEuclideanDist
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.cartesianProduct
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.decayfunctions.GaussianDecayFunction
import org.simbrain.util.decayfunctions.StepDecayFunction
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import kotlin.random.Random

/**
 * Distance-based connection strategy that creates synapses probabilistically based on
 * the distance between neurons, using configurable decay functions.
 *
 * Supports two modes:
 * - **Standard mode**: Uses a single decay function for all connections
 * - **Polarity mode**: Uses separate decay functions for each polarity combination
 *   (Exc→Exc, Exc→Inh, Inh→Exc, Inh→Inh, and non-polar neurons)
 *
 * This strategy consolidates the functionality of the deprecated [RadialGaussian] and
 * [RadialProbabilistic] strategies. Use [GaussianDecayFunction] for RadialGaussian behavior,
 * or [org.simbrain.util.decayfunctions.StepDecayFunction] for RadialProbabilistic behavior.
 */
class DistanceBased(

    usePolarityMode: Boolean = false,

    decayFunction: DecayFunction = GaussianDecayFunction(),

    eeDecayFunction: DecayFunction = GaussianDecayFunction(),
    eiDecayFunction: DecayFunction = GaussianDecayFunction(),
    ieDecayFunction: DecayFunction = GaussianDecayFunction(),
    iiDecayFunction: DecayFunction = GaussianDecayFunction(),
    npDecayFunction: DecayFunction = GaussianDecayFunction(),

    allowSelfConnections: Boolean = false,

    seed: Long = Random.nextLong()

) : ConnectionStrategy(seed), EditableObject {

    /**
     * When enabled, uses separate decay functions for each polarity combination
     * (EE, EI, IE, II, and non-polar). When disabled, uses a single decay function
     * for all connections.
     */
    var usePolarityMode by GuiEditable(
        initValue = usePolarityMode,
        label = "Use Polarity Mode",
        description = "Enable separate decay functions for each polarity combination (EE, EI, IE, II)",
        order = 1
    )

    /**
     * Decay function used for all connections when not in polarity mode.
     */
    var decayFunction by GuiEditable(
        initValue = decayFunction,
        label = "Decay Function",
        description = "Decay function for connection probability based on distance",
        conditionallyHiddenBy = DistanceBased::usePolarityMode,
        showDetails = false,
        order = 10
    )

    /**
     * Decay function for Excitatory → Excitatory connections.
     */
    var eeDecayFunction by GuiEditable(
        initValue = eeDecayFunction,
        label = "Exc \u2192 Exc",
        description = "Decay function for excitatory to excitatory connections",
        conditionallyVisibleBy = DistanceBased::usePolarityMode,
        showDetails = false,
        order = 20
    )

    /**
     * Decay function for Excitatory → Inhibitory connections.
     */
    var eiDecayFunction by GuiEditable(
        initValue = eiDecayFunction,
        label = "Exc \u2192 Inh",
        description = "Decay function for excitatory to inhibitory connections",
        conditionallyVisibleBy = DistanceBased::usePolarityMode,
        showDetails = false,
        order = 30
    )

    /**
     * Decay function for Inhibitory → Excitatory connections.
     */
    var ieDecayFunction by GuiEditable(
        initValue = ieDecayFunction,
        label = "Inh \u2192 Exc",
        description = "Decay function for inhibitory to excitatory connections",
        conditionallyVisibleBy = DistanceBased::usePolarityMode,
        showDetails = false,
        order = 40
    )

    /**
     * Decay function for Inhibitory → Inhibitory connections.
     */
    var iiDecayFunction by GuiEditable(
        initValue = iiDecayFunction,
        label = "Inh \u2192 Inh",
        description = "Decay function for inhibitory to inhibitory connections",
        conditionallyVisibleBy = DistanceBased::usePolarityMode,
        showDetails = false,
        order = 50
    )

    /**
     * Decay function for connections involving non-polar neurons (polarity = BOTH).
     */
    var npDecayFunction by GuiEditable(
        initValue = npDecayFunction,
        label = "Non-polar",
        description = "Decay function for connections involving non-polar neurons",
        conditionallyVisibleBy = DistanceBased::usePolarityMode,
        showDetails = false,
        order = 60
    )

    /**
     * Whether to allow synapses from a neuron to itself.
     */
    var allowSelfConnections by GuiEditable(
        initValue = allowSelfConnections,
        label = "Allow Self Connections",
        description = "Allow synapses from neurons to themselves",
        order = 100
    )

    override fun connectNeurons(
        source: List<Neuron>,
        target: List<Neuron>
    ): List<Synapse> {
        val syns = if (usePolarityMode) {
            createPolaritySynapses(
                source, target,
                eeDecayFunction, eiDecayFunction, ieDecayFunction, iiDecayFunction, npDecayFunction,
                allowSelfConnections, random
            )
        } else {
            createRadialSynapses(source, target, decayFunction, allowSelfConnections, random)
        }
        val polarized = splitSynapsesByPolarity(syns, percentExcitatory, random)
        weightInitializer.initializeWeights(polarized)
        return syns
    }

    override fun toString(): String {
        return name
    }

    override fun tooltipText(): String {
        return if (usePolarityMode) {
            "Distance Based (Polarity Mode)"
        } else {
            "Distance Based (${decayFunction.name})"
        }
    }

    override fun copy(): DistanceBased {
        return DistanceBased(
            usePolarityMode,
            decayFunction.copy() as DecayFunction,
            eeDecayFunction.copy() as DecayFunction,
            eiDecayFunction.copy() as DecayFunction,
            ieDecayFunction.copy() as DecayFunction,
            iiDecayFunction.copy() as DecayFunction,
            npDecayFunction.copy() as DecayFunction,
            allowSelfConnections
        ).also {
            commonCopy(it)
        }
    }

    override val name = "Distance Based"

}

/**
 * Creates synapses using a single decay function for all connections.
 */
fun createRadialSynapses(
    source: List<Neuron>,
    target: List<Neuron>,
    decay: DecayFunction,
    allowSelfConnections: Boolean = false,
    random: Random = Random
): List<Synapse> {
    val syns = ArrayList<Synapse>()
    (source cartesianProduct target).forEach { (src, tar) ->
        if (allowSelfConnections || src != tar) {
            val p = decay.getScalingFactor(getEuclideanDist(src, tar))
            if (random.nextDouble() < p) {
                syns.add(Synapse(src, tar))
            }
        }
    }
    return syns
}

/**
 * Creates synapses using polarity-specific decay functions.
 * Selects the appropriate decay function based on source and target neuron polarity.
 */
fun createPolaritySynapses(
    source: List<Neuron>,
    target: List<Neuron>,
    eeDecay: DecayFunction,
    eiDecay: DecayFunction,
    ieDecay: DecayFunction,
    iiDecay: DecayFunction,
    npDecay: DecayFunction,
    allowSelfConnections: Boolean = false,
    random: Random = Random
): List<Synapse> {
    val syns = ArrayList<Synapse>()
    (source cartesianProduct target).forEach { (src, tar) ->
        if (allowSelfConnections || src != tar) {
            val decay = getDecayForPolarity(src.polarity, tar.polarity, eeDecay, eiDecay, ieDecay, iiDecay, npDecay)
            val p = decay.getScalingFactor(getEuclideanDist(src, tar))
            if (random.nextDouble() < p) {
                syns.add(Synapse(src, tar))
            }
        }
    }
    return syns
}

/**
 * Returns the appropriate decay function based on source and target polarity.
 */
private fun getDecayForPolarity(
    srcPolarity: Polarity,
    tarPolarity: Polarity,
    eeDecay: DecayFunction,
    eiDecay: DecayFunction,
    ieDecay: DecayFunction,
    iiDecay: DecayFunction,
    npDecay: DecayFunction
): DecayFunction {
    return when (srcPolarity) {
        Polarity.EXCITATORY -> when (tarPolarity) {
            Polarity.EXCITATORY -> eeDecay
            Polarity.INHIBITORY -> eiDecay
            Polarity.BOTH -> npDecay
        }
        Polarity.INHIBITORY -> when (tarPolarity) {
            Polarity.EXCITATORY -> ieDecay
            Polarity.INHIBITORY -> iiDecay
            Polarity.BOTH -> npDecay
        }
        Polarity.BOTH -> npDecay
    }
}

// Adapter functions for migrating from deprecated connection strategies.

// Default constants for polarity-based connection probabilities.
// These were originally defined in RadialGaussian and are preserved for compatibility.

/** Default connection probability multiplier for Excitatory → Excitatory connections */
const val DEFAULT_EE_CONST = 0.2

/** Default connection probability multiplier for Excitatory → Inhibitory connections */
const val DEFAULT_EI_CONST = 0.3

/** Default connection probability multiplier for Inhibitory → Excitatory connections */
const val DEFAULT_IE_CONST = 0.4

/** Default connection probability multiplier for Inhibitory → Inhibitory connections */
const val DEFAULT_II_CONST = 0.1

/**
 * Conversion factor from RadialGaussian's lambda to GaussianDecayFunction's dispersion.
 * RadialGaussian uses: exp(-d²/λ²)
 * GaussianDecayFunction uses: exp(-2d²/dispersion²)
 * Therefore: dispersion = λ * sqrt(2)
 */
const val LAMBDA_TO_DISPERSION = 1.4142135623730951 // sqrt(2)

/**
 * Creates a [DistanceBased] configured to behave like the deprecated [RadialGaussian].
 *
 * Uses polarity mode with Gaussian decay functions, where each polarity combination
 * has its own baseMultiplier matching the old dist constants.
 *
 * @param lambda Distance drop-off parameter (default 200.0). Converted to dispersion internally.
 * @param eeDistConst Connection probability multiplier for Exc→Exc (default 0.2)
 * @param eiDistConst Connection probability multiplier for Exc→Inh (default 0.3)
 * @param ieDistConst Connection probability multiplier for Inh→Exc (default 0.4)
 * @param iiDistConst Connection probability multiplier for Inh→Inh (default 0.1)
 * @param distConst Connection probability multiplier for non-polar neurons (default 0.25)
 * @param seed Random seed for reproducibility
 */
fun radialGaussianStyle(
    lambda: Double = 200.0,
    eeDistConst: Double = 0.2,
    eiDistConst: Double = 0.3,
    ieDistConst: Double = 0.4,
    iiDistConst: Double = 0.1,
    distConst: Double = 0.25,
    seed: Long = Random.nextLong()
): DistanceBased {
    val dispersion = lambda * LAMBDA_TO_DISPERSION

    fun createGaussian(multiplier: Double) = GaussianDecayFunction(dispersion).apply {
        baseMultiplier = multiplier
    }

    return DistanceBased(
        usePolarityMode = true,
        decayFunction = createGaussian(distConst),
        eeDecayFunction = createGaussian(eeDistConst),
        eiDecayFunction = createGaussian(eiDistConst),
        ieDecayFunction = createGaussian(ieDistConst),
        iiDecayFunction = createGaussian(iiDistConst),
        npDecayFunction = createGaussian(distConst),
        allowSelfConnections = false,
        seed = seed
    )
}

/**
 * Creates a [DistanceBased] configured to behave like the deprecated [RadialProbabilistic].
 *
 * Uses a step decay function where connections are made with fixed probability
 * within a given radius.
 *
 * Note: The original RadialProbabilistic supported separate excitatory/inhibitory
 * radii and probabilities. This adapter uses a single radius and probability.
 * For dual-radius behavior, create two DistanceBased instances or use the original class.
 *
 * @param radius Distance within which connections can be made (default 100.0)
 * @param probability Probability of making a connection within the radius (default 0.8)
 * @param allowSelfConnections Whether to allow self-connections (default false)
 * @param seed Random seed for reproducibility
 */
fun radialProbabilisticStyle(
    radius: Double = 100.0,
    probability: Double = 0.8,
    allowSelfConnections: Boolean = false,
    seed: Long = Random.nextLong()
): DistanceBased {
    return DistanceBased(
        usePolarityMode = false,
        decayFunction = StepDecayFunction(radius).apply {
            baseMultiplier = probability
        },
        allowSelfConnections = allowSelfConnections,
        seed = seed
    )
}
