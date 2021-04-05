// /*
//  * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
//  * Authors. See http://www.simbrain.net/credits This program is free software;
//  * you can redistribute it and/or modify it under the terms of the GNU General
//  * Public License as published by the Free Software Foundation; either version 2
//  * of the License, or (at your option) any later version. This program is
//  * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
//  * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
//  * should have received a copy of the GNU General Public License along with this
//  * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
//  * - Suite 330, Boston, MA 02111-1307, USA.
//  */
// package org.simbrain.network.subnetworks;
//
// import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
// import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
// import org.deeplearning4j.nn.conf.distribution.UniformDistribution;
// import org.deeplearning4j.nn.conf.layers.OutputLayer;
// import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
// import org.nd4j.linalg.activations.Activation;
// import org.nd4j.linalg.api.ndarray.INDArray;
// import org.nd4j.linalg.dataset.api.DataSet;
// import org.nd4j.linalg.factory.Nd4j;
// import org.nd4j.linalg.learning.config.Sgd;
// import org.nd4j.linalg.lossfunctions.LossFunctions;
// import org.simbrain.network.NetworkModel;
// import org.simbrain.network.core.Network;
// import org.simbrain.network.trainers.Trainable;
// import org.simbrain.network.trainers.TrainingSet;
// import org.simbrain.util.UserParameter;
// import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
// import org.simbrain.util.propertyeditor.EditableObject;
//
// import java.awt.geom.Point2D;
//
// /**
//  * A Least Mean Squares network.
//  *
//  * TODO: Rename. This is not really "LMS" anymore.
//  *
//  * This solution involves a parallel dl4j object and Simbrain object which get synced during training
//  * Longer term new custom visualization for {@link org.simbrain.network.matrix.MultiLayerNet} should be used.
//  *
//  * @author Jeff Yoshimi
//  */
// public class LMSNetwork extends FeedForward implements Trainable {
//
//     /**
//      * Training set.
//      */
//     private final TrainingSet trainingSet = new TrainingSet();
//
//     /**
//      * LMS Configuration object that is edited using an {@link AnnotatedPropertyEditor}
//      */
//     private LMSConfig lmsConfig = new LMSConfig();
//
//     /**
//      * Input data.
//      */
//     private INDArray inputData;
//
//     /**
//      * Target data.
//      */
//     private INDArray targetData;
//
//     /**
//      * Dataset object used by dl4j.
//      */
//     private DataSet dataset;
//
//     /**
//      * DL4J  Multi-layer network.
//      */
//     private transient MultiLayerNetwork mln;
//
//     /**
//      * Construct a new LMS Network.
//      *
//      * @param network          the parent network
//      * @param numInputNeurons  number of input neurons
//      * @param numOutputNeurons number of output neurons
//      * @param initialPosition  initial location of the network
//      */
//     public LMSNetwork(final Network network, int numInputNeurons, int numOutputNeurons, Point2D initialPosition) {
//         super(network, new int[]{numInputNeurons, numOutputNeurons}, initialPosition);
//         setUseNeuronArrays(true);
//         setLabel("LMS Network");
//         inputData = Nd4j.zeros(5, numInputNeurons);
//         targetData = Nd4j.zeros(5, numOutputNeurons);
//         dataset = new org.nd4j.linalg.dataset.DataSet(inputData, targetData);
//         initNetwork();
//     }
//
//     @Override
//     public void initNetwork() {
//
//         MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
//                 // Using stochastic gradient decent
//                 .updater(new Sgd(lmsConfig.learningRate))
//                 .seed(lmsConfig.seed)
//                 .biasInit(lmsConfig.initalBias)
//                 .miniBatch(lmsConfig.useMiniBatch)
//                 .list()
//                 .layer(new OutputLayer.Builder(lmsConfig.lossFunc)
//                         .nIn(getNAList().get(0).getNumNodes())
//                         .nOut(getNAList().get(1).getNumNodes())
//                         .activation(lmsConfig.actFunc)
//                         .weightInit(new UniformDistribution(0, 1)) //TODO
//                         .build())
//                 .build();
//
//         // TODO: Use config file from LMSNetwork, and draw weights and biases from it as well
//         mln = new MultiLayerNetwork(config);
//         mln.init();    }
//
//     @Override
//     public NetworkModel getNetwork() {
//         return this;
//     }
//
//
//     @Override
//     public TrainingSet getTrainingSet() {
//         return trainingSet;
//     }
//
//     public double getError() {
//         return mln.score();
//     }
//
//     /**
//      * Train the network
//      */
//     public void train() {
//         for (int i = 0; i < 25; i++) {
//             mln.fit(dataset);
//             System.out.println("score:" + mln.score());
//         }
//
//         // Use DL4J net to set weights
//         getWeightMatrixList().get(0)
//                 .setWeights(Nd4j.toFlattened(mln.getLayer(0).getParam("W")).toDoubleVector());
//         // TODO: Allow setting of bias in neuron array
//         //getNAList().get(1).setBias(mln.getLayer(0).getParam("b").toDoubleVector());
//     }
//
//     public INDArray getInputData() {
//         return inputData;
//     }
//
//     public LMSConfig getConfig() {
//         return lmsConfig;
//     }
//
//     public void setInputData(INDArray inputData) {
//         this.inputData = inputData;
//         dataset = new org.nd4j.linalg.dataset.DataSet(this.inputData, targetData);
//     }
//
//     public INDArray getTargetData() {
//         return targetData;
//     }
//
//     public void setTargetData(INDArray targetData) {
//         this.targetData = targetData;
//         dataset = new org.nd4j.linalg.dataset.DataSet(inputData, this.targetData);
//     }
//
//     /**
//      * Configuration object.
//      */
//     private class LMSConfig implements EditableObject {
//
//         @UserParameter(label = "Loss Function", order = 10)
//         private LossFunctions.LossFunction lossFunc = LossFunctions.LossFunction.MSE;
//
//         @UserParameter(label = "Activation Function", order = 20)
//         private Activation actFunc = Activation.SIGMOID;
//
//         @UserParameter(label = "Minibatch", order = 30)
//         private boolean useMiniBatch = true;
//
//         @UserParameter(label = "Learning Rate", minimumValue = 0, increment = .01, order = 40)
//         private double learningRate = .2;
//
//         @UserParameter(label = "Seed", minimumValue = 1, increment = 1, order = 50)
//         private int seed = 1;
//
//         @UserParameter(label = "Initial Bias", minimumValue = 0.0, increment = .1, order = 60)
//         private double initalBias = 0.0;
//
//         @Override
//         public String getName() {
//             return "Optimizer Settings";
//         }
//
//         // Somehow deal with DL4JInvalidConfigException here
//     }
//
//
// }
