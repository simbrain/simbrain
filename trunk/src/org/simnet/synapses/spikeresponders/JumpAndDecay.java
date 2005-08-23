package org.simnet.synapses.spikeresponders;

import org.simnet.interfaces.SpikeResponder;
import org.simnet.interfaces.SpikingNeuron;

public class JumpAndDecay extends SpikeResponder {

    private double jumpHeight = 1;
    private double baseLine = 0;
    private double decayRate = .1;
    

    public SpikeResponder duplicate() {
        // TODO Auto-generated method stub
        return null;
    }

    public void update() {

   		if(((SpikingNeuron)parent.getSource()).hasSpiked() == true) {
   			value = jumpHeight;
   		} else {
   			value += decayRate * (baseLine - value);
   		}

    }

    /**
     * @return Returns the baseLine.
     */
    public double getBaseLine() {
        return baseLine;
    }

    /**
     * @param baseLine The baseLine to set.
     */
    public void setBaseLine(double baseLine) {
        this.baseLine = baseLine;
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
     * @return Returns the jumpHeight.
     */
    public double getJumpHeight() {
        return jumpHeight;
    }

    /**
     * @param jumpHeight The jumpHeight to set.
     */
    public void setJumpHeight(double jumpHeight) {
        this.jumpHeight = jumpHeight;
    }

	public static String getName() {return "Jump and decay";}

}
