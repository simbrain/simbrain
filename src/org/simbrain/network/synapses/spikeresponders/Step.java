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
import org.simbrain.network.interfaces.SpikingNeuronUpdateRule;


/**
 * <b>Step</b>.
 */
public class Step extends SpikeResponder {

    /** Timer. */
    private double timer = 0;

    /** Response height. */
    private double responseHeight = 1;

    /** Response time. */
    private double responseTime = 1;

    /**
     * @return duplicate StepSynapse (used, e.g., in copy/paste).
     */
    public SpikeResponder duplicate() {
        Step s = new Step();
        s = (Step) super.duplicate(s);
        s.setResponseHeight(getResponseHeight());
        s.setResponseHeight(getResponseTime());

        return s;
    }

    /**
     * Update the synapse.
     */
    public void update() {
        if (((SpikingNeuronUpdateRule) parent.getSource().getUpdateRule()).hasSpiked()) {
            timer = responseTime;
        } else {
            timer--;

            if (timer < 0) {
                timer = 0;
            }
        }

        if (timer > 0) {
            value = responseHeight;
        } else {
            value = 0;
        }
    }

    /**
     * @return Returns the responseHeight.
     */
    public double getResponseHeight() {
        return responseHeight;
    }

    /**
     * @param responseHeight The responseHeight to set.
     */
    public void setResponseHeight(final double responseHeight) {
        this.responseHeight = responseHeight;
    }

    /**
     * @return Returns the responseTime.
     */
    public double getResponseTime() {
        return responseTime;
    }

    /**
     * @param responseTime The responseTime to set.
     */
    public void setResponseTime(final double responseTime) {
        this.responseTime = responseTime;
    }

    /**
     * @return Name of synapse type.
     */
    public static String getName() {
        return "Step";
    }
}
