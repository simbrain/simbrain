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
 * <b>Probabilistic</b> spike responders produces a response with some
 * probability.
 */
public class ProbabilisticResponder extends SpikeResponder {

    /** Probability of producing an output; must be between 0 and 1. */
    private double activationProbability = .5;

    /**
     * Amount by which the synapse's strength will be scaled to determine the
     * post synaptic response of the synapse in the event that this responder is
     * actually active.
     */
    private double responseValue = 1;

    /**
     * {@inheritDoc}
     */
    @Override
    public ProbabilisticResponder deepCopy() {
        ProbabilisticResponder pr = new ProbabilisticResponder();
        pr.setActivationProbability(this.getActivationProbability());
        pr.setResponseValue(this.getResponseValue());
        return pr;
    }

    /**
     * {@inheritDoc}
     */
    public void update(Synapse s) {
        if (((SpikingNeuronUpdateRule) s.getSource().getUpdateRule())
                .hasSpiked()) {
            if (Math.random() > (1 - activationProbability)) {
                value = responseValue * s.getStrength();
            } else {
                value = 0;
            }
        } else {
            value = 0; // In case it did not spike at all;
        }
        s.setPsr(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
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
