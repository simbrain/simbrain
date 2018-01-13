package org.simbrain.network.trainers;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.List;

import org.jblas.DoubleMatrix;
import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.network.trainers.BackpropTrainer2.UpdateMethod;

public class BackpropTrainer2Test {

	@Test
	public void testCreation() {
		int noOut = 1;
		int noHid = 2;
		int noInp = 2;
		BackpropNetwork network = new BackpropNetwork(new Network(),
				new int[] { noInp, noHid, noOut });

		double str = 0.1;
		double[][] hidOutStrs = new double[noHid][noOut];
		int row = 0, col = 0;
		for (Neuron n : network.getOutputLayer().getNeuronListUnsafe()) {
			row = 0;
			for (Synapse s : n.getFanIn()) {
				s.forceSetStrength(str);
				hidOutStrs[row][col] = str;
				row++;
				str += 0.1;
			}
			col++;
		}

		str = 0.05;
		double[][] inpHidStrs = new double[noInp][noHid];
		row = 0;
		col = 0;
		for (Neuron n : network.getHiddenLayer().getNeuronListUnsafe()) {
			row = 0;
			for (Synapse s : n.getFanIn()) {
				s.forceSetStrength(str);
				inpHidStrs[row][col] = str;
				row++;
				str += 0.1;
			}
			col++;
		}

		double biases = 0;
		double [] outBiases = new double[noOut];
		int jj = 0;
		for (Neuron n : network.getOutputLayer().getNeuronListUnsafe()) {
			((SigmoidalRule) n.getUpdateRule()).setBias(0.314);
			biases += 0.314;
			outBiases[jj++] = ((SigmoidalRule) n.getUpdateRule()).getBias();
		}

		biases = 0.11;
		jj=0;
		double [] hidBiases = new double[noHid];
		for (Neuron n : network.getHiddenLayer().getNeuronList()) {
			((SigmoidalRule) n.getUpdateRule()).setBias(biases);
			biases += 0.1;
			hidBiases[jj++] = ((SigmoidalRule) n.getUpdateRule()).getBias();
		}

		BackpropTrainer2 trainer = new BackpropTrainer2(network);

		//        trainer.printDebugInfo();
		//         System.out.println(Arrays.deepToString(inpHidStrs));
		//         System.out.println();
		//         System.out.println(Arrays.deepToString(hidOutStrs));

		double[][] inHidJBlas = trainer.getWeightMatrices().get(0).transpose()
				.toArray2();
		System.out.println(Arrays.deepToString(inpHidStrs));
		System.out.println();
		System.out.println(Arrays.deepToString(inHidJBlas));
		System.out.println("-----");
		double[][] hidOutJBlas = trainer.getWeightMatrices().get(1).transpose()
				.toArray2();
		System.out.println(Arrays.deepToString(hidOutStrs));
		System.out.println();
		System.out.println(Arrays.deepToString(hidOutJBlas));
		System.out.println();
		System.out.println("-----");
		System.out.println();


		//  
		for (int ii = 0; ii < noInp; ++ii) {
			assertArrayEquals(inHidJBlas[ii], inpHidStrs[ii], 0);
		}
		for (int ii = 0; ii < noHid; ++ii) {
			assertArrayEquals(hidOutJBlas[ii], hidOutStrs[ii], 0);
		}
		

		
		List<DoubleMatrix> jblasBiases = trainer.getBiases();
		double [] biasesOutJBlas = jblasBiases.get(1).data;
		double [] biasesHidJBlas = jblasBiases.get(0).data;
		
		assertArrayEquals(biasesHidJBlas, hidBiases, 0);
		assertArrayEquals(biasesOutJBlas, outBiases, 0);
		
	}

