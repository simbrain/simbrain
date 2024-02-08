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

import org.simbrain.network.LocatableModel
import org.simbrain.network.core.AbstractNeuronCollection
import org.simbrain.util.minus
import org.simbrain.util.plus
import org.simbrain.util.point

/**
 * Directions.
 */
enum class Direction {
    NORTH, SOUTH, EAST, WEST
}

/**
 * Group1 stays fixed. Group2 is moved with respect to group 1 and is
 * centered with respect to it in the relevant direction.
 *
 *
 * Must be used after all the subgroups have been added.
 *
 * @param group1    the reference group
 * @param group2    the group to offset
 * @param direction String indication of absolute direction. Must be one of "North", "South", "East", or "West".
 * @param amount    the amount by which to offset the second group
 */
fun offsetNeuronGroup(group1: AbstractNeuronCollection, group2: AbstractNeuronCollection, direction: Direction, amount: Double) {
    group2.location = when(direction) {
        Direction.NORTH -> group1.location - point(0.0, group1.height / 2 + group2.height / 2 + amount)
        Direction.SOUTH -> group1.location + point(0.0, group1.height / 2 + group2.height / 2 + amount)
        Direction.EAST  -> group1.location + point(group1.width / 2 + group2.width / 2 + amount, 0.0)
        Direction.WEST  -> group1.location - point(group1.width / 2 + group2.width / 2 + amount, 0.0)
    }
}

/**
 * Same as above but specify height and width, which are assumed to be the same for both models.
 */
fun offsetNeuronGroup(group1: LocatableModel, group2: LocatableModel, direction: Direction, amount: Double,
    height: Double, width: Double) {
    group2.location = when(direction) {
        Direction.NORTH -> group1.location - point(0.0, height / 2 + height / 2 + amount)
        Direction.SOUTH -> group1.location + point(0.0, height / 2 + height / 2 + amount)
        Direction.EAST  -> group1.location + point(width / 2 + width / 2 + amount, 0.0)
        Direction.WEST  -> group1.location - point(width / 2 + width / 2 + amount, 0.0)
    }
}