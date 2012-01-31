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
package org.simbrain.network.groups.subnetworks;

import java.awt.geom.Point2D;

import org.simbrain.network.groups.FeedForward;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.network.trainers.LMSIterative;
import org.simbrain.network.trainers.Trainer;

/**
 * An LMS Network
 * 
 * @author Jeff Yoshimi
 */
public class LMSNetwork extends FeedForward {


    /** Reference to parent trainer. */
    private final Trainer trainer;


    public LMSNetwork(final RootNetwork network, int numInputNeurons,
            int numOutputNeurons, Point2D initialPosition) {
        super(network, new int[] { numInputNeurons, numOutputNeurons },
                initialPosition);
        getOutputLayer().setNeuronType(new LinearNeuron());
        setLabel("LMS Network");
        trainer = new Trainer(network, this.getNeuronGroup(0).getNeuronList(),
                this.getNeuronGroup(1).getNeuronList(),
                new LMSIterative());
        
    }

    /**
     * @return the trainer
     */
    public Trainer getTrainer() {
        return trainer;
    }

}
