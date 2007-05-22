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
 * <b>ClampedSynapse</b>.
 */
public class ClampedSynapse extends Synapse {
    /** clipped */
    public boolean clipped = false;
    
    /**
     * Creates a weight of some value connecting two neurons.
     *
     * @param src source neuron
     * @param tar target neuron
     * @param val initial weight value
     * @param theId Id of synapse
     */
    public ClampedSynapse(final Neuron src, final Neuron tar, final double val, final String theId) {
        source = src;
        target = tar;
        strength = val;
        id = theId;
    }

    /**
     * Creates a weight connecting source and target neurons.
     *
     * @param source source neuron
     * @param target target neuron
     */
    public ClampedSynapse(final Neuron source, final Neuron target) {
        this.source = source;
        this.target = target;
    }

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public ClampedSynapse() {
        super();
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param s Synapse to make of the type
     */
    public ClampedSynapse(final Synapse s) {
        super(s);
    }

    /**
     * @return duplicate ClampedSynapse (used, e.g., in copy/paste).
     */
    public Synapse duplicate() {
        ClampedSynapse cs = new ClampedSynapse();
        cs = (ClampedSynapse) super.duplicate(cs);

        return cs;
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
        return "Clamped (no learning)";
    }
    
    /**
     * Return clipped
     */
    public boolean isClipped(){
	return clipped;
    }
    
    /**
     * @param clipped value to set
     */
    public void setClipped(boolean clipped){
	this.clipped = clipped;
    }
    
    public void setStrength(double wt){
	if(clipped)
	    super.setStrength(clip(wt));
	else
	    super.setStrength(wt);
    }
    
    public double getStrength(){
	return super.getStrength();
    }

//    /**
//     * Set the parameters for this weight (it's strength, learning rule, etc).
//     *
//     * @param values an array of Strings containing new parameter settings
//     */
//    public void setParameters(String[] values) {
//        if (values.length < NUM_PARAMETERS)
//            return;
//
//        if (values[3] != null)
//            strength = Double.parseDouble(values[3]);
//        if (values[4] != null)
//            lowerBound = Double.parseDouble(values[4]);
//        if (values[5] != null)
//            upperBound = Double.parseDouble(values[5]);
//        if (values[6] != null)
//            increment = Double.parseDouble(values[6]);
//    }
//
//    /**
//     * Get the parameters for this weight (it's strength, learning rule, etc).
//     *
//     * @return  an array of Strings containing parameter settings for this weight
//     */
//    public String[] getParameters() {
//        return new String[] {
//            "","", null,
//            //getLearningRule().getName(),
//            Double.toString(getStrength()),
//            Double.toString(getLowerBound()),
//            Double.toString(getUpperBound()),
//            Double.toString(getIncrement()),
//            ""
//        };
//    }
}
