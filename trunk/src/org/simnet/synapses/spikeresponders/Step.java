/*
 * Created on Aug 18, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.simnet.synapses.spikeresponders;

import org.simnet.interfaces.SpikeResponse;

/**
 * @author jyoshimi
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Step extends SpikeResponse {

	private double responseHeight = 1;
	private double responseTime = 1;
	
	   public SpikeResponse duplicate() {
	   	 return null;
	   }
	   
	   public void update() {
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
