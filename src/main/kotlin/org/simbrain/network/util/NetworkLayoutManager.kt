/*
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

/**
 * A package for laying out neurons and groups with respect to each other. This
 * is not dependent on any GUI classes, because only model neuron position
 * properties are affected by these methods.
 *
 * @author jyoshimi
 * @author ztosi
 */
package org.simbrain.network.util

import org.simbrain.network.*
import org.simbrain.network.core.AbstractNeuronCollection
import org.simbrain.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.collections.component4
import kotlin.collections.listOf

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
 * Group1 stays fixed. Group2 is moved with respect to group 1 and is
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
    reference: AbstractNeuronCollection,
    target: AbstractNeuronCollection,
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
 * Same as above but specify height and width, which are assumed to be the same for both models.
 */
fun offsetNetworkModel(
    reference: LocatableModel, target: LocatableModel, direction: Direction, amount: Double,
    height: Double, width: Double
) {
    val (refLeft, refTop, refRight, refBottom) = reference.run {
        listOf(
            locationX - width / 2,
            locationY - height / 2,
            locationX + width / 2,
            locationY + height / 2
        )
    }
    when (direction) {
        Direction.NORTH -> target.locationY = refTop - amount - height / 2
        Direction.SOUTH -> target.locationY = refBottom + amount + height / 2
        Direction.EAST -> target.locationX = refRight + amount + width / 2
        Direction.WEST -> target.locationX = refLeft - amount - width / 2
    }
}