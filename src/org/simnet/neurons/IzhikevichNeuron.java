package org.simnet.neurons;

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.SpikingNeuron;

public class IzhikevichNeuron extends Neuron implements SpikingNeuron {

	private boolean hasSpiked = false;

	private double recovery = 0;
	
    private double a = .2;
    private double b = 2;
    private double c = -56;
    private double d = -16;
    private double timeStep = .1;
    
    public IzhikevichNeuron(){
        
    }
    
    public IzhikevichNeuron(Neuron n){
        super(n);
    }
    public Neuron duplicate() {
        // TODO Auto-generated method stub
        return null;
    }

    public void update() {
    		
    		recovery += timeStep * (a * (b * activation - recovery));
    		double val = activation + timeStep * 
				((.04 * (activation * activation)) +
				 (5 * activation) + 140 - recovery + weightedInputs());	
    	
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

}
