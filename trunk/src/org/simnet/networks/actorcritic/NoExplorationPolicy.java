/*
 * Part of Simbrain--a java-based neural network kit
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

package org.simnet.networks.actorcritic;

/**
 * This policy always selects the action with the highest activation.
 *
*/
public class NoExplorationPolicy implements ExplorationPolicy {

    /**
     * @param actions actions
     */
    public void selectAction(final double[] actions) {
    double max = actions[0];
    actions[0] = 1;
    int index = 0;
    for (int i = 1; i < actions.length; i++) {
        if (actions[i] > max) {
        actions[index] = 0;
        max = actions[i];
        actions[i] = 1;
        index = i;
        } else {
        actions[i] = 0;
        }
    }
    }
}
