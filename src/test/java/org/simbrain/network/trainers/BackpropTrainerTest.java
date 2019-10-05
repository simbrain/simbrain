package org.simbrain.network.trainers;

import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.network.trainers.BackpropTrainer.UpdateMethod;
import org.simbrain.util.Utils;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class BackpropTrainerTest {

    @Test
    public void testBasicTraining() {

        BackpropNetwork network = new BackpropNetwork(new Network(), new int[]{2, 2, 2});
        network.getTrainingSet().setInputData(new double[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}});
        network.getTrainingSet().setTargetData(new double[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}});

        BackpropTrainer trainer = new BackpropTrainer(network);
        trainer.setUpdateMethod(UpdateMethod.SINGLE);
        trainer.initData();
        trainer.setLearningRate(0.1);

        trainer.apply();
        trainer.commitChanges();

        try {
            trainer.iterate();
            System.out.println(trainer.getError());
            trainer.iterate();
            System.out.println(trainer.getError());
            trainer.iterate();
            // Make sure things don't blow up
            assertTrue(trainer.getError() < 10_000);

            // TODO: Test that training actually converges.
            // TODO: Test for different types of sigmoid, etc.
        } catch (Trainer.DataNotInitializedException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testCreation() {

        // Fails for noOut = 1, in 1.0.0-beta4.
        // See https://github.com/eclipse/deeplearning4j/issues/7839
        int noOut = 2;
        int noHid = 2;
        int noInp = 2;
        BackpropNetwork network = new BackpropNetwork(new Network(), new int[]{noInp, noHid, noOut});

        float str = 0.1f;
        float[][] hidOutStrs = new float[noHid][noOut];
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

        float[][] inpHidStrs = new float[noInp][noHid];
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

        float biases = 0;
        float[] outBiases = new float[noOut];
        int jj = 0;
        for (Neuron n : network.getOutputLayer().getNeuronListUnsafe()) {
            ((SigmoidalRule) n.getUpdateRule()).setBias(0.314);
            biases += 0.314;
            outBiases[jj++] = (float) ((SigmoidalRule) n.getUpdateRule()).getBias();
        }

        biases = 0.11f;
        jj = 0;
        float[] hidBiases = new float[noHid];
        for (Neuron n : network.getHiddenLayer().getNeuronList()) {
            ((SigmoidalRule) n.getUpdateRule()).setBias(biases);
            biases += 0.1;
            hidBiases[jj++] = (float) ((SigmoidalRule) n.getUpdateRule()).getBias();
        }

        BackpropTrainer trainer = new BackpropTrainer(network);

        float[][] inHidJBlas = trainer.getWeightMatrices().get(0).transpose().toFloatMatrix();
        //System.out.println(Arrays.deepToString(inpHidStrs));
        //System.out.println();
        //System.out.println(Arrays.deepToString(inHidJBlas));
        //System.out.println("-----");
        float[][] hidOutJBlas = trainer.getWeightMatrices().get(1).transpose().toFloatMatrix();
        //System.out.println(Arrays.deepToString(hidOutStrs));
        //System.out.println();
        //System.out.println(Arrays.deepToString(hidOutJBlas));
        //System.out.println();
        //System.out.println("-----");
        //System.out.println();

        for (int ii = 0; ii < noInp; ++ii) {
            assertArrayEquals(inHidJBlas[ii], inpHidStrs[ii], 0);
        }
        for (int ii = 0; ii < noHid; ++ii) {
            assertArrayEquals(hidOutJBlas[ii], hidOutStrs[ii], 0);
        }

        List<INDArray> jblasBiases = trainer.getBiases();
        float[] biasesOutJBlas = jblasBiases.get(1).toFloatVector();
        float[] biasesHidJBlas = jblasBiases.get(0).toFloatVector();

        assertArrayEquals(biasesHidJBlas, hidBiases, 0);
        assertArrayEquals(biasesOutJBlas, outBiases, 0);

    }

    @Test
    public void testCopyBack() {
        int noOut = 2; // Problem with 1d array from above
        int noHid = 2;
        int noInp = 2;

        BackpropNetwork network = new BackpropNetwork(new Network(), new int[]{noInp, noHid, noOut});
        network.getTrainingSet().setInputData(new double[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}});
        network.getTrainingSet().setTargetData(new double[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}});
        //network.getTrainingSet().setTargetData(new double[][]{{0}, {1}, {1}, {0}});
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

        BackpropTrainer trainer = new BackpropTrainer(network);
        trainer.setUpdateMethod(UpdateMethod.SINGLE);
        trainer.initData();
        trainer.setLearningRate(0.1);

        trainer.apply();
        trainer.commitChanges();

        float[] hidBiases = Utils.castToFloat(network.getHiddenLayer().getBiases());
        float[] outBiases = Utils.castToFloat(network.getOutputLayer().getBiases());
        Object[] tmp = network.getHiddenLayer().getIncomingSgs().toArray();
        float[][] inpHidStrs = Utils.castToFloat(((SynapseGroup) tmp[0]).getWeightMatrix());
        tmp = network.getOutputLayer().getIncomingSgs().toArray();
        float[][] hidOutStrs = Utils.castToFloat(((SynapseGroup) tmp[0]).getWeightMatrix());

        //trainer.printDebugInfo();
        //System.out.println(Arrays.deepToString(inpHidStrs));
        //System.out.println();
        //System.out.println(Arrays.deepToString(hidOutStrs));

        float[][] inHidJBlas = trainer.getWeightMatrices().get(0).transpose().toFloatMatrix();
        //System.out.println(Arrays.deepToString(inpHidStrs));
        //System.out.println();
        //System.out.println(Arrays.deepToString(inHidJBlas));
        //System.out.println("-----");

        float[][] hidOutJBlas = trainer.getWeightMatrices().get(1).transpose().toFloatMatrix();
        //System.out.println(Arrays.deepToString(hidOutStrs));
        //System.out.println();
        //System.out.println(Arrays.deepToString(hidOutJBlas));

        //
        for (int ii = 0; ii < noInp; ++ii) {
            assertArrayEquals(inHidJBlas[ii], inpHidStrs[ii], 0);
        }
        for (int ii = 0; ii < noHid; ++ii) {
            assertArrayEquals(hidOutJBlas[ii], hidOutStrs[ii], 0);
        }

        List<INDArray> jblasBiases = trainer.getBiases();
        float[] biasesOutJBlas = jblasBiases.get(1).toFloatVector();
        float[] biasesHidJBlas = jblasBiases.get(0).toFloatVector();

        assertArrayEquals(biasesHidJBlas, hidBiases, 0);
        assertArrayEquals(biasesOutJBlas, outBiases, 0);

    }



}
