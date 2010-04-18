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
package org.simbrain.network.synapses.spikeresponders;

import org.simbrain.network.interfaces.SpikeResponder;
import org.simbrain.network.interfaces.SpikingNeuron;


/**
 * <b>Probabilistic</b> spike responders produces a response with some probability.
 */
public class ProbabilisticResponder extends SpikeResponder {


    /** Probability of producing an output; must be between 0 and 1. */
    private double activationProbability = .5;

    /** Amount of activation to return when this responder is activated. */
    private double responseValue = 1;
    
    /**
     * @return duplicate StepSynapse (used, e.g., in copy/paste).
     */
    public SpikeResponder duplicate() {
        ProbabilisticResponder s = new ProbabilisticResponder();
        s = (ProbabilisticResponder) super.duplicate(s);
        s.setActivationProbability(this.getActivationProbability());
        s.setResponseValue(this.getResponseValue());
        return s;
    }

    /**
     * Update the synapse.
     */
    public void update() {
        if (((SpikingNeuron) parent.getSource()).hasSpiked()) {
            if (Math.random() > (1 - activationProbability)) {
                value = responseValue * parent.getStrength();
            } else {
                value = 0;
            }
        } else {
            value = 0; // In case it did not spike at all;
        }
    }

    /**
     * @return Name of synapse type.
     */
    public static String getName() {
        return "Probabilistic";
    }

    /**
     * @return the activationProbability
     */
    public double getActivationProbability() {
        return activationProbability;
    }

    /**
     * @param activationProbability the activationProbability to set
     */
    public void setActivationProbability(double activationProbability) {
        this.activationProbability = activationProbability;
    }

    /**
     * @return the responseVaue
     */
    public double getResponseValue() {
        return responseValue;
    }

    /**
     * @param responseVaue the responseVaue to set
     */
    public void setResponseValue(double responseVaue) {
        this.responseValue = responseVaue;
    }
}
