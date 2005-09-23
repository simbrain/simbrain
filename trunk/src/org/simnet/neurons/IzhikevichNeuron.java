package org.simnet.neurons;

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.SpikingNeuron;
import org.simnet.util.RandomSource;

/**
 * 
 * <b>IzhikevichNeuron</b>
 */
public class IzhikevichNeuron extends Neuron implements SpikingNeuron {

	private boolean hasSpiked = false;

	private double recovery = 0;
	
    private double a = .2;
    private double b = 2;
    private double c = -56;
    private double d = -16;
    
    private RandomSource noiseGenerator = new RandomSource();
    private boolean addNoise = false;
    
    public IzhikevichNeuron(){
        
    }
    
	public int getTimeType() {
		return org.simnet.interfaces.Network.CONTINUOUS;
	}

    
    public IzhikevichNeuron(Neuron n){
        super(n);
    }
    public Neuron duplicate() {
        IzhikevichNeuron in = new IzhikevichNeuron();
        in = (IzhikevichNeuron)super.duplicate(in);
        in.setA(getA());
        in.setB(getB());
        in.setC(getC());
        in.setD(getD());
        in.setAddNoise(getAddNoise());
        in.noiseGenerator = noiseGenerator.duplicate(noiseGenerator);
        return in;
    }

    public void update() {
    		double timeStep = this.getParentNetwork().getTimeStep();
    		double inputs = weightedInputs();
    		
    		if(addNoise == true) {
    			inputs += noiseGenerator.getRandom();
    		}
    		
    		recovery += timeStep * (a * (b * activation - recovery));
    		double val = activation + timeStep * 
				((.04 * (activation * activation)) +
				 (5 * activation) + 140 - recovery +inputs);	
    		
    	
    		if (val > 30) {
    			val = c;
    			recovery += d;
    			hasSpiked = true;
    		} else {
    			hasSpiked = false;
    		}
    
    		setBuffer(val);
    }

	public boolean hasSpiked() {
		return hasSpiked;
	}

    /**
     * @return Returns the a.
     */
    public double getA() {
        return a;
    }

    /**
     * @param a The a to set.
     */
    public void setA(double a) {
        this.a = a;
    }

    /**
     * @return Returns the b.
     */
    public double getB() {
        return b;
    }

    /**
     * @param b The b to set.
     */
    public void setB(double b) {
        this.b = b;
    }

    /**
     * @return Returns the c.
     */
    public double getC() {
        return c;
    }

    /**
     * @param c The c to set.
     */
    public void setC(double c) {
        this.c = c;
    }

    /**
     * @return Returns the d.
     */
    public double getD() {
        return d;
    }

    /**
     * @param d The d to set.
     */
    public void setD(double d) {
        this.d = d;
    }
    
    public static String getName() {return "Izhikevich";}

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
