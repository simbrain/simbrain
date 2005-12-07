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
package org.simnet.synapses.spikeresponders;

import org.simnet.interfaces.SpikeResponder;
import org.simnet.interfaces.SpikingNeuron;


/**
 * <b>RiseAndDecay</b>
 */
public class RiseAndDecay extends SpikeResponder {
    private double maximumResponse = 1;
    private double decayRate = .1;
    private double timeStep = .01;
    private double recovery = 0;

    public SpikeResponder duplicate() {
        // TODO Auto-generated method stub
        return null;
    }

    public void update() {
        if (((SpikingNeuron) parent.getSource()).hasSpiked() == true) {
            recovery = 1;
        }

        recovery += ((timeStep / decayRate) * (-recovery));
        value += ((timeStep / decayRate) * ((Math.E * maximumResponse * recovery * (1 - value)) - value));
    }

    /**
     * @return Returns the timeStep.
     */
    public double getTimeStep() {
        return timeStep;
    }

    /**
     * @param timeStep The timeStep to set.
     */
    public void setTimeStep(final double timeStep) {
        this.timeStep = timeStep;
    }

    /**
     * @return Returns the decayRate.
     */
    public double getDecayRate() {
        return decayRate;
    }

    /**
     * @param decayRate The decayRate to set.
     */
    public void setDecayRate(final double decayRate) {
        this.decayRate = decayRate;
    }

    /**
     * @return Returns the maximumResponse.
     */
    public double getMaximumResponse() {
        return maximumResponse;
    }

    /**
     * @param maximumResponse The maximumResponse to set.
     */
    public void setMaximumResponse(final double maximumResponse) {
        this.maximumResponse = maximumResponse;
    }

    public static String getName() {
        return "Rise and decay";
    }
}
