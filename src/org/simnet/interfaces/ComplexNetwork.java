/*
 * Created on Sep 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simnet.interfaces;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Networks which contain lists of sub-networks, e.g. backprop, where the subnetworks
 * are "layers"
 *
 */
public abstract class ComplexNetwork extends Network {
	
	protected ArrayList networkList = new ArrayList();
	
	public void init() {
		super.init();
		for (int i = 0; i < networkList.size(); i++) {
			((Network)networkList.get(i)).init();
		}
	}
	
	/**
	 * The core update function of the neural network.  Calls
	 * the current update function on each neuron,
	 * decays all the neurons, and checks their bounds. 
	 */
	public void update() {
		updateAllNetworks();	
	}
	
	public void updateAllNetworks() {
		Iterator i = networkList.iterator();
		while(i.hasNext()) {
			((Network)i.next()).update();
		}
	}
	public void addNetwork(Network n) {
		networkList.add(n);
	}
	public Network getNetwork(int i) {
		return (Network)networkList.get(i);
	}
	
	public void debug() {
		super.debug();
		for(int i = 0; i < networkList.size(); i++) {
			Network net = (Network)networkList.get(i);
			if (net instanceof ComplexNetwork) {
				((ComplexNetwork)net).debug();
			} else {
				net.debug();
			}
		}
	}

	
	/**
	 * @return Returns the networkList.
	 */
	public ArrayList getNetworkList() {
		return networkList;
	}
	
	/**
	 * @param networkList The networkList to set.
	 */
	public void setNetworkList(ArrayList networkList) {
		this.networkList = networkList;
	}
	
	/**
	 * Used before marshalling, so that all neurons have a unique id
	 */
	public void updateIds() {
		ArrayList flatNeuronList = getFlatNeuronList();
		for(int i = 0; i < flatNeuronList.size(); i++) {
			((Neuron)flatNeuronList.get(i)).setId("n" + i);
		}
		ArrayList flatSynapseList = getFlatSynapseList();
		for(int i = 0; i < flatSynapseList.size(); i++) {
			((Synapse)flatSynapseList.get(i)).setId("w" + i);
		}

	}

	
	/**
	 * return a list of input neurons
	 */
	public ArrayList getInputs() {
		ArrayList ret = inputList;
		for(int i = 0; i < networkList.size(); i++) {
			Network net = (Network)networkList.get(i);
			ArrayList toAdd;
			if (net instanceof ComplexNetwork) {
				toAdd = (ArrayList)((ComplexNetwork)net).getInputs();
			} else {
				toAdd = (ArrayList)((Network)networkList.get(i)).getInputs();
			}
			ret.addAll(toAdd);
		}
		return ret;
			
	}
	
	/**
	 * Create "flat" list of neurons, which includes the top-level neurons plus all subnet neurons
	 *
	 * @return the flat llist
	 */
	public ArrayList getFlatNeuronList() {
		ArrayList ret = new ArrayList();
		ret.addAll(neuronList);
		for(int i = 0; i < networkList.size(); i++) {
			Network net = (Network)networkList.get(i);
			ArrayList toAdd;
			if (net instanceof ComplexNetwork) {
				toAdd = (ArrayList)((ComplexNetwork)net).getFlatNeuronList();
			} else {
				toAdd = (ArrayList)((Network)networkList.get(i)).getNeuronList();
			}
			ret.addAll(toAdd);
		}
		return ret;
	}
	
	/**
	 * Create "flat" list of synapses, which includes the top-level synapses plus all subnet synapses
	 *
	 * @return the flat list
	 */
	public ArrayList getFlatSynapseList() {
		ArrayList ret = new ArrayList();
		ret.addAll(weightList);
		for(int i = 0; i < networkList.size(); i++) {
			Network net = (Network)networkList.get(i);
			ArrayList toAdd;
			if (net instanceof ComplexNetwork) {
				toAdd = (ArrayList)((ComplexNetwork)net).getFlatSynapseList();
			} else {
				toAdd = (ArrayList)((Network)networkList.get(i)).getWeightList();
			}
			ret.addAll(toAdd);
		}
		return ret;
	}
	
//	public void deleteNeuron(Neuron n) {
//		super.deleteNeuron(n);
//		
//	}
}
