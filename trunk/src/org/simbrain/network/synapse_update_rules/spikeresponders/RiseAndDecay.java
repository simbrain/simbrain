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
package org.simbrain.network.synapse_update_rules.spikeresponders;

import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.core.Synapse;

/**
 * <b>RiseAndDecay</b>.
 */
public class RiseAndDecay extends SpikeResponder {

    /** Maximum response value. */
    private double maximumResponse = 1;

    /** The time constant of decay and recovery (ms). */
    private double timeConstant = 3;

    /** Recovery value. */
    private double recovery;

    /**
     * {@inheritDoc}
     */
    @Override
    public RiseAndDecay deepCopy() {
        RiseAndDecay rad = new RiseAndDecay();
        rad.setMaximumResponse(this.getMaximumResponse());
        rad.setTimeConstant(this.getTimeConstant());
        return rad;
    }

    /**
     * {@inheritDoc}
     */
    public void update(Synapse s) {
        double timeStep = s.getParentNetwork().getTimeStep();
        if (((SpikingNeuronUpdateRule) s.getSource().getUpdateRule())
                .hasSpiked()) {
            recovery = 1;
        }

        recovery += ((timeStep / timeConstant) * (-recovery));
        value += ((timeStep / timeConstant) * ((Math.E * maximumResponse
                * recovery * (1 - value)) - value));

        s.setPsr(value * s.getStrength());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Rise and Decay";
    }

    /**
     * @return Returns the decayRate.
     */
    public double getTimeConstant() {
        return timeConstant;
    }

    /**
     * @param timeConstant The decayRate to set.
     */
    public void setTimeConstant(final double timeConstant) {
        this.timeConstant = timeConstant;
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

}
