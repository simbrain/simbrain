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

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.subnetworks.Hopfield;
import org.simbrain.util.math.SimbrainMath;

/**
 * Trainer for a Hopfield network.
 *
 * TODO: Add better Hopfield training! See
 * https://www.doc.ic.ac.uk/project/2012/163/g1216318/web/Refinements.html
 *
 * @author Jeff Yoshimi
 */
public class HopfieldTrainer extends Trainer {

    /** Reference to network being trained. */
    private final Hopfield hopfield;

    /**
     * Construct the Hopfield trainer.
     *
     * @param hop the hopfield network
     */
    public HopfieldTrainer(Hopfield hop) {
        super(hop);
        this.hopfield = hop;
    }

    @Override
    public void apply() throws DataNotInitializedException {

        if (getTrainableNetwork().getTrainingSet().getInputData() == null) {
            throw new DataNotInitializedException("Input data not initalized");
        }

        hopfield.getSynapseGroup().setStrengths(0);
        int numRows = hopfield.getTrainingSet().getInputData().length;
        int numInputs =  hopfield.getInputNeurons().size();
        float normConstant = 1 / (float) numRows;

        double[] vals = new double[numInputs * numInputs - numInputs];
        for (int row = 0; row < numRows; row++) {
            double[] pattern = hopfield.getTrainingSet().getInputData()[row];
            int k = 0;
            Neuron [] neurons = hopfield.getSynapseGroup().getSourceNeurons()
                    .toArray(new Neuron[pattern.length]);
            for (int i = 0; i < pattern.length; i++) {
                for (int j = 0; j < pattern.length; j++) {
                    if (i != j) {
                        Synapse s = neurons[i].getFanOut().get(neurons[j]);
                        s.setStrength(s.getStrength() + pattern[i] * pattern[j]);
                    }
                }
            }
        }
        vals = SimbrainMath.multVector(vals, normConstant);
        // Make sure excitatory/inhibitory are in proper lists
        hopfield.getSynapseGroup().revalidateSynapseSets();
        hopfield.getParentNetwork().fireNetworkChanged();
    }

}
