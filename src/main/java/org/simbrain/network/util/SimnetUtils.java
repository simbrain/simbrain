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
package org.simbrain.network.util;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.util.math.SimbrainMath;

import java.util.*;

import static org.simbrain.network.core.NetworkUtilsKt.getSynapse;

/**
 * <b>SimnetUtils</b> provides utility classes relating to Simbrain networks.
 *
 * @author jyoshimi
 * @author ztosi
 */
public class SimnetUtils {

    /**
     * Returns the weights connecting two lists of neurons as an N x M matrix of doubles, where N is the number of
     * source neurons, and M is the number of target neurons. That is, each row of the matrix corresponds to a source
     * neuron's fan-out weight vector.
     *
     * @param srcLayer    source layer
     * @param targetLayer target layer
     * @return weight matrix
     */
    public static double[][] getWeights(List<Neuron> srcLayer, List<Neuron> targetLayer) {

        double[][] ret = new double[srcLayer.size()][targetLayer.size()];

        for (int i = 0; i < srcLayer.size(); i++) {
            for (int j = 0; j < targetLayer.size(); j++) {
                Synapse s = getSynapse(srcLayer.get(i), targetLayer.get(j));

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
     * Gets a matrix of Synapse objects, formatted like the getWeights method. Non-existence synapses are given a null
     * value.
     *
     * @param srcLayer    source neurons
     * @param targetLayer target neurons
     * @return the matrix of synapses.
     */
    public static Synapse[][] getWeightMatrix(List<Neuron> srcLayer, List<Neuron> targetLayer) {

        Synapse[][] ret = new Synapse[srcLayer.size()][targetLayer.size()];

        for (int i = 0; i < srcLayer.size(); i++) {
            for (int j = 0; j < targetLayer.size(); j++) {
                Synapse s = getSynapse(srcLayer.get(i), targetLayer.get(j));

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
     * @param src    source neurons
     * @param tar    target neurons
     * @param scalar scalar value which is multiplied by the weight matrix
     */
    public static void scaleWeights(List<Neuron> src, List<Neuron> tar, double scalar) {
        for (Neuron source : src) {
            for (Neuron target : tar) {
                Synapse weight = getSynapse(source, target);
                if (weight != null) {
                    getSynapse(source, target).forceSetStrength(weight.getStrength() * scalar);
                }
            }
        }
    }

    /**
     * Given a source and target set of neurons, find all layers of neurons connecting them, as follows. Assumes a
     * sequence of layers from source to target, each fully connected to the next, and no other connections (e.g.
     * recurrent connections). If a path from the source to target layer is not found then a list containing only the
     * source and target layers is returned.
     *
     * @param network     the neural network
     * @param sourceLayer the source neurons
     * @param targetLayer the target neurons
     * @return the resulting list of layers
     */
    public static List<List<Neuron>> getIntermedateLayers(Network network, List<Neuron> sourceLayer, List<Neuron> targetLayer) {

        List<List<Neuron>> layers = new ArrayList<List<Neuron>>();
        layers.add(targetLayer);

        // Recursively add all hidden layers
        addPreviousLayer(layers, sourceLayer, targetLayer);
        Collections.reverse(layers); // So it's from source to target layers
        return layers;
    }

    /**
     * Helper method for getIntermedateLayers. Add the "next layer down" in the hierarchy.
     *
     * @param layers       the current set of layers
     * @param sourceLayer  the source layer
     * @param layerToCheck the current layer. Look for previous layers and if one is found add it to the layers.
     */
    private static void addPreviousLayer(List<List<Neuron>> layers, List<Neuron> sourceLayer, List<Neuron> layerToCheck) {

        // Stop adding layers when the number of layers exceeds this. Here
        // to prevent infinite recursions that result when invalid networks
        // are used with these methods. Perhaps there is a better way to
        // check for such a problem though...
        final int MAXLAYERS = 100;

        // The next layer. A Set to prevent duplicates.
        Set<Neuron> newLayerTemp = new HashSet<Neuron>();
        boolean theNextLayerIsTheSourceLayer = false;
        // Populate next layer
        for (Neuron neuron : layerToCheck) {
            for (Synapse synapse : neuron.getFanIn()) {
                Neuron sourceNeuron = synapse.getSource();
                if (sourceLayer.contains(sourceNeuron)) {
                    theNextLayerIsTheSourceLayer = true;
                }
                // Ignore recurrent connections
                if (sourceNeuron == neuron) {
                    continue;
                }
                newLayerTemp.add(synapse.getSource());
            }
        }

        if ((theNextLayerIsTheSourceLayer) || (newLayerTemp.size() == 0) || (layers.size() > MAXLAYERS)) {
            // We're done. We found the source layer or there was a problem. Add
            // the source layer and move on.
            layers.add(sourceLayer);
        } else {
            // Add this hidden layer then recursively add another layer, if
            // there is one.
            List<Neuron> newLayer = new ArrayList<Neuron>(newLayerTemp);
            Collections.sort(newLayer, OrientationComparator.X_ORDER);
            layers.add(newLayer);
            addPreviousLayer(layers, sourceLayer, newLayer);
        }
    }

    /**
     * Prints a group of layers (a list of lists of neurons), typically for debugging.
     *
     * @param layers the layers to print
     */
    public static void printLayers(List<List<Neuron>> layers) {
        for (List<Neuron> layer : layers) {
            System.out.println("Layer " + layers.indexOf(layer) + " has " + layer.size() + " elements");
        }
    }

    /**
     * Calculates the Euclidean distance between two neurons' positions in coordinate space.
     *
     * @param n1 The first neuron.
     * @param n2 The second neuron.
     */
    public static double getEuclideanDist(Neuron n1, Neuron n2) {
        return SimbrainMath.distance(n1.getLocation(), n2.getLocation());
    }


    /**
     * Return a list of excNeurons in a specific radius of a specified neuron.
     *
     * @param source the source neuron.
     * @param radius the radius to search within.
     * @return list of excNeurons in the given radius.
     */
    public static List<Neuron> getNeuronsInRadius(Neuron source, List<Neuron> neighbors, double radius) {
        ArrayList<Neuron> ret = new ArrayList<Neuron>();
        for (Neuron neuron : neighbors) {
            if (getEuclideanDist(source, neuron) < radius) {
                ret.add(neuron);
            }
        }
        return ret;
    }


}
