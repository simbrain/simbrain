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
package org.simnet.neurons;

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;

/**
 * 
 * <b>IACNeuron</b>
 */
public class IACNeuron extends Neuron {

    private double decay = 0;
    private double rest = 0;
    
    /**
     * Default constructor needed for external calls which create neurons then 
     * set their parameters
     */
    public IACNeuron() {
    }
    
    /**
     *  This constructor is used when creating a neuron of one type from another neuron of another type
     *  Only values common to different types of neuron are copied
     */
    public IACNeuron(Neuron n) {
        super(n);
    }
    
    public int getTimeType() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Returns a duplicate IACNeuron (used, e.g., in copy/paste)
     */
    public Neuron duplicate() {
        IACNeuron iac = new IACNeuron();
        iac = (IACNeuron)super.duplicate(iac);
        iac.setDecay(getDecay());
        iac.setRest(getRest());
        return iac;
    }

    public void update() {
		double val = activation;
		double wtdSum = 0;
		if (fanIn.size() > 0) {
			for (int j = 0; j < fanIn.size(); j++) {
				Synapse w = (Synapse) fanIn.get(j);
				Neuron source = w.getSource();
				if (source.getActivation() > 0) {
					wtdSum += w.getStrength() * source.getActivation();
				}
			}
		}
		
	if (wtdSum > 0) {

		val += wtdSum * (upperBound - activation) - decay * (activation - rest );

	} else {

		val += wtdSum * (activation - lowerBound) - decay * (activation - rest );    		
	
	}
//	if(addNoise == true) {
//		val += noiseGenerator.getRandom();
//	}
//	if (clipping == true) {
//		val = clip(val);
//	}

	setBuffer(val);   // TODO Auto-generated method stub

    }

    /**
     * @return Returns the decay.
     */
    public double getDecay() {
        return decay;
    }

    /**
     * @param decay The decay to set.
     */
    public void setDecay(double decay) {
        this.decay = decay;
    }

    /**
     * @return Returns the rest.
     */
    public double getRest() {
        return rest;
    }

    /**
     * @param rest The rest to set.
     */
    public void setRest(double rest) {
        this.rest = rest;
    }
    
    public static String getName() {return "IAC";}

}
