package org.simnet.neurons;

import org.simnet.interfaces.Neuron;

public class NakaRushtonNeuron extends Neuron {

    private double maximumSpikeRate = 0;
    private double steepness = 0;
    private double semiSaturationConstant = 0;
    private double timeConstant = 0;
    
    public NakaRushtonNeuron(){
        
    }
    
    public NakaRushtonNeuron(Neuron n){
        super(n);
    }
    
    public Neuron duplicate() {
        // TODO Auto-generated method stub
        return null;
    }

    public void update() {
        // TODO Auto-generated method stub

    }

    /**
     * @return Returns the maximumSpikeRate.
     */
    public double getMaximumSpikeRate() {
        return maximumSpikeRate;
    }

    /**
     * @param maximumSpikeRate The maximumSpikeRate to set.
     */
    public void setMaximumSpikeRate(double maximumSpikeRate) {
        this.maximumSpikeRate = maximumSpikeRate;
    }

    /**
     * @return Returns the semiSaturationConstant.
     */
    public double getSemiSaturationConstant() {
        return semiSaturationConstant;
    }

    /**
     * @param semiSaturationConstant The semiSaturationConstant to set.
     */
    public void setSemiSaturationConstant(double semiSaturationConstant) {
        this.semiSaturationConstant = semiSaturationConstant;
    }

    /**
     * @return Returns the steepness.
     */
    public double getSteepness() {
        return steepness;
    }

    /**
     * @param steepness The steepness to set.
     */
    public void setSteepness(double steepness) {
        this.steepness = steepness;
    }

    /**
     * @return Returns the timeConstant.
     */
    public double getTimeConstant() {
        return timeConstant;
    }

    /**
     * @param timeConstant The timeConstant to set.
     */
    public void setTimeConstant(double timeConstant) {
        this.timeConstant = timeConstant;
    }

    public static String getName() {return "NakaRushton";}
}
