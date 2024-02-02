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
package org.simbrain.network.subnetworks;

import org.simbrain.network.NetworkModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.SynapseGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.neurongroups.CompetitiveGroup;
import org.simbrain.network.neurongroups.NeuronGroup;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;

import java.util.List;

/**
 * <b>CompetitiveNetwork</b> is a small network encompassing a Competitive
 * group. An input layer and input data have been added so that the SOM can be
 * easily trained using existing Simbrain GUI tools
 *
 * @author Jeff Yoshimi
 */
public class CompetitiveNetwork extends Subnetwork {

    /**
     * The competitive network.
     */
    private final CompetitiveGroup competitive;

    /**
     * The input layer.
     */
    private final NeuronGroup inputLayer;

    /**
     * Training set.
     */
    // private final TrainingSet trainingSet = new TrainingSet();

    /**
     * Construct an SOM Network.
     *
     * @param net                   parent network Set to null when this is used simply as a
     *                              holder for param values.
     * @param numInputNeurons       number of neurons in the input layer
     * @param numCompetitiveNeurons number of neurons in the Competitive layer
     */
    public CompetitiveNetwork(Network net, int numInputNeurons, int numCompetitiveNeurons) {
        super(net);
        this.setLabel("Competitive Network");

        competitive = new CompetitiveGroup(net, numCompetitiveNeurons);
        competitive.setLabel("Competitive Group");
        this.addModel(competitive);
        competitive.setLayoutBasedOnSize();

        inputLayer = new NeuronGroup(net, numInputNeurons);
        this.addModel(inputLayer);
        inputLayer.setLabel("Input layer");
        inputLayer.setClamped(true);
        inputLayer.setLayoutBasedOnSize();
        inputLayer.getNeuronList().forEach(n -> n.setLowerBound(0));

        SynapseGroup sg = new SynapseGroup(inputLayer, competitive);
        this.addModel(sg);
        sg.getSynapses().forEach(s -> s.setLowerBound(0));

        inputLayer.offset(0, 400);

    }

    // @Override
    public List<Neuron> getInputNeurons() {
        return inputLayer.getNeuronList();
    }

    // @Override
    public List<Neuron> getOutputNeurons() {
        return competitive.getNeuronList();
    }

    // @Override
    // public TrainingSet getTrainingSet() {
    //     return trainingSet;
    // }

    /**
     * @return the competitive network
     */
    public CompetitiveGroup getCompetitive() {
        return competitive;
    }

    public NeuronGroup getInputLayer() {
        return inputLayer;
    }

    // @Override
    public NetworkModel getNetwork() {
        return this;
    }

    /**
     * Helper class for creating new competitive nets using {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}.
     */
    public static class CompetitiveCreator implements EditableObject {

        @UserParameter(label = "Number of inputs")
        int numIn = 20;

        @UserParameter(label = "Number of competitive neurons")
        int numComp = 20;

        /**
         * Create the competitive net
         */
        public CompetitiveNetwork create(Network network) {
            return new CompetitiveNetwork(network,  numIn, numComp);
        }

    }
}