	@Test
	public void testCopyBack() {
		int noOut = 1;
		int noHid = 2;
		int noInp = 2;
		BackpropNetwork network = new BackpropNetwork(new Network(),
				new int[] { noInp, noHid, noOut });
        network.getTrainingSet().setInputData(
                new double[][] { { 0, 0 }, { 0, 1 }, { 1, 0 }, { 1, 1 } });
        network.getTrainingSet()
                .setTargetData(new double[][] { { 0 }, { 1 }, { 1 }, { 0 } });
		double str = 0.1;
		for (Neuron n : network.getOutputLayer().getNeuronListUnsafe()) {
			for (Synapse s : n.getFanIn()) {
				s.forceSetStrength(str);
				str += 0.1;
			}
		}

		str = 0.05;
		for (Neuron n : network.getHiddenLayer().getNeuronListUnsafe()) {
			for (Synapse s : n.getFanIn()) {
				s.forceSetStrength(str);
				str += 0.1;
			}
		}

		double biases = 0;
		for (Neuron n : network.getOutputLayer().getNeuronListUnsafe()) {
			((SigmoidalRule) n.getUpdateRule()).setBias(0.314);
			biases += 0.314;
		}

		biases = 0.11;
		for (Neuron n : network.getHiddenLayer().getNeuronList()) {
			((SigmoidalRule) n.getUpdateRule()).setBias(biases);
			biases += 0.1;
		}

		BackpropTrainer2 trainer = new BackpropTrainer2(network);
        trainer.setUpdateMethod(UpdateMethod.SINGLE);
		trainer.initData();
        trainer.setLearningRate(0.1);
        
        trainer.apply();
        trainer.commitChanges();
        
        double[] hidBiases = network.getHiddenLayer().getBiases();
        double[] outBiases = network.getOutputLayer().getBiases();
        Object[] tmp = network.getHiddenLayer().getIncomingSgs().toArray();
        double[][] inpHidStrs = ((SynapseGroup) tmp[0]).getWeightMatrix();
        tmp = network.getOutputLayer().getIncomingSgs().toArray();
        double[][] hidOutStrs = ((SynapseGroup) tmp[0]).getWeightMatrix();
        
		//        trainer.printDebugInfo();
		//         System.out.println(Arrays.deepToString(inpHidStrs));
		//         System.out.println();
		//         System.out.println(Arrays.deepToString(hidOutStrs));

		double[][] inHidJBlas = trainer.getWeightMatrices().get(0).transpose()
				.toArray2();
		System.out.println(Arrays.deepToString(inpHidStrs));
		System.out.println();
		System.out.println(Arrays.deepToString(inHidJBlas));
		System.out.println("-----");
		double[][] hidOutJBlas = trainer.getWeightMatrices().get(1).transpose()
				.toArray2();
		System.out.println(Arrays.deepToString(hidOutStrs));
		System.out.println();
		System.out.println(Arrays.deepToString(hidOutJBlas));

		//  
		for (int ii = 0; ii < noInp; ++ii) {
			assertArrayEquals(inHidJBlas[ii], inpHidStrs[ii], 0);
		}
		for (int ii = 0; ii < noHid; ++ii) {
			assertArrayEquals(hidOutJBlas[ii], hidOutStrs[ii], 0);
		}
		

		
		List<DoubleMatrix> jblasBiases = trainer.getBiases();
		double [] biasesOutJBlas = jblasBiases.get(1).data;
		double [] biasesHidJBlas = jblasBiases.get(0).data;
		
		assertArrayEquals(biasesHidJBlas, hidBiases, 0);
		assertArrayEquals(biasesOutJBlas, outBiases, 0);
		
	}

	
//	  public BackpropNetwork createTestNetwork(int noInp, int noHid, int noOut, NeuronUpdateRule hidRule, NeuronUpdateRule outRule) {
//	        BackpropNetwork network = new BackpropNetwork(new Network(),
//	                new int[] { noInp, noHid, noOut });
//	        network.getHiddenLayer().setNeuronType(hidRule);
//	        network.getOutputLayer().setNeuronType(outRule);
//	        return network;
//	    }
//
//	    // Just to illustrate how to set up some tests
//	    public void createSomeNetworks() {
//
//	        SigmoidalRule tanh = new SigmoidalRule(SquashingFunction.TANH);
//	        SigmoidalRule logistic = new SigmoidalRule(SquashingFunction.LOGISTIC);
//	        SigmoidalRule arctan = new SigmoidalRule(SquashingFunction.ARCTAN);
//	        LinearRule linear = new LinearRule(); // May have to make Linear implement transfer function
//
//	        BackpropNetwork network1 = createTestNetwork(2,2,2, tanh, linear);
//	        BackpropNetwork network2 = createTestNetwork(2,2,2, logistic, linear);
//	        BackpropNetwork network3 = createTestNetwork(2,2,2, arctan, linear);
//	        BackpropNetwork network4 = createTestNetwork(2,2,2, tanh, tanh);
//
//	        // Try some non-standard bounds. The transfer functions should be able to handle this
//	        SigmoidalRule logistic_1_1 = new SigmoidalRule(SquashingFunction.LOGISTIC);
//	        logistic_1_1.setLowerBound(-1);
//	        logistic_1_1.setUpperBound(1);
//	        BackpropNetwork network5 = createTestNetwork(2,2,2, logistic_1_1, linear);
//
//	    }
	
}
