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

import org.simbrain.network.subnetworks.CompetitiveNetwork;

/**
 * A trainer for SOM Networks. Just goes through input data sets input node and
 * updates the SOM Group, which has the training code built in.
 *
 * TODO: Possibly refactor to an "unsupervised trainer" class for use by
 * competitive too, which is similar.
 *
 * @author Jeff Yoshimi
 */
public class CompetitiveTrainer extends Trainer {

    /** Reference to trainable network. */
    private final CompetitiveNetwork network;

    /** Flag used for iterative training methods. */
    private boolean updateCompleted = true;

    /** Iteration number. An epoch. */
    private int iteration = 0;

    /**
     * Construct the competitive network trainer. //TODO: Fix javadoc in SOM
     *
     * @param network the parent network
     */
    public CompetitiveTrainer(CompetitiveNetwork network) {
        super(network);
        this.network = network;
        this.setIteration(0);

    }

    @Override
    public void apply() throws DataNotInitializedException {

        if (network.getTrainingSet().getInputData() == null) {
            throw new DataNotInitializedException("Input data not initalized");
        }

        int numRows = network.getTrainingSet().getInputData().length;
        for (int row = 0; row < numRows; row++) {
            double[] inputs = network.getTrainingSet().getInputData()[row];
            network.getInputLayer().forceSetActivations(inputs);
            network.getCompetitive().update(); // Call a function here to be overriden in subclasses?
        }
        incrementIteration();

    }

    /**
     * @return boolean updated completed.
     */
    public boolean isUpdateCompleted() {
        return updateCompleted;
    }

    /**
     * Sets updated completed value.
     *
     * @param updateCompleted Updated completed value to be set
     */
    public void setUpdateCompleted(final boolean updateCompleted) {
        this.updateCompleted = updateCompleted;
    }

    /**
     * Increment the iteration number by 1.
     */
    public void incrementIteration() {
        iteration++;
    }

    /**
     * @param iteration the iteration to set
     */
    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    /**
     * Return the current iteration.
     *
     * @return current iteration.
     */
    public int getIteration() {
        return iteration;
    }

}
