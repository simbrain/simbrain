/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world;

/**
 * <b>Agent</b>.
 */
public interface Agent {

    /**
     * Return the name of this agent.
     *
     * @return the name of this agent
     */
    String getName();

    /**
     * Return the parent world for this agent.
     *
     * @return the parent world for this agent
     */
    World getParentWorld();

    /**
     * Return the stimulus for this agent provided the
     * specified sensor id.
     *
     * @param sensorId sensor id
     * @return the stimulus for this agent
     */
    double getStimulus(String[] sensorId);

    /**
     * Use when a full set of stimuli has been gathered,
     * to initiate post-stimulus gathering activties.
     */
    void completedInputRound();

    /**
     * Set the motor command for this agent to (some combination of?)
     * <code>commandList</code> and <code>value</code>.
     *
     * @param commandList command list
     * @param value value
     */
    void setMotorCommand(String[] commandList, double value);
}
