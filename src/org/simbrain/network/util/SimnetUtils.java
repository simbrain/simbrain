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
package org.simbrain.network.util;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.Synapse;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * <b>SimnetUtils</b> provides utility classes relating to Simbrain networks.
 *
 * @author jyoshimi
 * @author ztosi
 */
public class SimnetUtils {

    /**
     * Returns the weights connecting two lists of neurons as an N x M matrix of
     * doubles, where N is the number of source neurons, and M is the number of
     * target neurons. That is, each row of the matrix corresponds to a source
     * neuron's fan-out weight vector.
     *
     * @param srcLayer source layer
     * @param targetLayer target layer
     * @return weight matrix
     */
    public static double[][] getWeights(List<Neuron> srcLayer,
            List<Neuron> targetLayer) {

        double[][] ret = new double[srcLayer.size()][targetLayer.size()];

        for (int i = 0; i < srcLayer.size(); i++) {
            for (int j = 0; j < targetLayer.size(); j++) {
                Synapse s = Network.getSynapse(srcLayer.get(i),
                        targetLayer.get(j));

                if (s != null) {
                    ret[i][j] = s.getStrength();
                } else {
                    ret[i][j] = 0;
                }
                // System.out.println("[" + i + "][" + j + "]" + ret[i][j]);
            }
        }
        return ret;
    }

    /**
     * Set the weights connecting two lists of neurons using a weight matrix.
     * Assumes that each row of the matrix corresponds to a source neuron's
     * fan-out weight vector, as above. Missing weights are ignored.
     *
     * @param src the list of source neurons
     * @param tar the list of target neurons
     * @param w the new weight values for the network.
     */
    public static void setWeights(final List<Neuron> src,
            final List<Neuron> tar, final double[][] w) {
        for (int i = 0; i < src.size(); i++) {
            for (int j = 0; j < tar.size(); j++) {
                Synapse s = Network.getSynapse(src.get(i), tar.get(j));
                if (s != null) {
                    s.setStrength(w[i][j]);
                }
            }
        }
    }

    /**
     * Set the weights connecting two lists of neurons using a weight matrix.
     * Assumes that each row of the matrix corresponds to a source neuron's
     * fan-out weight vector, as above. If a weight is missing it is added to
     * the provided network
     *
     * @param network network in which missing weights should be added.
     * @param src the list of source neurons
     * @param tar the list of target neurons
     * @param w the new weight values for the network.
     */
    public static void setWeightsFillBlanks(final Network network,
            final List<Neuron> src, final List<Neuron> tar,
            final double[][] w) {
        for (int i = 0; i < src.size(); i++) {
            for (int j = 0; j < tar.size(); j++) {
                Synapse s = Network.getSynapse(src.get(i), tar.get(j));
                if (s != null) {
                    s.setStrength(w[i][j]);
                } else {
                    Synapse newSynapse = new Synapse(src.get(i), tar.get(j));
                    newSynapse.setStrength(w[i][j]);
                    network.addSynapse(newSynapse);
                }
            }
        }
    }

    /**
     * Gets a matrix of Synapse objects, formatted like the getWeights method.
     * Non-existence synapses are given a null value.
     *
     * @param srcLayer source neurons
     * @param targetLayer target neurons
     * @return the matrix of synapses.
     */
    public static Synapse[][] getWeightMatrix(List<Neuron> srcLayer,
            List<Neuron> targetLayer) {

        Synapse[][] ret = new Synapse[srcLayer.size()][targetLayer.size()];

        for (int i = 0; i < srcLayer.size(); i++) {
            for (int j = 0; j < targetLayer.size(); j++) {
                Synapse s = Network.getSynapse(srcLayer.get(i),
                        targetLayer.get(j));

                if (s != null) {
                    ret[i][j] = s;
                    // System.out.println("[" + i + "][" + j + "]" +
                    // ret[i][j].getStrength());
                } else {
                    ret[i][j] = null;
                }
            }
        }
        return ret;
    }

    /**
     * Scales weights connecting source and target lists.
     *
     * @param src source neurons
     * @param tar target neurons
     * @param scalar scalar value which is multiplied by the weight matrix
     */
    public static void scaleWeights(List<Neuron> src, List<Neuron> tar,
            double scalar) {
        for (Neuron source : src) {
            for (Neuron target : tar) {
                Synapse weight = Network.getSynapse(source, target);
                if (weight != null) {
                    Network.getSynapse(source, target).setStrength(
                            weight.getStrength() * scalar);
                }
            }
        }
    }

    /**
     * Find the largest eigenvalue for the provided matrix.
     *
     * @param weightMatrix a matrix representation of the weights for use in
     *            linear algebraic operations
     * @return the largest eigenvalue of this matrix by absolute value
     */
    public static double findMaxEig(double[][] weightMatrix) {
        // get reservoir weight matrix
        Matrix resWeights = new Matrix(weightMatrix);

        // get an array of all the matrix's eigenvalues
        double[] eigs = new EigenvalueDecomposition(resWeights)
                .getRealEigenvalues();

        // lowest possible absolute value
        double maxEig = 0.0;

        // find largest eigenvalue by absolute value
        for (int i = 0; i < eigs.length; i++) {
            if (Math.abs(eigs[i]) > maxEig) {
                maxEig = Math.abs(eigs[i]);
            }
        }

        return maxEig;
    }

    /**
     * @param src list of source neurons
     * @param tar list of target neurons
     * @param desiredEigen : the new max eig or spectral radius for the weight
     *            matrix
     */
    public static void scaleEigenvalue(List<Neuron> src, List<Neuron> tar,
            double desiredEigen) {
        double maxEigen = findMaxEig(getWeights(src, tar));
        scaleWeights(src, tar, desiredEigen / maxEigen);
    }

    /**
     * Return the upper left corner of a list of objects, based on neurons.
     *
     * @param objects list of objects
     * @return the point corresponding to the upper left corner of the objects
     */
    public static Point2D getUpperLeft(final ArrayList objects) {
        double x = Double.MAX_VALUE;
        double y = Double.MAX_VALUE;

        for (final Object object : objects) {
            if (object instanceof Neuron) {
                Neuron neuron = (Neuron) object;
                if (neuron.getX() < x) {
                    x = neuron.getX();
                }
                if (neuron.getY() < y) {
                    y = neuron.getY();
                }
            } else if (object instanceof Network) {
                for (Neuron neuron : ((Network) object).getFlatNeuronList()) {
                    if (neuron.getX() < x) {
                        x = neuron.getX();
                    }
                    if (neuron.getY() < y) {
                        y = neuron.getY();
                    }
                }
            }
        }
        if (x == Double.MAX_VALUE) {
            x = 0;
        }
        if (y == Double.MAX_VALUE) {
            y = 0;
        }
        return new Point2D.Double(x, y);
    }

}
