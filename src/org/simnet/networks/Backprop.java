/*
 * Created on Sep 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simnet.networks;

import org.simnet.interfaces.ActivationRule;
import org.simnet.interfaces.ComplexNetwork;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.neurons.StandardNeuron;
import org.simnet.util.ConnectNets;

import edu.wlu.cs.levy.SNARLI.BPLayer;

/**
 * @author yoshimi
 *
 */
public class Backprop extends ComplexNetwork {
	
	private int n_inputs = 5;
	private int n_hidden = 5;
	private int n_outputs = 5;
	private int epochs = 1000;
	private double error = 0;
	private double eta = .5;
	private double mu = .1;
	private int error_interval = 100;
	private double[][] training_inputs = null;
	private double[][] training_outputs = null;
    private BPLayer inp, hid, out;
	
	public Backprop() {
		
	}
	
	public void defaultInit() {
		
        addNetwork(new StandardNetwork(n_inputs)); 
        setRules((StandardNetwork)getNetwork(0), "Clamped");
        addNetwork(new StandardNetwork(n_hidden));
        setRules((StandardNetwork)getNetwork(1), "Sigmoidal");
        addNetwork(new StandardNetwork(n_outputs));
        setRules((StandardNetwork)getNetwork(2), "Sigmoidal");
        ConnectNets.oneWayFull(this, getNetwork(0), getNetwork(1));
        ConnectNets.oneWayFull(this, getNetwork(1), getNetwork(2));    
        
    		for (int i = 0; i < getFlatSynapseList().size(); i++) {
    			((Synapse)getFlatSynapseList().get(i)).setUpperBound(100);
    			((Synapse)getFlatSynapseList().get(i)).setLowerBound(-100);	
    		}

       	for (int i = 0; i < getFlatNeuronList().size(); i++) {
			((Neuron)getFlatNeuronList().get(i)).setUpperBound(1);	
			((Neuron)getFlatNeuronList().get(i)).setLowerBound(-1);	
    			((Neuron)getFlatNeuronList().get(i)).setIncrement(1);	
    		}

    	
        
	}
	/**
	 * The core update function of the neural network.  Calls
	 * the current update function on each neuron,
	 * decays all the neurons, and checks their bounds. 
	 */
	public void update() {
		time++;
		updateAllNetworks();
		checkAllBounds();		
	}
	
	
	public void train() {

		buildSnarliNetwork();
		attachInputsAndOutputs();

		batchTrain();
		
		buildSimbrainNetwork();
		
	}
	
	public void iterate() {

		buildSnarliNetwork();
		attachInputsAndOutputs();

		batchIterate();
		
		buildSimbrainNetwork();
		
	}
	
	public void buildSnarliNetwork(){
        inp = new BPLayer(getNetwork(0).getNeuronCount());
        hid = new BPLayer(getNetwork(1).getNeuronCount());
        out = new BPLayer(getNetwork(2).getNeuronCount());
        hid.connect(inp);
        out.connect(hid);	
        hid.setWeights(inp, ConnectNets.getWeights(getNetwork(0), getNetwork(1)));
        hid.setBias(getBiases((StandardNetwork)getNetwork(1)));
		out.setWeights(hid, ConnectNets.getWeights(getNetwork(1), getNetwork(2)));		
		out.setBias(getBiases((StandardNetwork)getNetwork(2)));
		
	}
	
	public void attachInputsAndOutputs(){
		if(training_inputs == null || training_outputs==null) {
			return;
		}

		inp.attach(training_inputs);
		out.attach(training_outputs);
	}
	
	public void buildSimbrainNetwork(){
		ConnectNets.setConnections(this, getNetwork(0), hid.getWeights(inp));
		ConnectNets.setConnections(this, getNetwork(1), out.getWeights(hid));		
		setBiases((StandardNetwork)getNetwork(1), hid.getBias());
		setBiases((StandardNetwork)getNetwork(2), out.getBias());
	}
	
	public void batchTrain(){
		out.batch(epochs, eta, mu, error_interval);
	}
	
	public void batchIterate(){
		out.getRMSError();
		out.batch(1,eta,mu, error_interval);
	}

	public void randomize() {
		if (hid == null || out == null) {
	        inp = new BPLayer(getNetwork(0).getNeuronCount());
	        hid = new BPLayer(getNetwork(1).getNeuronCount());
	        out = new BPLayer(getNetwork(2).getNeuronCount());
	        hid.connect(inp);
	        out.connect(hid);	
		}
		if( this.getNetworkList().size() == 0) {
			return;
		}
		hid.randomize();
		out.randomize();
		ConnectNets.setConnections(this, getNetwork(0), hid.getWeights(inp));
		ConnectNets.setConnections(this, getNetwork(1), out.getWeights(hid));		
		setBiases((StandardNetwork)getNetwork(1), hid.getBias());
		setBiases((StandardNetwork)getNetwork(2), out.getBias());	
	}
	
