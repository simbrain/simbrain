/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.simnet.interfaces;

import java.util.ArrayList;
import java.util.Collection;

//import org.simnet.NeuronLayer;

/**
 * <b>Network</b> provides core neural network functionality and is the
 * the main API for external calls. Network objects are sets of neurons and 
 * weights connecting them. Much of the  actual update and  learning logic
 * occurs (currently) in the individual nodes.
 * 
 */
public abstract class Network {

	//
	// TODO: 	Add a command-line interface for creating and testing simple networks 
	// 			independently of the GUI.
	//
	// TODO: 	Make saving /opening of files possible from here.  Change file format
	//			so that first character indicates what kind of object is being read.
	//

	private ArrayList neuronList = new ArrayList();
	private ArrayList weightList = new ArrayList();
	private ArrayList inputs = new ArrayList();
	private ArrayList outputs = new ArrayList();
	
	private String inputList = new String();
	private String outputList = new String();
	
	protected int time = 0; 	// Keeps track of time
	
	private boolean roundOffActivationValues = true; 	// Whether to round off neuron values
	private int precision = 0; // Degree to which to round off values

	public Collection getNeuronList() {
		return this.neuronList;
	}

	public Collection getWeightList() {
		return this.weightList;
	}

	public int getNeuronCount() {
		return neuronList.size();
	}

	public Neuron getNeuron(int index) {
		return (Neuron) neuronList.get(index);
	}

	public void addNeuron(Neuron neuron) {
		neuronList.add(neuron);		
	}

	public int getWeightCount() {
		return weightList.size();
	}

	public Synapse getWeight(int index) {
		return (Synapse) weightList.get(index);
	}
	
	public int getTime() {
		return time;
	}

	public void setTime(int i) {
		time = i;
	}


	/**
	 * Adds a weight to the neuron network, where that weight already
	 * has designated source and target neurons
	 *
	 * @param weight the weight object to add
	 */
	public void addWeight(Synapse weight) {
		Neuron source = (Neuron) weight.getSource();
		source.addTarget(weight);
		Neuron target = (Neuron) weight.getTarget();
		target.addSource(weight);
		weightList.add(weight);
	}

