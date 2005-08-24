package org.simnet.synapses.spikeresponders;

import org.simnet.interfaces.SpikeResponder;
import org.simnet.interfaces.SpikingNeuron;

public class RiseAndDecay extends SpikeResponder {

    private double maximumResponse = 1;
    private double decayRate = .1;
    
    private double timeStep = .01;
    private double recovery = 0;
    
    public SpikeResponder duplicate() {
        // TODO Auto-generated method stub
        return null;
    }

    public void update() {
    	
   		if(((SpikingNeuron)parent.getSource()).hasSpiked() == true) {
   			recovery = 1;
   		} 
   		
   		recovery += (timeStep/decayRate) * (-recovery);
   		value+= (timeStep/decayRate) * (Math.E * maximumResponse * recovery * (1 - value) - value);

    }

    /**
     * @return Returns the timeStep.
     */
    public double getTimeStep() {
        return timeStep;
    }

    /**
     * @param timeStep The timeStep to set.
     */
    public void setTimeStep(double timeStep) {
        this.timeStep = timeStep;
    }

    /**
     * @return Returns the decayRate.
     */
    public double getDecayRate() {
        return decayRate;
    }

    /**
     * @param decayRate The decayRate to set.
     */
    public void setDecayRate(double decayRate) {
        this.decayRate = decayRate;
    }

    /**
     * @return Returns the maximumResponse.
     */
    public double getMaximumResponse() {
        return maximumResponse;
    }

    /**
     * @param maximumResponse The maximumResponse to set.
     */
    public void setMaximumResponse(double maximumResponse) {
        this.maximumResponse = maximumResponse;
    }
    
	public static String getName() {return "Rise and decay";}


}
