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
 * 
 * <b>DeltaRuleSynapse</b>
 */
public class DeltaRuleSynapse extends Synapse {
    
    private boolean inputOutput = false;
    private double desiredOutput = 0;
    private double momentum = 0;
    
    /**
     * Creates a weight of some value connecting two neurons
     * 
     * @param source source neuron
     * @param target target neuron
     * @param val initial weight value
     */
    public DeltaRuleSynapse(Neuron src, Neuron tar, double val, String the_id) {
        source = src;
        target = tar;
        strength = val;
        id = the_id;
    }
    
    public DeltaRuleSynapse() {
    }
    
    public DeltaRuleSynapse(Synapse s) {
        super(s);
    }
    
    /**
     * Creates a weight connecting source and target neurons
     * 
     * @param source source neuron
     * @param target target neuron
     */
    public DeltaRuleSynapse(Neuron source, Neuron target) {
        this.source = source;
        this.target = target;
    }
    
    public void update() {

    }

    public Synapse duplicate() {
        DeltaRuleSynapse dr = new DeltaRuleSynapse();
        dr = (DeltaRuleSynapse)super.duplicate(dr);
        return dr;
    }
    
    public static String getName() {return "Delta rule";}

    /**
     * @return Returns the desiredOutput.
     */
    public double getDesiredOutput() {
        return desiredOutput;
    }

    /**
     * @param desiredOutput The desiredOutput to set.
     */
    public void setDesiredOutput(double desiredOutput) {
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
    public void setMomentum(double momentum) {
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
    public void setInputOutput(boolean inputOutput) {
        this.inputOutput = inputOutput;
    }

}
