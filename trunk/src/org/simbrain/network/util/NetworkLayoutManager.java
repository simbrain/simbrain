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
package org.simbrain.network.util;

import org.simbrain.network.groups.NeuronGroup;

/**
 * A package for laying out neurons and groups with respect to each other. This
 * is not dependent on any GUI classes, because only model neuron position
 * properties are affected by these methods.
 *
 * @author jyoshimi
 * @author ztosi
 */
public class NetworkLayoutManager {

    /** Directions. */
    public static enum Direction {
        NORTH, SOUTH, EAST, WEST
    };

    // TODO: Make a version of the method below that takes arbitrary lists of
    // neurons as arguments.

    /**
     * Group1 stays fixed. Group2 is moved with respect to group 1 and is
     * centered with respect to it in the relevant direction.
     *
     * Must be used after all the subgroups have been added.
     *
     * @param group1 the reference group
     * @param group2 the group to offset
     * @param direction String indication of absolute direction. Must be one of
     *            "North", "South", "East", or "West".
     * @param amount the amount by which to offset the second group
     */
    public static void offsetNeuronGroup(NeuronGroup group1,
            NeuronGroup group2, Direction direction, double amount) {

        double targetX = 0;
        double targetY = 0;

        if (direction == Direction.NORTH) {
            targetX = group1.getCenterX();
            targetY = group1.getCenterY() - (group1.getHeight() / 2) - amount
                    - (group2.getHeight() / 2);
        } else if (direction == Direction.SOUTH) {
            targetX = group1.getCenterX();
            targetY = group1.getCenterY() + (group1.getHeight() / 2) + amount
                    + (group2.getHeight() / 2);
        } else if (direction == Direction.EAST) {
            targetX = group1.getCenterX() + (group1.getWidth() / 2) + amount
                    + (group2.getWidth() / 2);
            targetY = group1.getCenterY();
        } else if (direction == Direction.WEST) {
            targetX = group1.getCenterX() - (group1.getWidth() / 2) - amount
                    - (group2.getWidth() / 2);
            targetY = group1.getCenterY();
        }

        double offsetX = targetX - group2.getCenterX();
        double offsetY = targetY - group2.getCenterY();
        group2.offset(offsetX, offsetY);
    }

}
