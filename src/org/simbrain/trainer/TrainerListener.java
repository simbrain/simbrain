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
package org.simbrain.trainer;

import java.util.List;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;

/**
 * Observer class for trainer objects.
 *
 * @author jyoshimi
 */
public interface TrainerListener {

    /**
     * Called when the error value is updated.
     *
     * @param error new error value
     */
    void errorUpdated(double error);

    /**
     * The trainer's network changed.
     *
     * @param oldNetwork the old network
     * @param newNetwork the new network
     */
    void networkChanged(Network oldNetwork, Network newNetwork);

    /**
     * The trainer's input data changed.
     *
     * @param inputData the new input data.
     */
    void inputDataChanged(double[][] inputData);

    /**
     * The trainer's training data changed.
     *
     * @param trainingData the new training data
     */
    void trainingDataChanged(double[][] trainingData);

    /**
     * The trainer's input layer changed.
     *
     * @param inputLayer the new input layer
     */
    void inputLayerChanged(List<Neuron> inputLayer);

    /**
     * The trainer's output layer changed.
     *
     * @param outputLayer the new output layer
     */
    void outputLayerChanged(List<Neuron> outputLayer);

}
