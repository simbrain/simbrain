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

import java.awt.geom.Point2D;

import org.simbrain.network.core.Network;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.trainers.Trainable;
import org.simbrain.network.trainers.TrainingSet;

/**
 * A Least Mean Squares network.
 *
 * @author Jeff Yoshimi
 */
public class LMSNetwork extends FeedForward implements Trainable {

    /**
     * Training set.
     */
    private final TrainingSet trainingSet = new TrainingSet();

    /**
     * Construct a new LMS Network.
     *
     * @param network the parent network
     * @param numInputNeurons number of input neurons
     * @param numOutputNeurons number of output neurons
     * @param initialPosition initial location of the network
     */
    public LMSNetwork(final Network network, int numInputNeurons,
            int numOutputNeurons, Point2D initialPosition) {
        super(network, new int[] { numInputNeurons, numOutputNeurons },
                initialPosition);
        getOutputLayer().setNeuronType(new LinearRule());
        setLabel("LMS Network");
    }

    @Override
    public TrainingSet getTrainingSet() {
        return trainingSet;
    }

    @Override
    public void initNetwork() {
    }

}
