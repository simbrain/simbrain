/*
 * Created on Oct 3, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simnet.util;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.networks.Backprop;
import org.simnet.synapses.StandardSynapse;


/**
 * Simbrain mathematics methods
 */
public class SMath {
	
	
	public static double tanh(double input, double lambda) {
		input = input * lambda;
		return (Math.exp(input) - Math.exp(-input)) / (Math.exp(input) + Math.exp(-input));
	}
	
	public static double arctan(double input, double lambda) {
		input = input * lambda;
		return (Math.atan(input));
	}
	

}
