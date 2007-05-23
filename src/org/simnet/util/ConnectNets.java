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
package org.simnet.util;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Synapse;
import org.simnet.synapses.ClampedSynapse;

/**
 * <b>ConnectNets</b> provides utilities for connecting the nodes of networks
 * together.
 */
public final class ConnectNets {

    /**
     * Private default constructor.
     */
    private ConnectNets() {
        // empty
    }

    /**
     * Set the weights connecting specified networks to values in a double matrix.
     *
     * @param src the source network whose outgoing weights need to be changed.
     * @param tar the target network whose incoming weights (from src) need to be changed.
     * @param w the new weight values for the network.
     */
    public static void setConnections(final Network src, final Network tar, final double[][] w) {
        for (int i = 0; i < src.getNeuronCount(); i++) {
            for (int j = 0; j < tar.getNeuronCount(); j++) {
                Synapse s = Network.getWeight(src.getNeuron(i), tar.getNeuron(j));
                if (s != null) {
                    s.setStrength(w[j][i]);
                }
            }
        }
    }

    /**
     * Set the weights of an existing simbrain network to those specified.
     *
     * @param src the network whose weights need to be changed.
     * @param w the new weight values for the network.
     */
    public static void setConnections(final Network src, final double[][] w) {
        // Validate inputs. Make sure this matrix can connect these layers
        // if ((src.getNeuronCount()!= w.length) || (tar.getNeuronCount() !=
        // w[0].length)) {
        // System.out.println("the weight matrix does not match the source and
        // target layers");
        // return;
        // }
        for (int i = 0; i < w.length; i++) {
            for (int j = 0; j < w[i].length; j++) {
                // check to see if there is already a connection
                src.getWeight(i, j).setStrength(w[i][j]);
            }
        }
    }

    /**
     * Connect every neuron in the source network to every neuron in the target network.
     *
     * @param container the network which contains the src and target subnetworks.
     * @param src the source network
     * @param tar the target network
     */
    public static void oneWayFull(final Network container, final Network src, final Network tar) {
        for (int i = 0; i < src.getNeuronCount(); i++) {
            for (int j = 0; j < tar.getNeuronCount(); j++) {
                ClampedSynapse s = new ClampedSynapse();
                s.setSource(src.getNeuron(i));
                s.setTarget(tar.getNeuron(j));
                container.addSynapse(s);
            }
        }
    }

    /**
     * Connect every neurons in source and target network 1-1.
     *
     * @param container the network which contains the src and target subnetworks.
     * @param src the source network
     * @param tar the target network
     */
    public static void oneWayOneOne(final Network container, final Network src, final Network tar) {
        if (src.getNeuronCount() != tar.getNeuronCount()) {
            return;
        }
        for (int i = 0; i < src.getNeuronCount(); i++) {
                ClampedSynapse s = new ClampedSynapse();
                s.setSource(src.getNeuron(i));
                s.setTarget(tar.getNeuron(i));
                container.addSynapse(s);
        }
    }

    /**
     * Returns a matrix representation of the connections between two
     * subnetworks.
     *
     * @param src the source network
     * @param tar the target network
     * @return a 2-d matrix of weight strengths
     */
    public static double[][] getWeights(final Network src, final Network tar) {
        double[][] ret = new double[tar.getNeuronCount()][src.getNeuronCount()];

        for (int i = 0; i < src.getNeuronCount(); i++) {
            for (int j = 0; j < tar.getNeuronCount(); j++) {
                Synapse s = Network.getWeight(src.getNeuron(i), tar
                        .getNeuron(j));

                if (s != null) {
                    ret[j][i] = s.getStrength();
                } else {
                    ret[j][i] = 0;
                }

                // System.out.println("[" + i + "][" + j + "]" + ret[i][j]);
            }
        }

        return ret;
    }
}
