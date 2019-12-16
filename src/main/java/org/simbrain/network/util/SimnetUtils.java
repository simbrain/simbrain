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

import org.simbrain.network.LocatableModel;
import org.simbrain.network.NetworkModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkTextObject;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.dl4j.NeuronArray;
import org.simbrain.network.groups.NeuronGroup;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;

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
                Synapse s = Network.getLooseSynapse(srcLayer.get(i), targetLayer.get(j));

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
     * Set the weights connecting two lists of neurons using a weight matrix. Assumes that each row of the matrix
     * corresponds to a source neuron's fan-out weight vector, as above. If a weight is missing it is added to the root
     * network (from where it can in some cases be routed to a SynapseGroup)
     *
     * @param src the list of source neurons
     * @param tar the list of target neurons
     * @param w   the new weight values for the network.
     */
    public static void setWeights(final List<Neuron> src, final List<Neuron> tar, final double[][] w) {
        for (int i = 0; i < src.size(); i++) {
            for (int j = 0; j < tar.size(); j++) {
                Synapse s = Network.getLooseSynapse(src.get(i), tar.get(j));
                if (s != null) {
                    s.forceSetStrength(w[i][j]);
                } else {
                    Synapse newSynapse = new Synapse(src.get(i), tar.get(j));
                    newSynapse.forceSetStrength(w[i][j]);
                    newSynapse.getParentNetwork().addLooseSynapse(newSynapse);
                }
            }
        }
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
                Synapse s = Network.getLooseSynapse(srcLayer.get(i), targetLayer.get(j));

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
                Synapse weight = Network.getLooseSynapse(source, target);
                if (weight != null) {
                    Network.getLooseSynapse(source, target).forceSetStrength(weight.getStrength() * scalar);
                }
            }
        }
    }

    /**
     * Return the center position of the upper-left-most object in a list of network objects.
     */
    public static Point2D getUpperLeft(final List<NetworkModel> networkObjects) {

        double x = Double.POSITIVE_INFINITY;
        double y = Double.POSITIVE_INFINITY;

        List<LocatableModel> locatableModels =
                networkObjects.stream()
                .filter(LocatableModel.class::isInstance)
                .map(LocatableModel.class::cast)
                .collect(Collectors.toList());

        for (LocatableModel model : locatableModels) {
            if (model.getCenterX() < x) {
                x = model.getCenterX();
            }
            if (model.getCenterY() < y) {
                y = model.getCenterY();
            }

            if (x == Double.POSITIVE_INFINITY) {
                x = 0;
            }
            if (y == Double.POSITIVE_INFINITY) {
                y = 0;
            }
        }
        return new Point2D.Double(x, y);
    }

    /**
     * Translate a set of network model object.
     */
    public static void translate(final List<NetworkModel> networkObjects, final Point2D translation) {
        List<LocatableModel> locatableModels =
                networkObjects.stream()
                        .filter(LocatableModel.class::isInstance)
                        .map(LocatableModel.class::cast)
                        .collect(Collectors.toList());

        for (LocatableModel model : locatableModels) {
            //System.out.println(model.getCenterX() + "," + model.getCenterY()
            //        + ":" + translation.getX() + "," + translation.getY());
            model.setCenterX(model.getCenterX() + translation.getX());
            model.setCenterY(model.getCenterY() + translation.getY());
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
     * Creates a deep copy of a list of network model elements: neurons, synapses,
     * and groups.  This is called first when a copy happens, then again when paste happens
     * (Need to be able to have a copy in case the copied object is deleted.   Needs
     * to be copied again on paste in case the parent network changes).
     *
     * @param newParent parent network for these objects. May be a root network
     *                  or a subnetwork.
     * @param items     the list of items to copy.
     * @return the list of copied items.
     */
    public static List<NetworkModel> getCopy(final Network newParent, final List<NetworkModel> items) {
        List<NetworkModel> ret = new ArrayList<>();
        // Match new to old neurons for synapse adding
        Hashtable<Neuron, Neuron> neuronMappings = new Hashtable<Neuron, Neuron>();
        ArrayList<Synapse> synapses = new ArrayList<Synapse>();

        for (Object item : items) {
            if (item instanceof Neuron) {
                Neuron oldNeuron = ((Neuron) item);
                Neuron newNeuron = new Neuron(newParent, oldNeuron);
                ret.add(newNeuron);
                neuronMappings.put(oldNeuron, newNeuron);
            } else if (item instanceof Synapse) {
                if (!isStranded((Synapse) item, items)) {
                    synapses.add((Synapse) item);
                }
            } else if (item instanceof NetworkTextObject) {
                NetworkTextObject text = ((NetworkTextObject) item);
                NetworkTextObject newText = new NetworkTextObject(newParent, text);
                ret.add(newText);
            } else if (item instanceof NeuronGroup) {
                ret.add(((NeuronGroup) item).deepCopy(newParent));
            } else if (item instanceof NeuronArray) {
                LocatableModel copy = ((NeuronArray) item).deepCopy(newParent, (NeuronArray) item);
                ret.add(copy);
            }
        }

        // Copy synapses
        for (Synapse synapse : synapses) {
            Synapse newSynapse = new Synapse(newParent, neuronMappings.get(synapse.getSource()), neuronMappings.get(synapse.getTarget()), synapse.getLearningRule().deepCopy(), synapse);
            ret.add(newSynapse);
        }

        return ret;
    }

    /**
     * Returns true if this synapse is not connected to two neurons (i.e. is
     * "stranded"), false otherwise.
     *
     * @param synapse  synapse to check
     * @param allItems includes neurons to check
     * @return true if this synapse is stranded, false otherwise
     */
    public static boolean isStranded(final Synapse synapse, final List<?> allItems) {

        // The list of checked neurons should include neurons in the list
        // as well as all neurons contained in networks in the list
        ArrayList<Neuron> check = new ArrayList<Neuron>();
        for (Object object : allItems) {
            if (object instanceof Neuron) {
                check.add((Neuron) object);
            } else if (object instanceof Network) {
                check.addAll(((Network) object).getFlatNeuronList());
            }
        }

        if (check.contains(synapse.getSource()) && (check.contains(synapse.getTarget()))) {
            return false;
        }
        return true;
    }

    /**
     * Returns the minimum X position of these model elements
     */
    public static double getMinX(List<? extends LocatableModel> models) {
        return models.stream().map(m -> m.getLocation().getX()).min(Double::compareTo).orElse(0.0);
    }

    /**
     * Returns the maximum X position of these model elements
     */
    public static double getMaxX(List<? extends LocatableModel> models) {
        return models.stream().map(m -> m.getLocation().getX()).max(Double::compareTo).orElse(0.0);
    }

    /**
     * Returns the min Y position of these model elements
     */
    public static double getMinY(List<? extends LocatableModel> models) {
        return models.stream().map(m -> m.getLocation().getY()).min(Double::compareTo).orElse(0.0);
    }

    /**
     * Returns the max Y position of these model elements
     */
    public static double getMaxY(List<? extends LocatableModel> models) {
        return models.stream().map(m -> m.getLocation().getY()).max(Double::compareTo).orElse(0.0);
    }

    /**
     * Returns the height (max y - min y) of a list of objects
     */
    public static double getHeight(List<? extends LocatableModel> models) {
        return getMaxY(models) - getMinY(models);
    }

    /**
     * Returns the height (max x - min x) of a list of objects
     */
    public static double getWidth(List<? extends LocatableModel> models) {
        return getMaxX(models) - getMinX(models);
    }
}
