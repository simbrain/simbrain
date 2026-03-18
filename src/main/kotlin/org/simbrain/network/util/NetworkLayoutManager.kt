/**
 * A package for laying out neurons and groups with respect to each other. This
 * is not dependent on any GUI classes, because only model neuron position
 * properties are affected by these methods.
 *
 * @author jyoshimi
 * @author ztosi
 */
package org.simbrain.network.util

import org.simbrain.network.core.*
import org.simbrain.util.component1
import org.simbrain.util.component2
import org.simbrain.util.point

/**
 * Directions.
 */
enum class Direction {
    NORTH, SOUTH, EAST, WEST
}

enum class Alignment {
    VERTICAL, HORIZONTAL
}

fun alignNetworkModels(reference: LocatableModel, target: LocatableModel, alignment: Alignment) {
    val (x1, y1) = reference.location
    val (x2, y2) = target.location
    when (alignment) {
        Alignment.VERTICAL -> target.location = point(x1, y2)
        Alignment.HORIZONTAL -> target.location = point(x2, y1)
    }
}

/**
 * Reference stays fixed. Target is moved with respect to reference and is
 * centered with respect to it in the relevant direction.
 *
 * Must be used after all the subgroups have been added.
 *
 * @param reference    the reference group
 * @param target    the group to offset
 * @param direction String indication of absolute direction. Must be one of "North", "South", "East", or "West".
 * @param amount    the amount by which to offset the second group
 */
fun offsetNeuronCollections(
    reference: NeuronCollection,
    target: NeuronCollection,
    direction: Direction,
    amount: Double
) {
    val (refLeft, refTop, refRight, refBottom) = reference.neuronList.run { listOf(minX, minY, maxX, maxY) }
    val (tarLeft, tarTop, tarRight, tarBottom) = target.neuronList.run { listOf(minX, minY, maxX, maxY) }
    when (direction) {
        Direction.NORTH -> target.locationY = refTop - amount - (tarBottom - tarTop) / 2
        Direction.SOUTH -> target.locationY = refBottom + amount + (tarBottom - tarTop) / 2
        Direction.EAST -> target.locationX = refRight + amount + (tarRight - tarLeft) / 2
        Direction.WEST -> target.locationX = refLeft - amount - (tarRight - tarLeft) / 2
    }
}

/**
 * Reference stays fixed. Target is moved with respect to reference and is centered with respect to it in the relevant
 * direction.
 *
 * Must be used after all the subgroups have been added.
 *
 * Gap defaults to the distance between the center of the reference and target objects.
 * If it should be the gap between the borders of the objects heights or widths of reference and target objects must be supplied
 *
 * @param reference  the reference model
 * @param target     the model to offset
 * @param direction  String indication of absolute direction. Must be one of "North", "South", "East", or "West".
 * @param gap        the amount by which to offset the second group
 */
@JvmOverloads
fun offsetNetworkModel(
    reference: LocatableModel, target: LocatableModel, direction: Direction, gap: Double,
    refHeight: Double = 0.0, refWidth: Double = 0.0, tarHeight: Double = refHeight, tarWidth: Double = refWidth
) {
    val (refLeft, refTop, refRight, refBottom) = reference.run {
        listOf(
            locationX - refWidth / 2,
            locationY - refHeight / 2,
            locationX + refWidth / 2,
            locationY + refHeight / 2
        )
    }
    when (direction) {
        Direction.NORTH -> target.locationY = refTop - gap - tarHeight / 2
        Direction.SOUTH -> target.locationY = refBottom + gap + tarHeight / 2
        Direction.EAST -> target.locationX = refRight + gap + tarWidth / 2
        Direction.WEST -> target.locationX = refLeft - gap - tarWidth / 2
    }
}