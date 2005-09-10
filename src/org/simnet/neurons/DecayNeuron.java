package org.simnet.neurons;

import org.simnet.interfaces.Neuron;
import org.simnet.util.RandomSource;

public class DecayNeuron extends Neuron {
    
	private static final int RELATIVE = 0;
	private static final int ABSOLUTE = 1;
    private int relAbs = RELATIVE;
    private double decayAmount = 0;
    private double decayFraction = 0;
    private double baseLine = 0;
    private boolean clipping = true;
    private RandomSource noiseGenerator = new RandomSource();
    private boolean addNoise = false;
    
    public DecayNeuron(){
        
    }
    
    public DecayNeuron(Neuron n){
        super(n);
    }

    public Neuron duplicate() {

        DecayNeuron dn = new DecayNeuron();
        dn = (DecayNeuron)super.duplicate(dn);
        dn.setRelAbs(getRelAbs());
        dn.setDecayAmount(getDecayAmount());
        dn.setDecayFraction(getDecayFraction());
        dn.setClipping(getClipping());
        dn.setAddNoise(getAddNoise());
        dn.noiseGenerator = noiseGenerator.duplicate(noiseGenerator);
        return dn;
    }

    public void update() {
    	
		double val = activation + this.weightedInputs();
		double decayVal = 0;
		
    		if (relAbs == RELATIVE) {
    			decayVal = decayFraction * Math.abs(val - baseLine);
    		} else if (relAbs == ABSOLUTE) {
    			decayVal = decayAmount;
    		}

		// Here's where the action happens
		if (val < baseLine) {
			val += decayVal;
			if (val > baseLine) val = baseLine;
		} else if (val > baseLine) {
			val -= decayVal;
			if (val < baseLine) val = baseLine;
		}
		
		if(addNoise == true) {
			val += noiseGenerator.getRandom();
		}
		if (clipping == true) {
			val = clip(val);
		}

		setBuffer(val);
    }
    
    public static String getName() {return "Decay";}

    /**
     * @return Returns the decayAmount.
     */
    public double getDecayAmount() {
        return decayAmount;
    }

    /**
     * @param decayAmount The decayAmount to set.
     */
    public void setDecayAmount(double decayAmount) {
        this.decayAmount = decayAmount;
    }

    /**
     * @return Returns the dedayPercentage.
     */
    public double getDecayFraction() {
        return decayFraction;
    }

    /**
     * @param dedayPercentage The dedayPercentage to set.
     */
    public void setDecayFraction(double decayFraction) {
        this.decayFraction = decayFraction;
    }

    /**
     * @return Returns the relAbs.
     */
    public int getRelAbs() {
        return relAbs;
    }

    /**
     * @param relAbs The relAbs to set.
     */
    public void setRelAbs(int relAbs) {
        this.relAbs = relAbs;
    }

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
     * @return Returns the clipping.
     */
    public boolean getClipping() {
        return clipping;
    }

    /**
     * @param clipping The clipping to set.
     */
    public void setClipping(boolean clipping) {
        this.clipping = clipping;
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
