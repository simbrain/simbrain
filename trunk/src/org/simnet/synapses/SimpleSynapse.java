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

package org.simnet.synapses;

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;

/**
 * A simple synpase should be used as part of sub-networks that implement the
 * weight-update rule for it. Simple synapse does not have any weight update
 * rule for itself.
 */
public class SimpleSynapse extends Synapse {

    /** Clipped. */
    private boolean clipped = false;

    /**
     * Creates a weight of some value connecting two neurons.
     *
     * @param src
     *            source neuron
     * @param tar
     *            target neuron
     * @param val
     *            initial weight value
     * @param theId
     *            Id of synapse
     */
    public SimpleSynapse(final Neuron src, final Neuron tar, final double val,
            final String theId) {
        super(src, tar);
        // setSource(src);
        // setTarget(tar);
        strength = val;
        id = theId;
    }

    /**
     * Creates a weight connecting source and target neurons.
     *
     * @param source source neuron
     * @param target target neuron
     */
    public SimpleSynapse(final Neuron source, final Neuron target) {
        super(source, target);
    }

    /**
     * This constructor is used when creating a neuron of one type from another
     * neuron of another type Only values common to different types of neuron
     * are copied.
     *
     * @param s Synapse to make of the type
     */
    public SimpleSynapse(final Synapse s) {
        super(s);
    }

    /**
     * @return duplicate ClampedSynapse (used, e.g., in copy/paste).
     */
    public Synapse duplicate() {
        SimpleSynapse ss = new SimpleSynapse(this.getSource(), this.getTarget());
        ss = (SimpleSynapse) super.duplicate(ss);

        return ss;
    }

    /**
     * Set strength.
     */
    public void setStrength(final double wt) {
        if (clipped) {
            super.setStrength(clip(wt));
        } else {
            super.setStrength(wt);
        }
    }

    /**
     * Returns strength.
     */
    public double getStrength() {
        return super.getStrength();
    }

    /**
     * Update the synapse.
     */
    public void update() {
    }

    /**
     * @return Name of synapse type.
     */
    public static String getName() {
        return "Simple Synapse";
    }

    /**
     * Return clipped.
     */
    public boolean isClipped() {
        return clipped;
    }

    /**
     * @param clipped value to set
     */
    public void setClipped(final boolean clipped) {
        this.clipped = clipped;
    }

}
