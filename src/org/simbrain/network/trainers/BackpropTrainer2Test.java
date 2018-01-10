package org.simbrain.network.trainers;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;

import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.subnetworks.BackpropNetwork;

public class BackpropTrainer2Test {

	public static void main(String[] args) {
		int noHid = 5;
		int noOut = 1;
		int noInp = 1;
		BackpropNetwork network = new BackpropNetwork(new Network(),
                new int[] { noInp, noHid, noOut });
		
		// Set and collect the values of 
		double str = 0.1;
		double[][] hidOutStrs = new double[noHid][noOut];
		int row = 0, col = 0;
		for(Neuron n : network.getOutputLayer().getNeuronListUnsafe()) {
			row=0;
			for(Synapse s : n.getFanIn()) {
				s.forceSetStrength(str);
				
				hidOutStrs[row][col] = str;
				row++;		
				str += 0.1;
			}
			col++;
		}

		str = 0.05;
		double[][] inpHidStrs = new double[noHid][noOut];
		row = 0;
		col = 0;
		for(Neuron n : network.getHiddenLayer().getNeuronListUnsafe()) {
			row=0;
			for(Synapse s : n.getFanIn()) {
				s.forceSetStrength(str);
				inpHidStrs[row][col] = str;
				row++;		
				str += 0.1;
			}
			col++;
		}
		double biases = 0;
		for(Neuron n : network.getOutputLayer().getNeuronListUnsafe()) {
			((SigmoidalRule)n.getUpdateRule()).setBias(0.314);
			biases += 0.314;
		}
		
		
		biases = 0.11;
		for(Neuron n : network.getHiddenLayer().getNeuronList()) {
			((SigmoidalRule)n.getUpdateRule()).setBias(biases);
			biases += 0.1;
		}
		
		BackpropTrainer2 trainer = new BackpropTrainer2(network);
		double [][] hidOutJBlasMat = trainer.getWeightMatrices().get(1).toArray2();
		printArray2(hidOutJBlasMat);
		System.out.println();
		printArray2(hidOutStrs);
	}
	
	public static void printArray2(double [][] arr2) {
		System.out.print("[ ");
		for(int ii=0; ii< arr2.length; ++ii) {
			if (ii < arr2.length-1) {
				System.out.println(Arrays.toString(arr2[ii]));
			} else {
				System.out.print(Arrays.toString(arr2[ii]));
			}
		}
		System.out.println(" ]");
	}
	
	@Test
	public void testCreation() {
		int noHid = 5;
		int noOut = 1;
		int noInp = 1;
		BackpropNetwork network = new BackpropNetwork(new Network(),
                new int[] { noInp, noHid, noOut });
		
		// Set and collect the values of 
		double str = 0.1;
		double[][] hidOutStrs = new double[noHid][noOut];
		int row = 0, col = 0;
		for(Neuron n : network.getOutputLayer().getNeuronListUnsafe()) {
			row=0;
			for(Synapse s : n.getFanIn()) {
				s.forceSetStrength(str);
				
				hidOutStrs[row][col] = str;
				row++;		
				str += 0.1;
			}
			col++;
		}

		str = 0.05;
		double[][] inpHidStrs = new double[noHid][noOut];
		row = 0;
		col = 0;
		for(Neuron n : network.getHiddenLayer().getNeuronListUnsafe()) {
			row=0;
			for(Synapse s : n.getFanIn()) {
				s.forceSetStrength(str);
				inpHidStrs[row][col] = str;
				row++;		
				str += 0.1;
			}
			col++;
		}
		double biases = 0;
		for(Neuron n : network.getOutputLayer().getNeuronListUnsafe()) {
			((SigmoidalRule)n.getUpdateRule()).setBias(0.314);
			biases += 0.314;
		}
		
		
		biases = 0.11;
		for(Neuron n : network.getHiddenLayer().getNeuronList()) {
			((SigmoidalRule)n.getUpdateRule()).setBias(biases);
			biases += 0.1;
		}
		
		BackpropTrainer2 trainer = new BackpropTrainer2(network);
		double [][] hidOutJBlasMat = trainer.getWeightMatrices().get(0).toArray2();
		for(int ii=0; ii<noHid; ++ii) {
			assertArrayEquals(hidOutJBlasMat[ii], hidOutStrs[ii], 0);
		}
		
	}
	
	
}
