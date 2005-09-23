package org.simnet.neurons;

import org.simnet.interfaces.Neuron;
import org.simnet.util.RandomSource;

/**
 * 
 * <b>NakaRushtonNeuron</b>
 */
public class NakaRushtonNeuron extends Neuron {

    private double maximumSpikeRate = 10;
    private double steepness = 1;
    private double semiSaturationConstant = 5;
    private double timeConstant = .1;
    private RandomSource noiseGenerator = new RandomSource();
    private boolean addNoise = false;

    public NakaRushtonNeuron(){
    	init();
    }
    
	public int getTimeType() {
		return org.simnet.interfaces.Network.CONTINUOUS;
	}

    
    public NakaRushtonNeuron(Neuron n){
        super(n);
        init();
    }

    public void init() {
        upperBound = maximumSpikeRate;
        lowerBound = 0;    	
    }
 
    public Neuron duplicate() {
        NakaRushtonNeuron rn = new NakaRushtonNeuron();
        rn = (NakaRushtonNeuron)super.duplicate(rn);
        rn.setMaximumSpikeRate(getMaximumSpikeRate());
        rn.setSteepness(getSteepness());
        rn.setSemiSaturationConstant(getSemiSaturationConstant());
        rn.setAddNoise(getAddNoise());
        rn.noiseGenerator = noiseGenerator.duplicate(noiseGenerator);
        return rn;
    }

    /**
     * See Spikes (Hugh Wilson), pp. 20-21
     */
    public void update() {
    		double P = weightedInputs();
    		double S = 0;
    		
    		if (P > 0) {
    			S = (maximumSpikeRate * Math.pow(P, steepness))/
				(Math.pow(semiSaturationConstant, steepness) + Math.pow(P, steepness));
    		}
    		
    		double val = getActivation();

    		if(addNoise == true) {
        		val +=  this.getParentNetwork().getTimeStep() * ((1/timeConstant) * (-val + S) + noiseGenerator.getRandom());
    		} else {
        		val +=  this.getParentNetwork().getTimeStep() * ((1/timeConstant) * (-val + S));
    		}

    		setBuffer(val);
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
        init();
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

    public static String getName() {return "Naka-Rushton";}

    /**
     * @return Returns the addNoise.
     */
    public boolean getAddNoise() {
        return addNoise;
    }

    /**
     * @param addNoise The addNoise to set.
     */
    public void setAddNoise(boolean addNoise) {
        this.addNoise = addNoise;
    }

    /**
     * @return Returns the noiseGenerator.
     */
    public RandomSource getNoiseGenerator() {
        return noiseGenerator;
    }

    /**
     * @param noiseGenerator The noiseGenerator to set.
     */
    public void setNoiseGenerator(RandomSource noiseGenerator) {
        this.noiseGenerator = noiseGenerator;
    }
}
