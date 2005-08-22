package org.simnet.neurons;

import org.simnet.interfaces.Neuron;

public class IzhikevichNeuron extends Neuron {

    private double a = 0;
    private double b = 0;
    private double c = 0;
    private double d = 0;
    
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
        // TODO Auto-generated method stub

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
