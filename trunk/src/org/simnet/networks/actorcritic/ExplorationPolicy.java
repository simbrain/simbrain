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
 * Exploration is a critical requirement to enable learning in a
 * reinforcement learning technique. ExplorationPolicy adds some
 * form of randomization to the action selection process and gives a
 * chance to the network to explore undiscovered regions of the state
 * space
 *
 */
public interface ExplorationPolicy {
    /**
     *
     * @param actions actions
     */
    void selectAction(double [] actions);
}
