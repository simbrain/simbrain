/*
 * Created on Aug 18, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.simnet.synapses.spikeresponders;

import org.simnet.interfaces.SpikeResponder;
import org.simnet.interfaces.SpikingNeuron;

/**
 * @author jyoshimi
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Step extends SpikeResponder {

	private double timer = 0;
	private double responseHeight = 1;
	private double responseTime = 1;
	
	   public SpikeResponder duplicate() {
	   	 return null;
	   }
	   
	   public void update() {
	    		
	   		if(((SpikingNeuron)parent.getSource()).hasSpiked() == true) {
	   			timer = responseTime;
	   		} else {
	   			timer--;
	   			if (timer < 0) timer = 0;
	   		}
	   		
	   		if (timer > 0) {
	   			value = responseHeight;
	   		} else {
	   			value = 0;
	   		}
	   }
	    
	    
	/**
	 * @return Returns the responseHeight.
	 */
	public double getResponseHeight() {
		return responseHeight;
	}
	/**
	 * @param responseHeight The responseHeight to set.
	 */
	public void setResponseHeight(double responseHeight) {
		this.responseHeight = responseHeight;
	}
	/**
	 * @return Returns the responseTime.
	 */
	public double getResponseTime() {
		return responseTime;
	}
	/**
	 * @param responseTime The responseTime to set.
	 */
	public void setResponseTime(double responseTime) {
		this.responseTime = responseTime;
	}
	
	public static String getName() {return "Step";}

}
