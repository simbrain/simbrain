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
 * <b>DeltaRuleSynapse</b>
 */
public class DeltaRuleSynapse extends Synapse {
    private boolean inputOutput = false;
    private double desiredOutput = 0;
    private double momentum = 0;

    /**
     * Creates a weight of some value connecting two neurons.
     *
     * @param src source neuron
     * @param tar target neuron
     * @param val initial weight value
     * @param theId Id of the synapse
     */
    public DeltaRuleSynapse(final Neuron src, final Neuron tar, final double val, final String theId) {
        source = src;
        target = tar;
        strength = val;
        id = theId;
    }

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public DeltaRuleSynapse() {
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param s Synapse to make of the type
     */
    public DeltaRuleSynapse(final Synapse s) {
        super(s);
    }

    /**
     * Creates a weight connecting source and target neurons.
     *
     * @param source source neuron
     * @param target target neuron
     */
    public DeltaRuleSynapse(final Neuron source, final Neuron target) {
        this.source = source;
        this.target = target;
    }

    /**
     * Updates the synapse.
     */
    public void update() {
        double input = getSource().getActivation();
        double output = getTarget().getActivation();

        if (inputOutput) {
            desiredOutput = getTarget().getInputValue();
        }

        strength += (momentum * input * (desiredOutput - output));

        strength = clip(strength);
    }

    /**
     * @return duplicate DeltaRuleSynapse (used, e.g., in copy/paste).
     */
    public Synapse duplicate() {
        DeltaRuleSynapse dr = new DeltaRuleSynapse();
        dr.setDesiredOutput(getDesiredOutput());
        dr.setInputOutput(getInputOutput());
        dr.setMomentum(getMomentum());

        return super.duplicate(dr);
    }

    /**
     * @return Name of synapse type.
     */
    public static String getName() {
        return "Delta rule";
    }

    /**
     * @return Returns the desiredOutput.
     */
    public double getDesiredOutput() {
        return desiredOutput;
    }

    /**
     * @param desiredOutput The desiredOutput to set.
     */
    public void setDesiredOutput(final double desiredOutput) {
        this.desiredOutput = desiredOutput;
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

    /**
     * @return Returns the inputOutput.
     */
    public boolean getInputOutput() {
        return inputOutput;
    }

    /**
     * @param inputOutput The inputOutput to set.
     */
    public void setInputOutput(final boolean inputOutput) {
        this.inputOutput = inputOutput;
    }
}