	/**
	 * @return Returns the epochs.
	 */
	public int getEpochs() {
		return epochs;
	}
	/**
	 * @param epochs The epochs to set.
	 */
	public void setEpochs(int epochs) {
		this.epochs = epochs;
	}
	/**
	 * @return Returns the error.
	 */
	public double getError() {
		return error;
	}
	/**
	 * @param error The error to set.
	 */
	public void setError(double error) {
		this.error = error;
	}
	/**
	 * @return Returns the error_interval.
	 */
	public int getError_interval() {
		return error_interval;
	}
	/**
	 * @param error_interval The error_interval to set.
	 */
	public void setError_interval(int error_interval) {
		this.error_interval = error_interval;
	}
	/**
	 * @return Returns the eta.
	 */
	public double getEta() {
		return eta;
	}
	/**
	 * @param eta The eta to set.
	 */
	public void setEta(double eta) {
		this.eta = eta;
	}
	/**
	 * @return Returns the hid.
	 */
	public BPLayer getHid() {
		return hid;
	}
	/**
	 * @param hid The hid to set.
	 */
	public void setHid(BPLayer hid) {
		this.hid = hid;
	}
	/**
	 * @return Returns the inp.
	 */
	public BPLayer getInp() {
		return inp;
	}
	/**
	 * @param inp The inp to set.
	 */
	public void setInp(BPLayer inp) {
		this.inp = inp;
	}
	/**
	 * @return Returns the mu.
	 */
	public double getMu() {
		return mu;
	}
	/**
	 * @param mu The mu to set.
	 */
	public void setMu(double mu) {
		this.mu = mu;
	}
	/**
	 * @return Returns the n_hidden.
	 */
	public int getN_hidden() {
		return n_hidden;
	}
	/**
	 * @param n_hidden The n_hidden to set.
	 */
	public void setN_hidden(int n_hidden) {
		this.n_hidden = n_hidden;
	}
	/**
	 * @return Returns the n_inputs.
	 */
	public int getN_inputs() {
		return n_inputs;
	}
	/**
	 * @param n_inputs The n_inputs to set.
	 */
	public void setN_inputs(int n_inputs) {
		this.n_inputs = n_inputs;
	}
	/**
	 * @return Returns the n_outputs.
	 */
	public int getN_outputs() {
		return n_outputs;
	}
	/**
	 * @param n_outputs The n_outputs to set.
	 */
	public void setN_outputs(int n_outputs) {
		this.n_outputs = n_outputs;
	}
	/**
	 * @return Returns the out.
	 */
	public BPLayer getOut() {
		return out;
	}
	/**
	 * @param out The out to set.
	 */
	public void setOut(BPLayer out) {
		this.out = out;
	}
	
	

	/**
	 * @return Returns the training_inputs.
	 */
	public double[][] getTraining_inputs() {
		return training_inputs;
	}
	/**
	 * @param training_inputs The training_inputs to set.
	 */
	public void setTraining_inputs(double[][] training_inputs) {
		this.training_inputs = training_inputs;
	}
	/**
	 * @return Returns the training_outputs.
	 */
	public double[][] getTraining_outputs() {
		return training_outputs;
	}
	/**
	 * @param training_outputs The training_outputs to set.
	 */
	public void setTraining_outputs(double[][] training_outputs) {
		this.training_outputs = training_outputs;
	}
	
	public double[] getBiases(StandardNetwork net) {
		double[] ret = new double[net.getNeuronCount()];
		for (int i = 0; i < net.getNeuronCount(); i++) {
			ret[i] = ((StandardNeuron)net.getNeuron(i)).getBias();
		}
		return ret;
		
	}
	
	/**
	 * Set bias values for all neurons in this network
	 * 
	 * @param biases array of new bias values
	 */
	public void setBiases(StandardNetwork net, double[] biases) {
		if (biases.length != net.getNeuronCount()) {
			System.out.println("Invalid argument to setBiases");
			return;
		}
		
		for (int i = 0; i < net.getNeuronCount(); i++) {
			((StandardNeuron)net.getNeuron(i)).setBias(biases[i]);
		}
	}
	
	/**
	 * Set activation rule for every neuron in the network
	 * 
	 * @param rule the name of the rule to set the neurons to
	 */
	public void setRules(StandardNetwork net, String rule) {
		for (int i = 0; i < net.getNeuronCount(); i++) {
			((StandardNeuron)net.getNeuron(i)).setActivationFunction(ActivationRule.getActivationFunction(rule));
		}		
	}
}
