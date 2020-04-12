package org.simbrain.network.trainers;

import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.simbrain.network.core.Network;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.network.trainers.BackpropTrainer.UpdateMethod;

import java.util.stream.IntStream;

import static org.junit.Assert.assertTrue;

public class BackpropTrainerTest {

    /**
     * A simple identity map task
     */
    private BackpropTrainer getSimpleBackprop(int numHidden) {
        BackpropNetwork network = new BackpropNetwork(new Network(), new int[]{2, numHidden, 2});
        network.getTrainingSet().setInputData(new double[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}});
        network.getTrainingSet().setTargetData(new double[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}});
        BackpropTrainer trainer = new BackpropTrainer(network);
        trainer.setUpdateMethod(UpdateMethod.SINGLE);
        trainer.initData();
        trainer.setLearningRate(0.1);
        trainer.commitChanges();
        return trainer;
    }

    // TODO: Test Different UpdateMethods
    // TODO: XOR

    @Test
    public void testConvergence() {

        BackpropTrainer trainer = getSimpleBackprop(2);
        IntStream.range(0, 100).forEach(i -> {
            try {
                trainer.iterate();
            } catch (Trainer.DataNotInitializedException e) {
                e.printStackTrace();
            }
        });
        assertTrue(trainer.getError() < 1);

        BackpropTrainer trainer2 = getSimpleBackprop(2);
        IntStream.range(0, 100).forEach(i -> {
            try {
                trainer2.iterate();
            } catch (Trainer.DataNotInitializedException e) {
                e.printStackTrace();
            }
        });
        assertTrue(trainer2.getError() < 1);

        // Fails when running for 1000.  Error < .01 but output is not 1,1
        //BackpropNetwork bp = trainer2.getNetwork();
        //bp.getNeuronGroup(0).forceSetActivations(new double[]{1,1});
        //bp.update();
        //System.out.println(bp);
        //System.out.println(trainer2.getError());

        //TODO: Other activation rules
    }

    // @Test
    public void nd4JScratch() {
        INDArray input = Nd4j.ones(1,2);
        System.out.println(input);
        INDArray weights = Nd4j.ones(2,3);
        System.out.println(weights);
        System.out.println(input.mmul(weights));
        /// In place operation
        INDArray result = Nd4j.ones(1,3);
        input.mmuli(weights, result);
        System.out.println(result);
    }
}
