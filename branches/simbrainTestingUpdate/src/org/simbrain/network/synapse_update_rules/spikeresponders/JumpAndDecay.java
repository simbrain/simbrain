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

/**
 * <b>JumpAndDecay</b>.
 */
public class JumpAndDecay extends SpikeResponder {
    /** Jump height value. */
    private double jumpHeight = 2;
    /** Base line value. */
    private double baseLine = 0;
    /** Rate at which synapse will decay. */
    private double decayRate = .1;

    /**
     * @return null
     */
    public SpikeResponder duplicate() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Update the synapse.
     */
    public void update() {
        if (((SpikingNeuronUpdateRule) parent.getSource().getUpdateRule())
                .hasSpiked()) {
            value = jumpHeight;
        } else {
            value += (decayRate * (baseLine - value));
        }
    }

    /**
     * @return Returns the baseLine.
     */
    public double getBaseLine() {
        return baseLine;
    }

    /**
     * @param baseLine The baseLine to set.
     */
    public void setBaseLine(final double baseLine) {
        this.baseLine = baseLine;
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
     * @return Returns the jumpHeight.
     */
    public double getJumpHeight() {
        return jumpHeight;
    }

    /**
     * @param jumpHeight The jumpHeight to set.
     */
    public void setJumpHeight(final double jumpHeight) {
        this.jumpHeight = jumpHeight;
    }

    /**
     * @return Name of synapse type.
     */
    public static String getName() {
        return "Jump and decay";
    }
}
