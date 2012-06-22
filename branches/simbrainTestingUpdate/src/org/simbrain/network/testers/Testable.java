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
package org.simbrain.network.testers;

import java.util.List;

import org.simbrain.network.core.Neuron;

/**
 * Interface that must be implemented by any object that can be tested by a
 * tester.
 *
 * @author jyoshimi
 * @author ztosi
 *
 */
public interface Testable {

    /**
     * Returns the list of input neurons.
     *
     * @return a list of the intput neurons.
     */
	List<Neuron> getInputNeurons();

	/**
     * Returns the list of output neurons.
     *
     * @return a list of the output neurons.
     */
	List<Neuron> getOutputNeurons();

    /**
     * Return input data. Must be same number of columns as input neurons.
     *
     * @return the input data during testing.
     */
    double[][] getInputData();

    /**
     * Return training data. Must be same number of columns as output neurons.
     *
     * @return the expected output data during testing (if this is known).
     */
    double[][] getTestingData();


}
