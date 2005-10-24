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
 * <b>JumpAndDecay</b>
 */
public class JumpAndDecay extends SpikeResponder {
    private double jumpHeight = 1;
    private double baseLine = 0;
    private double decayRate = .1;

    public SpikeResponder duplicate() {
        // TODO Auto-generated method stub
        return null;
    }

    public void update() {
        if (((SpikingNeuron) parent.getSource()).hasSpiked() == true) {
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
    public void setBaseLine(double baseLine) {
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
    public void setDecayRate(double decayRate) {
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
    public void setJumpHeight(double jumpHeight) {
        this.jumpHeight = jumpHeight;
    }

    public static String getName() {
        return "Jump and decay";
    }
}
