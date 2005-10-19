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
 * <b>HebbianThresholdSynapse</b>
 */
public class HebbianThresholdSynapse extends Synapse {
    
    private double momentum = 1;
    private double inputThresholdMomentum = 0;
    private double outputThresholdMomentum = 0;
    private double inputThreshold = 0;
    private boolean useSlidingInputThreshold = false;
    private double outputThreshold = 0;
    private boolean useSlidingOutputThreshold = false;
    
    public HebbianThresholdSynapse(Neuron src, Neuron tar, double val, String the_id) {
        source = src;
        target = tar;
        strength = val;
        id = the_id;
    }
    
    public HebbianThresholdSynapse() {
    }
    
    public HebbianThresholdSynapse(Synapse s) {
        super(s);
    }
    
    public static String getName() {return "Hebbian threshold";}

    public Synapse duplicate() {
        Hebbian h = new Hebbian();
        return super.duplicate(h);
    }
    
    /**
     * Creates a weight connecting source and target neurons
     * 
     * @param source source neuron
     * @param target target neuron
     */
    public HebbianThresholdSynapse(Neuron source, Neuron target) {
        this.source = source;
        this.target = target;
    }

    public void update() {
        
        double input = getSource().getActivation();
        double output = getTarget().getActivation();
        
        if (useSlidingInputThreshold == true) {
            inputThreshold += inputThresholdMomentum/10 * ((input * input) - inputThreshold);
        }
        if (useSlidingOutputThreshold == true) {
            outputThreshold += outputThresholdMomentum/10 * ((output * output) - outputThreshold);
        }
        
        strength += momentum * (input - inputThreshold) * (output - outputThreshold);
    
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
    public void setMomentum(double momentum) {
        this.momentum = momentum;
    }
    /**
     * @return Returns the inputThreshold.
     */
    public double getInputThreshold() {
        return inputThreshold;
    }
    /**
     * @param inputThreshold The inputThreshold to set.
     */
    public void setInputThreshold(double inputThreshold) {
        this.inputThreshold = inputThreshold;
    }
    /**
     * @return Returns the outputThreshold.
     */
    public double getOutputThreshold() {
        return outputThreshold;
    }
    /**
     * @param outputThreshold The outputThreshold to set.
     */
    public void setOutputThreshold(double outputThreshold) {
        this.outputThreshold = outputThreshold;
    }
    /**
     * @return Returns the useSlidingInputThreshold.
     */
    public boolean getUseSlidingInputThreshold() {
        return useSlidingInputThreshold;
    }
    /**
     * @param useSlidingInputThreshold The useSlidingInputThreshold to set.
     */
    public void setUseSlidingInputThreshold(boolean useSlidingInputThreshold) {
        this.useSlidingInputThreshold = useSlidingInputThreshold;
    }
    /**
     * @return Returns the useSlidingOutputThreshold.
     */
    public boolean getUseSlidingOutputThreshold() {
        return useSlidingOutputThreshold;
    }
    /**
     * @param useSlidingOutputThreshold The useSlidingOutputThreshold to set.
     */
    public void setUseSlidingOutputThreshold(boolean useSlidingOutputThreshold) {
        this.useSlidingOutputThreshold = useSlidingOutputThreshold;
    }

    /**
     * @return Returns the inputThresholdMomentum.
     */
    public double getInputThresholdMomentum() {
        return inputThresholdMomentum;
    }

    /**
     * @param inputThresholdMomentum The inputThresholdMomentum to set.
     */
    public void setInputThresholdMomentum(double inputThresholdMomentum) {
        this.inputThresholdMomentum = inputThresholdMomentum;
    }

    /**
     * @return Returns the outputThresholdMomentum.
     */
    public double getOutputThresholdMomentum() {
        return outputThresholdMomentum;
    }

    /**
     * @param outputThresholdMomentum The outputThresholdMomentum to set.
     */
    public void setOutputThresholdMomentum(double outputThresholdMomentum) {
        this.outputThresholdMomentum = outputThresholdMomentum;
    }
}
