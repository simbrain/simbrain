/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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

import java.util.ArrayList;

import org.simnet.NetworkPreferences;
import org.simnet.interfaces.*;
import org.simnet.neurons.rules.*;

public class BinaryNeuron extends Neuron{
	
	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public BinaryNeuron() {
		this.setUpperBound(1);
		this.setLowerBound(-1);
	}
	
	public Neuron duplicate() {
		return super.duplicate(this);
	}
	
	public void update() {
		activationFunction.apply(this);
		commitBuffer();
		if(activation > 0) {
			setBuffer(1);
		}
		else setBuffer(-1);
	}

}