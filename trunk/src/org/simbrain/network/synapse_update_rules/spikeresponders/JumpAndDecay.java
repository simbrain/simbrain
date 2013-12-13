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
 * <b>JumpAndDecay</b>.
 */
public class JumpAndDecay extends SpikeResponder {

    /** Jump height value. */
    private double jumpHeight = 1;

    /** Base line value. */
    private double baseLine;

    /** Rate at which synapse will decay (ms). */
    private double timeConstant = 3;

    /**
     * {@inheritDoc}
     */
    @Override
    public JumpAndDecay deepCopy() {
        JumpAndDecay jad = new JumpAndDecay();
        jad.setBaseLine(this.getBaseLine());
        jad.setJumpHeight(this.getJumpHeight());
        jad.setTimeConstant(this.getTimeConstant());
        return jad;
    }

    /**
     * {@inheritDoc}
     */
    public void update(final Synapse s) {
        value = s.getPsr();
        if (((SpikingNeuronUpdateRule) s.getSource().getUpdateRule())
                .hasSpiked()) {
            value = jumpHeight * s.getStrength();
        } else {
            double timeStep = s.getParentNetwork().getTimeStep();
            value += timeStep * (baseLine - value) / timeConstant;
        }
        s.setPsr(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Jump and Decay";
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

    /**
     * @return the time constant of the exponential decay of the post synaptic
     *         response
     */
    public double getTimeConstant() {
        return timeConstant;
    }

    /**
     * @param decayTimeConstant the new time constant of the exponential decay
     *            of the post synaptic response
     */
    public void setTimeConstant(double decayTimeConstant) {
        this.timeConstant = decayTimeConstant;
    }

}