	/**
	 * Calls {@link Neuron#update} for each neuron
	 */
	public void updateAllNeurons() {
		// First update the activation buffers
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron n = (Neuron) neuronList.get(i);
			n.update();		// update neuron buffers
		}
		// Then update the activations themselves
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron n = (Neuron) neuronList.get(i);
			n.commitBuffer();
		}

	}
	
	/**
	 * Calls {@link Weight#update} for each weight
	 */
	public void updateAllWeights() {
		// No Buffering necessary because the values of weights don't depend on one another
		for (int i = 0; i < weightList.size(); i++) {
			Synapse w = (Synapse) weightList.get(i);
			w.update();
		}

	}
	/**
	 * Calls {@link Neuron#checkBounds} for each neuron, which makes sure the neuron
	 * has not exceeded its upper bound or gone below its lower bound. 
	 * 
	 * TODO: Add or replace with normalization within bounds?
	 */
	public void checkAllBounds() {
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron n = (Neuron) neuronList.get(i);
			n.checkBounds();
		}
		for (int i = 0; i < weightList.size(); i++) {
			Synapse w = (Synapse) weightList.get(i);
			w.checkBounds();
		}		

	}

	/**
	 * Decay each neuron using its decay method
	 * 
	 * @see Neuron#decay()
	 */
	public void decayAll() {
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron temp = (Neuron) neuronList.get(i);
			temp.decay();
		}
	}

	/**
	 * Bias each neuron using its decay method
	 * 
	 * @see Neuron#bias()
	 */
	public void biasAll() {
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron temp = (Neuron) neuronList.get(i);
			temp.bias();
		}
	}	

	//Round activatons off to integers; for testing	
	public void roundAll() {
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron temp = (Neuron) neuronList.get(i);
			temp.round(precision);
		}
	}	
	
	
	/**
	 * Deletes a neuron from the network
	 * 
	 * @param toDelete neuron to delete
	 */
	public void deleteNeuron(Neuron toDelete) {
		if (neuronList.contains(toDelete)) {
			if(inputs.contains(toDelete)) {
				removeInputNeuron(toDelete);
			}
			if(outputs.contains(toDelete)) {
				removeOutputNeuron(toDelete);
			}
			while (toDelete.getFanOut().size() > 0) {
				Synapse w = (Synapse) toDelete.getFanOut().get(0);
				deleteWeight(w);
			}
			toDelete.getFanOut().clear();
			while (toDelete.getFanIn().size() > 0) {
				Synapse w = (Synapse) toDelete.getFanIn().get(0);
				deleteWeight(w);
			}
			toDelete.getFanIn().clear();
			neuronList.remove(toDelete);
		}

	}
	
	/**
	 * Wipe out the whole network
	 */
	public void deleteAllNeurons() {

		weightList.clear();
		neuronList.clear();
		inputs.clear();
		outputs.clear();

	}

	/**
	 * Delete a specified weight 
	 * 
	 * @param toDelete the  Weight to delete
	 */
	public void deleteWeight(Synapse toDelete) {

		toDelete.getSource().getFanOut().remove(toDelete);
		toDelete.getTarget().getFanIn().remove(toDelete);
		weightList.remove(toDelete);
	}

	/**
	 * Set the activation level of all neurons to zero
	 */
	public void setNeuronsToZero() {
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron temp = (Neuron) neuronList.get(i);
			temp.setActivation(0);
		}
	}

	/**
	 * Returns the "state" of the network--the activation level of its neurons.  Used by the
	 * gauge component
	 *
	 * @return an array representing the activation levels of all the neurons in this network
	 */
	public double[] getState() {
		double ret[] = new double[this.getNeuronCount()];

		for (int i = 0; i < this.getNeuronCount(); i++) {
			Neuron n = getNeuron(i);
			ret[i] = (int) n.getActivation();
		}

		return ret;
	}

	/**
	 * Adds the specified neuron to the list of input neurons
	 * 
	 * @param n input neuron to add
	 */
	public void addInputNeuron(Neuron n) {
		inputs.add(n);
	}

	/**
	 * Adds the specified neuron to the list of input neurons
	 * 
	 * @param n input neuron to add
	 */
	public void addOutputNeuron(Neuron n) {
		outputs.add(n);
	}
		
	/**
	 * Removes the specified neuron from the list of input neurons
	 * 
	 * @param n input neuron to remove
	 */
	public void removeInputNeuron(Neuron n) {
		inputs.remove(n);	
	}

	/**
	 * Removes the specified neuron from the list of input neurons
	 * 
	 * @param n input neuron to remove
	 */
	public void removeOutputNeuron(Neuron n) {
		outputs.remove(n);		
	}	
	
	// update input and output lists based on input and output indices of neurons
	// used when reading in savednetworks
	public void updateInOut() {
		inputs.clear();
		outputs.clear();
		
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron temp = (Neuron)neuronList.get(i);
			if (temp.isInput()) {
				addInputNeuron(temp);
			}
			if (temp.isOutput()) {
				addOutputNeuron(temp);
			}
		}
	}

	/**
	 * @return list of output neurons
	 */
	public ArrayList getOutputs() {
		return outputs;
	}

	/**
	 * Same as getOutputs but returns an array of doubles
	 * 
	 * @return array of output neurons
	 */
	public double[] getOutputArray() {
		double[] ret = new double[outputs.size() + 1];
		Neuron temp = null;
		for (int i = 0; i < outputs.size(); i++) {
			temp = (Neuron) outputs.get(i);
			ret[i] = temp.getActivation();
		}

		return ret;
	}

	/**
	 * return a list of input neurons
	 */
	public ArrayList getInputs() {
		return inputs;
	}

	/**
	 * Same as getInputs but returnss an array of doubles
	 * 
	 * @return array of input neurons
	 */
	public double[] getInputsD() {
		double[] ret = new double[inputs.size()];
		Neuron temp = null;
		for (int i = 0; i < inputs.size(); i++) {
			temp = (Neuron) inputs.get(i);
			ret[i] = temp.getActivation();
		}

		return ret;
	}

	/**
	 * Returns the highest input index.  Used when adding new input nodes, so that new indices are created just above the old ones
	 * @return the highest index among the set of input nodes.
	 */
	public int getLargestInputIndex() {
		return inputs.size() + 1;
		
		//TODO: Fix this!
	}		
		
	
	public String getOutputList() {
		String ret = new String("" + ((Neuron)outputs.get(0)).getName());
		for (int i = 1; i < outputs.size(); i++) {
			ret = ret.concat("," + ((Neuron)outputs.get(i)).getName());
		}
		outputList = ret;
		return ret;
	}

	public String getInputList() {
		String ret = new String("" + ((Neuron)inputs.get(0)).getName());
		for (int i = 1; i < inputs.size(); i++) {
			ret = ret.concat("," + ((Neuron)inputs.get(i)).getName());
		}
		inputList = ret;
		return ret;
	}
	
	/**
	 * @param inputList The inputList to set.
	 */
	public void setInputList(String inputList) {
		this.inputList = inputList;
	}
	/**
	 * @param outputList The outputList to set.
	 */
	public void setOutputList(String outputList) {
		this.outputList = outputList;
	}
	/**
	 * Sets the input layer of neurons to values specified by some external source,
	 * currently simulated smell stimuli from the World Component
	 *
	 * @param sensorium external vector of values for the network's "sensorium"
	 * @see org.simbrain.sim.world.World
	 */
	public void setInputs(double[] sensorium) {
		Neuron temp = null;
		double val = 0;

		int num =
			(sensorium.length < inputs.size())
				? sensorium.length
				: inputs.size();

		for (int i = 0; i < num; i++) {
			temp = (Neuron) inputs.get(i);
			val = sensorium[i];
			temp.setActivation(val);
		}
	}

	/**
	 * Sets all weight values to zero, effectively eliminating them
	 */
	public void setWeightsToZero() {
		for (int i = 0; i < weightList.size(); i++) {
			Synapse temp = (Synapse) weightList.get(i);
			temp.setStrength(0);
		}
	}

	/**
	 * Randomizes all neurons.
	 */
	public void randomizeNeurons() {
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron temp = (Neuron) neuronList.get(i);
			temp.randomize();
		}
	}

	/**
	 * Randomizes all weights.
	 */
	public void randomizeWeights() {
		for (int i = 0; i < weightList.size(); i++) {
			Synapse temp = (Synapse) weightList.get(i);
			temp.randomize();
		}
	}

