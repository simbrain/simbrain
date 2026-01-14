package org.simbrain.network.connections

import org.simbrain.network.core.Synapse
import org.simbrain.util.SimbrainConstants.Polarity
import kotlin.random.Random

/**
 * Utility functions/interfaces/etc for manipulating synapses.
 * Usually, for manipulating loose synapses since most changes to Synapses in a
 * synapse group should be done through the synapse group, but there are
 * counter-examples.
 *
 * @author Zoë Tosi
 */

const val DEFAULT_EXCITATORY_STRENGTH = 1.0

const val DEFAULT_INHIBITORY_STRENGTH = -1.0

/**
 * Holds the result of splitting synapses by polarity.
 */
data class PolarizedSynapseCollection(
    val excitatory: List<Synapse>,
    val inhibitory: List<Synapse>
)

/**
 * Splits the provided synapses into excitatory and inhibitory lists based on [percentExcitatory].
 *
 * This method will attempt to maintain the requested percentage even
 * if some or all of the source neurons are themselves polarized. In such
 * cases the polarity of the Neurons' outgoing synapses will not be
 * overridden. Though it may not be possible to obtain the desired
 * percentage in this case, this method will get as close as possible.
 *
 * @param synapses the synapses to split
 * @param percentExcitatory the percent of the synapses to make excitatory
 * @return a [PolarizedSynapseCollection] containing the excitatory and inhibitory synapse lists
 */
@JvmOverloads
fun splitSynapsesByPolarity(synapses: Collection<Synapse>, percentExcitatory: Double, random: Random = Random): PolarizedSynapseCollection {
    val excitatoryRatio = percentExcitatory / 100
    if (excitatoryRatio > 1 || excitatoryRatio < 0) {
        throw IllegalArgumentException("Randomization had failed. The ratio of excitatory synapses cannot be greater than 1 or less than 0.")
    }

    val synapsesByPolarity = synapses.groupBy { it.source.polarity }
    val excitatory = synapsesByPolarity[Polarity.EXCITATORY] ?: emptyList()
    val inhibitory = synapsesByPolarity[Polarity.INHIBITORY] ?: emptyList()
    val both = synapsesByPolarity[Polarity.BOTH] ?: emptyList()

    if (both.isEmpty()) {
        return PolarizedSynapseCollection(excitatory, inhibitory)
    }

    val excitatoryNeed = (synapses.size * excitatoryRatio).toInt() - excitatory.size
    val inhibitoryNeed = (synapses.size * (1 - excitatoryRatio)).toInt() - inhibitory.size

    if (excitatoryNeed < 0 || inhibitoryNeed < 0) {
        throw IllegalArgumentException("""
            Insufficient free synapses to meet the requested excitatory ratio.
            Existing excitatory synapses: ${excitatory.size}
            Existing inhibitory synapses: ${inhibitory.size}
            Existing both synapses: ${both.size}
            Requested excitatory size: $excitatoryNeed
            Requested inhibitory size: $inhibitoryNeed
        """.trimIndent())
    }

    val shuffled = both.shuffled(random)
    return PolarizedSynapseCollection(
        excitatory = shuffled.take(excitatoryNeed) + excitatory,
        inhibitory = shuffled.drop(excitatoryNeed) + inhibitory
    )
}

/**
 * Changes the strengths of the provided synapses so that [percentExcitatory] of them
 * are excitatory.
 *
 * This method will attempt to maintain the requested percentage even
 * if some or all of the source neurons are themselves polarized. In such
 * cases the polarity of the Neurons' outgoing synapses will not be
 * overridden. Though it may not be possible to obtain the desired
 * percentage in this case, this method will get as close as possible.
 *
 * @param synapses the synapses to polarize
 * @param percentExcitatory the percent of the synapses to make excitatory
 */
@JvmOverloads
fun polarizeSynapses(synapses: Collection<Synapse>, percentExcitatory: Double, random: Random = Random) {
    val (excitatory, inhibitory) = splitSynapsesByPolarity(synapses, percentExcitatory, random)
    excitatory.forEach { it.strength = DEFAULT_EXCITATORY_STRENGTH }
    inhibitory.forEach { it.strength = DEFAULT_INHIBITORY_STRENGTH }
}
