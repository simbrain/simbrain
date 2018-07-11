/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
package org.simbrain.network.trainers;

import org.jblas.DoubleMatrix;
import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.util.Utils;
import org.simbrain.util.math.SquashingFunctionEnum;
import org.simbrain.util.math.ProbDistributions.NormalDistribution;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 * Test backprop trainer.
 * <p>
 * Future test methods. But for now just run them with a main.
 */
public class BackpropTrainerTest {

    /**
     * Classic XOR Test. Due to variation in test outcome, we are only asserting
     * that the final error should be less than the initial error, not that the
     * training will converge to any particular value.
     */
    @Test
    public void testXor() {
        // Create a network. Hidden layer of 5 makes training much more reliable.
        BackpropNetwork network = new BackpropNetwork(new Network(), new int[]{2, 5, 1});

        network.getTrainingSet().setInputData(new double[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}});
        network.getTrainingSet().setTargetData(new double[][]{{.2}, {.8}, {.8}, {.2}});

        BackpropTrainer2 trainer = new BackpropTrainer2(network);
        trainer.initData();
        trainer.setLearningRate(.1);
        trainer.setMomentum(.9);
        //TODO
//        trainer.getRandomizer().ifPresent(rnd -> {
//            rnd = new NormalDistribution(0, 0.1);
//        });
        trainer.randomize();
        trainer.setUpdateMethod(BackpropTrainer2.UpdateMethod.STOCHASTIC);
        trainer.apply();
        double expected = trainer.getError();
        for (int i = 0; i < 10000; i++) {
            trainer.apply();
        }
        double actual = trainer.getError();
        assertTrue(actual < expected);
        System.out.println(String.format("Initial Error: %s  Final Error: %s", expected, actual));
    }

    /**
     * Simple 2-2-2 auto-associator.
     */
    @Test
    public void testAssociator() {
        BackpropNetwork network = new BackpropNetwork(new Network(), new int[]{2, 2, 2});

        network.getTrainingSet().setInputData(new double[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}});
        network.getTrainingSet().setTargetData(new double[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}});

        // BackpropTrainer trainer = new BackpropTrainer(network);
        BackpropTrainer2 trainer = new BackpropTrainer2(network);
//        trainer.getRandomizer().ifPresent(rnd -> {
//            rnd = new NormalDistribution(0, 0.1);
//        });
        trainer.randomize();
        trainer.initData();
        trainer.setLearningRate(.15);
        trainer.setMomentum(.8);

        trainer.apply();
        double expected = trainer.getError();
        for (int i = 0; i < 10000; i++) {
            trainer.apply();
        }
        double actual = trainer.getError();
        assertTrue(actual < expected);
        System.out.println(String.format("Initial Error: %s  Final Error: %s", expected, actual));
    }

    /**
     * 5-5-5 auto-associator.
     */
    @Test
    public void testAssociator5() {
        BackpropNetwork network = new BackpropNetwork(new Network(), new int[]{5, 5, 5});

        network.getTrainingSet().setInputData(Utils.getDoubleMatrix(new File("./simulations/tables/5_binary.csv")));
        network.getTrainingSet().setTargetData(Utils.getDoubleMatrix(new File("./simulations/tables/5_binary.csv")));

        // BackpropTrainer trainer = new BackpropTrainer(network);
        BackpropTrainer2 trainer = new BackpropTrainer2(network);
//        trainer.getRandomizer().ifPresent(rnd -> {
//            rnd = new NormalDistribution(0, 0.1);
//        });
        trainer.randomize();
        trainer.initData();
        trainer.setLearningRate(.1);
        trainer.setMomentum(.9);
        trainer.randomize();

        trainer.apply();
        double expected = trainer.getError();
        for (int i = 0; i < 10000; i++) {
            trainer.apply();
        }
        double actual = trainer.getError();
        assertTrue(actual < expected);
        System.out.println(String.format("Initial Error: %s  Final Error: %s", expected, actual));
    }

    @Test
    public void testSineWave() {
        BackpropNetwork network = new BackpropNetwork(new Network(), new int[]{1, 5, 1});

        double[][] inpData = new double[100][1];
        double[][] targData = new double[100][1];
        for (int ii = 0; ii < 100; ++ii) {
            inpData[ii][0] = ii * (2 * Math.PI / 100);
            targData[ii][0] = Math.sin(inpData[ii][0]) / 10 + 0.5;
        }
        network.getTrainingSet().setInputData(inpData);
        network.getTrainingSet().setTargetData(targData);

        BackpropTrainer2 trainer = new BackpropTrainer2(network);
        trainer.initData();
//        trainer.getRandomizer().ifPresent(rnd -> {
//            rnd = new NormalDistribution(0, 0.1);
//        });
        trainer.randomize();
        trainer.setLearningRate(0.05);
        trainer.setMomentum(0.5);

        trainer.apply();
        double expected = trainer.getError();
        for (int i = 0; i < 10000; i++) {
            trainer.apply();
        }
        double actual = trainer.getError();
        assertTrue(actual < expected);
        System.out.println(String.format("Initial Error: %s  Final Error: %s", expected, actual));
    }

    /**
     * Test based on this discussion.
     * https://mattmazur.com/2015/03/17/a-step-by-step-backpropagation-example/
     * <p>
     */
    @Test
    public void testMazur() {
        BackpropNetwork network = new BackpropNetwork(new Network(), new int[]{2, 2, 2});

        SigmoidalRule sigmoidal = new SigmoidalRule();
        sigmoidal.setSquashFunctionType(SquashingFunctionEnum.LOGISTIC);
        sigmoidal.setSlope(0.25);

        network.getNeuronGroup(1).setNeuronType(sigmoidal);
        network.getNeuronGroup(2).setNeuronType(sigmoidal);

        network.getTrainingSet().setInputData(new double[][]{{.05, .10}});
        network.getTrainingSet().setTargetData(new double[][]{{.01, .99}});

        BackpropTrainer2 trainer = new BackpropTrainer2(network);
        trainer.initData();
        trainer.setMomentum(0);
        trainer.setLearningRate(.5);

        DoubleMatrix weightLayer1 = new DoubleMatrix(new double[][]{{.15, .25}, {.2, .3}});
        trainer.getWeightMatrices().set(0, weightLayer1);
        DoubleMatrix weightLayer2 = new DoubleMatrix(new double[][]{{.4, .5}, {.45, .55}});
        trainer.getWeightMatrices().set(1, weightLayer2);

        DoubleMatrix biases1 = new DoubleMatrix(new double[]{.35, .35});
        trainer.getBiases().set(0, biases1);

        DoubleMatrix biases2 = new DoubleMatrix(new double[]{.6, .6});
        trainer.getBiases().set(1, biases2);

        for (int i = 0; i < 1; i++) {
            trainer.apply();
            System.out.println("Error:" + trainer.getError());
        }
    }

}
