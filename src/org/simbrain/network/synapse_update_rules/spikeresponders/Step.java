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
import org.simbrain.util.UserParameter;

/**
 * <b>Step</b>.
 */
public class Step extends SpikeResponder {

    /**
     * Timer.
     */
    private double timer;

    /**
     * Response height: The value by which the strength of the synapse is scaled
     * to determine the post synaptic response.
     */
    @UserParameter(label = "Response height", description = "This value is multiplied by"
    		+ " the strength to determine the total instantaneous rise in a post-synaptic"
    		+ " response to an action potential or spike.", defaultValue = "1", order = 1)
    private double responseHeight;

    /**
     * Response duration (ms).
     */
    @UserParameter(label = "Response time", description = "Response duration (ms)", defaultValue = "1", order = 1)
    private double responseDuration;

    /**
     * {@inheritDoc}
     */
    public void update(Synapse s) {
        if (s.getSource().isSpike()) {
            timer = responseDuration;
            value = responseHeight * s.getStrength();
        } else {
            timer -= s.getNetwork().getTimeStep();
            if (timer < 0) {
                timer = 0;
            }
        }

        if (timer <= 0) {
            value = 0;
        }

        s.setPsr(value);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Step deepCopy() {
        Step st = new Step();
        st.setResponseHeight(this.getResponseHeight());
        st.setResponseDuration(this.getResponseDuration());
        return st;
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
    public double getResponseDuration() {
        return responseDuration;
    }

    /**
     * @param responseDuration The responseTime to set.
     */
    public void setResponseDuration(final double responseDuration) {
        this.responseDuration = responseDuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Step";
    }
}
