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

import org.simbrain.network.core.Synapse;
import org.simbrain.network.util.ScalarDataHolder;
import org.simbrain.util.UserParameter;

/**
 * <b>RiseAndDecay</b>.
 */
public class RiseAndDecay extends SpikeResponder {

    /**
     * Maximum response value.
     */
    @UserParameter(label = "Maximum Response", description = "Maximum response value.",
            increment = .1, order = 1)
    private double maximumResponse = 1;

    /**
     * The time constant of decay and recovery (ms).
     */
    @UserParameter(label = "Time constant", description = "Rate at which synapse will decay (ms)",
            increment = .1, order = 1)
    private double timeConstant = 3;

    /**
     * Recovery value.
     */
    private double recovery;

    @Override
    public RiseAndDecay deepCopy() {
        RiseAndDecay rad = new RiseAndDecay();
        rad.setMaximumResponse(this.getMaximumResponse());
        rad.setTimeConstant(this.getTimeConstant());
        return rad;
    }

    @Override
    public void apply(Synapse s, ScalarDataHolder responderData) {
        double timeStep = s.getParentNetwork().getTimeStep();
        if (s.getSource().isSpike()) {
            recovery = 1;
        }

        recovery += ((timeStep / timeConstant) * (-recovery));
        s.setPsr(s.getPsr() + ((timeStep / timeConstant)
                * ((Math.E * maximumResponse * recovery * (1 - s.getPsr())) - s.getPsr())));

        s.setPsr(s.getPsr() * s.getStrength());

    }

    @Override
    public String getDescription() {
        return "Rise and Decay";
    }

    @Override
    public String getName() {
        return "Rise and Decay";
    }

    public double getTimeConstant() {
        return timeConstant;
    }

    public void setTimeConstant(final double timeConstant) {
        this.timeConstant = timeConstant;
    }

    public double getMaximumResponse() {
        return maximumResponse;
    }

    public void setMaximumResponse(final double maximumResponse) {
        this.maximumResponse = maximumResponse;
    }

}
