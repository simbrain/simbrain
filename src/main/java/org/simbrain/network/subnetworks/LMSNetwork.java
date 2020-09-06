/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.subnetworks;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.distribution.UniformDistribution;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.simbrain.network.NetworkModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.dl4j.MultiLayerNet;
import org.simbrain.network.dl4j.NeuronArray;
import org.simbrain.network.gui.dialogs.network.LMSDialog;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.trainers.Trainable;
import org.simbrain.network.trainers.TrainingSet;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;

import java.awt.geom.Point2D;

/**
 * A Least Mean Squares network.
 *
 * TODO: Rename. This is not really "LMS" anymore.
 * This solution involves a parallel dl4j object and simbrian object which get synced during training
 * Longer term new custom visualization for {@link org.simbrain.network.dl4j.MultiLayerNet} should be used.
 *
 * @author Jeff Yoshimi
 */
public class LMSNetwork extends FeedForward implements Trainable {

    /**
     * Training set.
     */
    private final TrainingSet trainingSet = new TrainingSet();

    //TODO: Put MLNConfig object here

    /**
     * Construct a new LMS Network.
     *
     * @param network          the parent network
     * @param numInputNeurons  number of input neurons
     * @param numOutputNeurons number of output neurons
     * @param initialPosition  initial location of the network
     */
    public LMSNetwork(final Network network, int numInputNeurons, int numOutputNeurons, Point2D initialPosition) {
        super(network, new int[]{numInputNeurons, numOutputNeurons}, initialPosition);
        setUseNeuronArrays(true);
        setLabel("LMS Network");

    }

    @Override
    public TrainingSet getTrainingSet() {
        return trainingSet;
    }

    @Override
    public void initNetwork() {
    }

    @Override
    public NetworkModel getNetwork() {
        return this;
    }

    public void train(MultiLayerNetwork mln, DataSet data) {
        for (int i = 0; i < 25; i++) {
            mln.fit(data);
            System.out.println("score:" + mln.score());
        }
        getWeightMatrixList().get(0)
                .setWeights(Nd4j.toFlattened(mln.getLayer(0).getParam("W")).toDoubleVector());
        // TODO: Allow setting of bias in neuron array
        //getNAList().get(1).setBias(mln.getLayer(0).getParam("b").toDoubleVector());
    }



}
