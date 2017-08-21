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

import org.simbrain.network.core.Network;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.util.math.SquashingFunction;

/**
 * Test backprop trainer.
 * 
 * Future test methods. But for now just run them with a main.
 */
public class BackpropTrainerTest {

    public static void main(String[] args) {
        //testAndGate();
        testAssociator();
    }

    public static void testAndGate() {

        BackpropNetwork network = new BackpropNetwork(new Network(),
                new int[] { 2, 2, 1 });

        network.getTrainingSet().setInputData(
                new double[][] { { 0, 0 }, { 0, 1 }, { 1, 0 }, { 1, 1 } });
        network.getTrainingSet()
                .setTargetData(new double[][] { { 0 }, { 1 }, { 1 }, { 1 } });

        // BackpropTrainer trainer = new BackpropTrainer(network);
        BackpropTrainer2 trainer = new BackpropTrainer2(network);
        trainer.initData();
        // TODO: Init specific weights for deterministic testing
        trainer.randomize();
        for (int i = 0; i < 100; i++) {
            trainer.apply();
            System.out.println(trainer.getError());
        }
    }
    
    // Contrast between trainer1 and 2 stark in this one
    public static void testAssociator() {

        BackpropNetwork network = new BackpropNetwork(new Network(),
                new int[] { 2, 2, 2 });

        network.getTrainingSet().setInputData(
                new double[][] { { 0, 0 }, { 0, 1 }, { 1, 0 }, { 1, 1 } });
        network.getTrainingSet().setTargetData(
                new double[][] { { 0, 0 }, { 0, 1 }, { 1, 0 }, { 1, 1 } });
//        System.out.println(SquashingFunction.getFunctionFromIndex(((SigmoidalRule)network.getOutputLayer().getNeuronList().get(0)
//                .getUpdateRule()).getSquashFunctionInt()));
//        System.out.println(SquashingFunction.getFunctionFromIndex(((SigmoidalRule)network.getHiddenLayer().getNeuronList().get(0)
//                .getUpdateRule()).getSquashFunctionInt()));

        // BackpropTrainer trainer = new BackpropTrainer(network);
        BackpropTrainer2 trainer = new BackpropTrainer2(network);
        trainer.initData();
        trainer.randomize();
        for (int i = 0; i < 100; i++) {
            trainer.apply();
            System.out.println(trainer.getError());
        }
    }

}
