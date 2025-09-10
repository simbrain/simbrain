package org.simbrain.network.connections

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.bound
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import kotlin.random.Random

/**
 * Connect each source neuron to a single target.
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
class OneToOne(

    /**
     * If true, synapses are added in both directions.
     */
    @UserParameter(label = "Bi-directional", description = "If true, synapses are added in both directions.", order = 2)
    var useBidirectionalConnections: Boolean = false,

    seed: Long = Random.nextLong()

) : ConnectionStrategy(seed), EditableObject {

    override fun connectNeurons(
        source: List<Neuron>,
        target: List<Neuron>
    ): List<Synapse> {
        val syns = createOneToOneSynapses(source, target, useBidirectionalConnections)
        polarizeSynapses(syns, percentExcitatory, random)
        return syns
    }

    override val name = "One to one"

    override fun toString(): String {
        return name
    }

    override fun copy(): OneToOne {
        return OneToOne(useBidirectionalConnections).also {
            commonCopy(it)
        }
    }

}

/**
 * Connect neurons 1-1
 */
fun createOneToOneSynapses(
    sourceNeurons: List<Neuron>,
    targetNeurons: List<Neuron>,
    useBidirectionalConnections: Boolean = false
): List<Synapse> {
    val sourceBounds = sourceNeurons.bound
    val targetBounds = targetNeurons.bound
    val sourceCenter = sourceBounds.center
    val targetCenter = targetBounds.center
    val (_, _, sw, sh) = sourceBounds
    val (_, _, tw, th) = targetBounds

    val isSourceVertical = sw < sh
    val isTargetVertical = tw < th

    val isReversedTarget = isSourceVertical != isTargetVertical &&
            (targetCenter - sourceCenter).let { (x, y) -> (x > 0 && y > 0) || (x < 0 && y < 0) }

    return sourceNeurons.sortedBy { if (isSourceVertical) it.y else it.x }
        .zip(targetNeurons.sortedBy { if (isTargetVertical) it.y else it.x }.let { if (isReversedTarget) it.reversed() else it })
        .flatMap { (source, target) ->
            buildList {
                add(Synapse(source, target))
                if (useBidirectionalConnections) {
                    add(Synapse(target, source))
                }
            }
        }

}