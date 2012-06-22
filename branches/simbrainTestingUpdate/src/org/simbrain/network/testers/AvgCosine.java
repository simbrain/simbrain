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

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.Subnetwork;

/**
 * Finds the average cosine between each output vector and each expected
 * output vector: 1 representing perfect accuracy, 0 representing total
 * orthogonality.
 *
 * @author ztosi
 */
public class AvgCosine  extends Tester{

    /** A running value of the cosine. */
	private double cos;

	/**
	 * Construct an average cosine tester.
	 * @param subNet a reference to the subnetwork being tested.
	 * @param network a reference to the testable network being tested.
	 */
	public AvgCosine(final Subnetwork subNet, final Testable network) {
		super(subNet, network);
		cos = 0.0;
	}

	@Override
	public final void apply() {
		fireTestingBegin();


		for (int i = 0; i < network.getTestingData().length; i++) {
			int j = 0;
			for (Neuron n : network.getInputNeurons()) {
				n.setActivation(network.getInputData()[i][j]);
				j++;
			}
			subNet.update();

			double [] acts = Network.getActivationVector(network.
					getOutputNeurons());

			cos += cosine(acts, network.getTestingData()[i]);
		}



	}

	/**
	 * Finds the dot product of two vectors (expressed as arrays).
	 *
	 * @param a vector 1.
	 * @param b vector 2.
	 * @return the dot product of the two vectors.
	 */
	public final double dotProduct(final double [] a,
			final double [] b) {
		if (a.length != b.length) {
			return Double.NaN;
		}
		double runningVal = 0.0;
		for (int i = 0; i < a.length; i++) {
			runningVal += a[i] * b[i];
		}
		return runningVal;
	}

	/**
	 * Returns the magnitude of the vector (expressed as an array).
	 * @param v the vector whose magnitude we're trying to find.
	 * @return the magnitude of vector (double []) v.
	 */
	public final double vectorMagnitude(final double [] v) {
		return Math.sqrt(dotProduct(v, v));
	}

	/**
	 * Finds the cosine of the angle between two vectors (expressed as
	 * arrays).
	 * @param a vector 1.
	 * @param b vector 2.
	 * @return the cosine of the angle between a and b.
	 */
	public final double cosine(final double [] a, final double [] b) {
		return dotProduct(a, b) / (vectorMagnitude(a)
				* (vectorMagnitude(b)));
	}



}
