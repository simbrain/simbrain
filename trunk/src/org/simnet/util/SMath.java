/*
 * Created on Oct 3, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simnet.util;



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