//	/**
//	 * @return an array of strings representing the layers by name
//	 */
//	public String[] getLayers() {
//		String[] retStrings = new String[layerList.size()];
//		for (int i = 0; i < layerList.size(); i++) {
//			retStrings[i] = ((NeuronLayer) layerList.get(i)).getName();
//		}
//		return retStrings;
//	}


//	/**
//	 * Temporary method for testing.  Connects first and second layers in the layerList
//	 * 
//	 * @param values matrix of weight values
//	 */
//	public void connectLayers(double[][] values) {
//		connectLayers(
//			(NeuronLayer) layerList.get(0),
//			(NeuronLayer) layerList.get(1),
//			values);
//	}
//
//	/**
//	 * Connects source and target layers using a two-dimensional matrix of weight values
//	 * 
//	 * @param sourceLayer
//	 * @param targetLayer
//	 * @param connectionMatrix
//	 */
//	public void connectLayers(
//		NeuronLayer sourceLayer,
//		NeuronLayer targetLayer,
//		double[][] connectionMatrix) {
//
//		ArrayList sLayer = sourceLayer.getNeurons();
//		ArrayList tLayer = targetLayer.getNeurons();
//
//		// Validate inputs.  Make sure this matrix can connect these layers
//		if ((sLayer.size() != connectionMatrix.length)
//			|| (tLayer.size() != connectionMatrix[0].length)) {
//			System.out.println(
//				"the matrix does not match the source and target layers");
//			return;
//		}
//
//		for (int i = 0; i < connectionMatrix.length; i++) {
//			for (int j = 0; j < connectionMatrix[i].length; j++) {
//				// check to see if there is already a connection
//				addWeight(
//					new Synapse(
//						(Neuron) sLayer.get(i),
//						(Neuron) tLayer.get(j),
//						connectionMatrix[i][j]));
//			}
//		}
//
//	}
//
//	/**
//	 * Adds a layer of neurons to the neural network
//	 * 
//	 * @param theLayer the layer of neurons to add
//	 */
//	public void addLayer(Collection theLayer) {
//		if (theLayer.size() == 0)
//			return;
//
//		layerList.add(
//			new NeuronLayer(theLayer, new String("" + (layerList.size() + 1))));
//	}
//
//	public ArrayList getLayerList() {
//		return layerList;
//	}
//
//	/**
//	 * Gets a neuron layer by name
//	 * 
//	 * @param layerName name of the layer to get
//	 * @return the neuron layer
//	 */
//	public NeuronLayer getLayer(String layerName) {
//		NeuronLayer ret = null;
//		for (int i = 0; i < layerList.size(); i++) {
//			NeuronLayer currentLayer = (NeuronLayer) layerList.get(i);
//			if (currentLayer.getName().equals(layerName)) {
//				ret = currentLayer;
//				break;
//			}
//		}
//		return ret;
//
//	}


	/**
	 * Round a value off to indicated number of decimal places
	 * 
	 * @param value value to round off
	 * @param decimalPlace degree of precision
	 * @return rounded number
	 */
	 static double round(double value, int decimalPlace) {
		double power_of_ten = 1;
		while (decimalPlace-- > 0)
		   power_of_ten *= 10.0;
		return Math.round(value * power_of_ten) / power_of_ten;
	} 

	/**
	 * Sends relevant information about the network to standard output.
	 */
	public void debug() {

		System.out.println("\nNeurons\n----------");
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron tempRef = (Neuron) neuronList.get(i);
			System.out.println("Neuron [" + i + "]:" + tempRef.getActivation());
		}
		System.out.println("\nWeights\n-----------");
		for (int i = 0; i < weightList.size(); i++) {
			Synapse tempRef = (Synapse) weightList.get(i);
			System.out.print("Weight [" + i + "]: " + tempRef.getStrength() + ".");
			System.out.println(
				"  Connects neuron "
					+ tempRef.getSource().getName()
					+ " to neuron "
					+ tempRef.getTarget().getName());
		}
		System.out.println("\nInputs\n-----------");
		for (int i = 0; i < inputs.size(); i++) {
			Neuron tempRef = (Neuron) inputs.get(i);
			System.out.println("Input [" + i + "]:" + tempRef.getActivation());
		}
		System.out.println("\nOutputs\n-----------");
		for (int i = 0; i < outputs.size(); i++) {
			Neuron tempRef = (Neuron) outputs.get(i);
			System.out.println("Output [" + i + "]:" + tempRef.getActivation());
		}

	}


	public int getPrecision() {
		return precision;
	}

	public boolean isRoundingOff() {
		return roundOffActivationValues;
	}

	public void setPrecision(int i) {
		precision = i;
	}

	public void setRoundingOff(boolean b) {
		roundOffActivationValues = b;
	}
	

	/**
	 * @return Returns the roundOffActivationValues.
	 */
	public boolean isRoundOffActivationValues() {
		return roundOffActivationValues;
	}
	/**
	 * @param roundOffActivationValues The roundOffActivationValues to set.
	 */
	public void setRoundOffActivationValues(boolean roundOffActivationValues) {
		this.roundOffActivationValues = roundOffActivationValues;
	}
	/**
	 * @param inputs The inputs to set.
	 */
	public void setInputs(ArrayList inputs) {
		this.inputs = inputs;
	}
	/**
	 * @param neuronList The neuronList to set.
	 */
	public void setNeuronList(ArrayList neuronList) {
		System.out.println("-->" + neuronList.size());
		this.neuronList = neuronList;
	}

	/**
	 * @param outputs The outputs to set.
	 */
	public void setOutputs(ArrayList outputs) {
		this.outputs = outputs;
	}
	/**
	 * @param weightList The weightList to set.
	 */
	public void setWeightList(ArrayList weightList) {
		this.weightList = weightList;
	}
}
