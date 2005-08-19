package org.simnet.synapses.spikeresponders;

import org.simnet.interfaces.SpikeResponse;

public class RiseAndDecay extends SpikeResponse {

    private double maximumResponse = 1;
    private double baseLineResponse = 0;
    private double decayRate = .1;
    
    public SpikeResponse duplicate() {
        // TODO Auto-generated method stub
        return null;
    }

    public void update() {
        // TODO Auto-generated method stub

    }

    /**
     * @return Returns the baseLineResponse.
     */
    public double getBaseLineResponse() {
        return baseLineResponse;
    }

    /**
     * @param baseLineResponse The baseLineResponse to set.
     */
    public void setBaseLineResponse(double baseLineResponse) {
        this.baseLineResponse = baseLineResponse;
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

}
