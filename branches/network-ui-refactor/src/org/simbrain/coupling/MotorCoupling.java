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
package org.simbrain.coupling;

import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.world.Agent;

/**
 * <b>Motor Coupling</b> represents a coupling between the output node of a neural
 * network and some form of agent movement.  E.g. a motor neuron and a form of behavior.
 */
public class MotorCoupling extends Coupling {

    /** Array of command names for this motor coupling. */
    private String[] commandArray;  // also known as motor_id ?


    /**
     * Create a new motor coupling.
     */
    public MotorCoupling() {
        super();
    }

    /**
     * Create a new motor coupling with the specified agent and
     * array of command names.
     *
     * @param a agent for this coupling
     * @param ca array of command names for this motor coupling
     */
    public MotorCoupling(final Agent a, final String[] ca) {
        super(a);
        commandArray = ca;
    }

    /**
     * Create a new motor coupling with the specified agent, neuron,
     * and array of command names.
     *
     * @param a agent for this coupling
     * @param n neuron for this coupling
     * @param ca array of command names for this motor coupling
     */
    public MotorCoupling(final Agent a, final NeuronNode n, final String[] ca) {
        super(a, n);
        commandArray = ca;
    }

    /**
     * Create a new motor coupling with the specified neuron and array
     * of command names.
     *
     * @param n neuron for this coupling
     * @param ca array of command names for this motor coupling
     */
    public MotorCoupling(final NeuronNode n, final String[] ca) {
        super(n);
        commandArray = ca;
    }


    /**
     * Return the array of command names for this motor coupling.
     *
     * @return the array of command names for this motor coupling
     */
    public String[] getCommandArray() {
        return commandArray;
    }

    /**
     * Set the array of command names for this motor coupling to <code>ca</code>.
     *
     * @param ca array of command names for this motor coupling
     */
    public void setCommandArray(final String[] ca) {
        this.commandArray = ca;
    }

    /**
     * Print debug information to <code>System.out</code>.
     */
    public void debug() {
        super.debug();

        for (int i = 0; i < commandArray.length; i++) {
            System.out.println("\t Command [" + i + "]" + ": " + commandArray[i]);
        }
    }

    /**
     * Return a short label for this motor coupling, that is
     * the elements in the array of command names separated by spaces.
     *
     * @return a short label for this motor coupling
     */
    public String getShortLabel() {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < commandArray.length; i++) {
            sb.append(commandArray[i]);
            sb.append(" ");
        }

        return sb.toString();
    }
}
