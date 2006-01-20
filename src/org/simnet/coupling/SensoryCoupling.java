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
package org.simnet.coupling;

import org.simbrain.world.Agent;
import org.simnet.interfaces.Neuron;

/**
 * <b>Sensory Coupling</b> represents an coupling between an agent's sensory apparatus
 * and an input node of a neural network.
 */
public class SensoryCoupling extends Coupling {

    /** Array of sensor names for this sensory coupling. */
    private String[] sensorArray;


    /**
     * Create a new sensory coupling.
     */
    public SensoryCoupling() {
        super();
    }

    /**
     * Create a new sensory coupling with the specified agent, neuron,
     * and array of sensor names.
     *
     * @param a agent for this coupling
     * @param n neuron for this coupling
     * @param sa array of sensor names for this coupling
     */
    public SensoryCoupling(final Agent a, final Neuron n, final String[] sa) {
        super(a, n);
        sensorArray = sa;
    }

    /**
     * Create a new sensory coupling with the specified neuron and
     * array of sensor names.
     *
     * @param n neuron for this coupling
     * @param sa array of sensor names for this coupling
     */
    public SensoryCoupling(final Neuron n, final String[] sa) {
        super(n);
        sensorArray = sa;
    }

    /**
     * Create a new sensory coupling with the specified agent and
     * array of sensor names.
     *
     * @param a agent for this coupling
     * @param sa array of sensor names for this coupling
     */
    public SensoryCoupling(final Agent a, final String[] sa) {
        super(a);
        sensorArray = sa;
    }


    /**
     * Return the array of sensor names for this sensory coupling.
     *
     * @return the array of sensor names for this sensory coupling
     */
    public String[] getSensorArray() {
        return sensorArray;
    }

    /**
     * Set the array of sensor names for this sensory coupling to <code>sa</code>.
     *
     * @param sa array of sensor names
     */
    public void setSensorArray(final String[] sa) {
        this.sensorArray = sa;
    }

    /**
     * Print debug information to <code>System.out</code>.
     */
    public void debug() {
        super.debug();

        for (int i = 0; i < sensorArray.length; i++) {
            System.out.println("\t Sensor [" + i + "]" + ": " + sensorArray[i]);
        }
    }

    /**
     * Return a short label for this sensory coupling, that is
     * the elements in the array of sensor names separated by spaces.
     *
     * @return a short label for this sensory coupling
     */
    public String getShortLabel() {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < sensorArray.length; i++) {
            sb.append(sensorArray[i]);
            sb.append(" ");
        }

        return sb.toString();
    }
}
