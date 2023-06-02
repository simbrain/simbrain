// /*
//  * Part of Simbrain--a java-based neural network kit
//  * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
//  *
//  * This program is free software; you can redistribute it and/or modify
//  * it under the terms of the GNU General Public License as published by
//  * the Free Software Foundation; either version 2 of the License, or
//  * (at your option) any later version.
//  *
//  * This program is distributed in the hope that it will be useful,
//  * but WITHOUT ANY WARRANTY; without even the implied warranty of
//  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  * GNU General Public License for more details.
//  *
//  * You should have received a copy of the GNU General Public License
//  * along with this program; if not, write to the Free Software
//  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//  */
// package org.simbrain.network.trainers;
//
// import org.simbrain.network.core.NetworkKt;
// import org.simbrain.network.core.Neuron;
// import org.simbrain.network.core.Synapse;
// import org.simbrain.network.updaterules.interfaces.BiasedUpdateRule;
//
// import java.util.List;
//
// import static org.simbrain.network.core.NetworkUtilsKt.updateNeurons;
//
// /**
//  * Train loose neurons using least mean squares. Assumes they are connected.
//  *
//  * @author jyoshimi
//  */
// public class LMSIterative extends IterableTrainer {
//
//     /**
//      * Input neurons.
//      */
//     private List<Neuron> inputs;
//
//     /**
//      * Output neurons.
//      */
//     private List<Neuron> outputs;
//
//     /**
//      * Training set with input data and target data with numbers of column to match the size of inputs and outputs.
//      */
//     private TrainingSet ts;
//
//     /**
//      * Current error.
//      */
//     private double rmsError;
//
//     // TODO: Use annotations around here?
//     public static double DEFAULT_LEARNING_RATE = .01;
//
//     /**
//      * Learning rate.
//      */
//     private double learningRate = DEFAULT_LEARNING_RATE;
//
//
//     // TODO
//     public LMSIterative(List<Neuron> inputs, List<Neuron> outputs, TrainingSet ts) {
//         this.inputs = inputs;
//         this.outputs = outputs;
//         this.ts = ts;
//     }
//
//     @Override
//     public double getError() {
//         return rmsError;
//     }
//
//
//     /**
//      * A standard way of randomizing networks to which LMSIterative is applied,
//      * by randomizing bias on output nodes and the single layer of weights.
//      */
//     public void randomize() {
//         for (Neuron neuron : outputs) {
//             neuron.clear(); // Cleared output nodes look nicer in the GUI
//             if (neuron.getUpdateRule() instanceof BiasedUpdateRule) {
//                 ((BiasedUpdateRule) neuron.getUpdateRule()).setBias(Math.random());
//             }
//         }
//         // network.getWeightMatrixList().forEach(WeightMatrix::randomize);
//     }
//
//     @Override
//     public TrainingSet getTrainingSet() {
//         return ts;
//     }
//
//     public double getLearningRate() {
//         return learningRate;
//     }
//
//     public void setLearningRate(double learningRate) {
//         this.learningRate = learningRate;
//     }
//
//     public List<Neuron> getInputs() {
//         return inputs;
//     }
//
//     public List<Neuron> getOutputs() {
//         return outputs;
//     }
//
//     @Override
//     public void apply() throws DataNotInitializedException {
//
//         rmsError = 0;
//
//         // Set local variables
//         int numRows = ts.getInputData().length;
//         int numInputs = inputs.size();
//         int numOutputs = outputs.size();
//
//         // Run through training data
//         for (int row = 0; row < numRows; row++) {
//
//             // Set input layer values
//             for (int i = 0; i < numInputs; i++) {
//                 inputs.get(i).forceSetActivation(ts.getInputData()[row][i]);
//             }
//
//             // Update output node
//             updateNeurons(outputs);
//
//             // Iterate through weights and biases and update them
//             for (int i = 0; i < numOutputs; i++) {
//
//                 // Get target neuron and compute error
//                 Neuron outputNeuron = outputs.get(i);
//                 double targetValue = ts.getTargetData()[row][i];
//                 double error = targetValue - outputNeuron.getActivation();
//                 rmsError += (error * error); // TODO: Validate rmse
//
//                 // Update weights
//                 for (Synapse synapse : outputNeuron.getFanIn()) {
//                     double deltaW = (learningRate * error * synapse.getSource().getActivation());
//                     synapse.setStrength(synapse.getStrength() + deltaW);
//                 }
//
//                 // Update bias of target neuron
//                 BiasedUpdateRule bias = (BiasedUpdateRule) outputNeuron.getUpdateRule();
//                 bias.setBias(bias.getBias() + (learningRate * error));
//             }
//             rmsError = rmsError / (numInputs * numOutputs);
//         }
//         getEvents().fireErrorUpdated();
//         incrementIteration();
//     }
//
// }
