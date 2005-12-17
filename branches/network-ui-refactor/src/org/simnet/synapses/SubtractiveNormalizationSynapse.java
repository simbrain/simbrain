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
package org.simnet.synapses;

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;


/**
 * <b>SubtractiveNormalizationSynapse</b>
 */
public class SubtractiveNormalizationSynapse extends Synapse {
    private double momentum = 1;

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public SubtractiveNormalizationSynapse() {
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param s Synapse to make of the type
     */
    public SubtractiveNormalizationSynapse(final Synapse s) {
        super(s);
    }

    /**
     * @return Name of synapse type.
     */
    public static String getName() {
        return "Subtractive Normalizaion";
    }

    /**
     * @return duplicate SubtractiveNormalizationSynapse (used, e.g., in copy/paste).
     */
    public Synapse duplicate() {
        SubtractiveNormalizationSynapse sns = new SubtractiveNormalizationSynapse();
        sns = (SubtractiveNormalizationSynapse) super.duplicate(sns);
        sns.setMomentum(getMomentum());

        return sns;
    }

    /**
     * Creates a weight connecting source and target neurons.
     *
     * @param source source neuron
     * @param target target neuron
     */
    public SubtractiveNormalizationSynapse(final Neuron source, final Neuron target) {
        this.source = source;
        this.target = target;
    }

    /**
     * Updates the syanpse.
     */
    public void update() {
        double input = getSource().getActivation();
        double output = getTarget().getActivation();
        double averageInput = getTarget().getAverageInput();

        strength += (momentum * ((output * input) - (output * averageInput)));
        strength = clip(strength);
    }

    /**
     * @return Returns the momentum.
     */
    public double getMomentum() {
        return momentum;
    }

    /**
     * @param momentum The momentum to set.
     */
    public void setMomentum(final double momentum) {
        this.momentum = momentum;
    }
}
