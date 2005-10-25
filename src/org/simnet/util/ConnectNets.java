/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
import org.simnet.networks.Backprop;
import org.simnet.synapses.ClampedSynapse;


/**
 * <b>ConnectNets</b>
 */
public class ConnectNets {
    //TODO: Generlize beyond backprop
    public static void setConnections(Backprop container, Network src, double[][] w) {
        // Validate inputs.  Make sure this matrix can connect these layers
//			if ((src.getNeuronCount()!= w.length) || (tar.getNeuronCount() != w[0].length)) {
//				System.out.println("the weight matrix does not match the source and target layers");
//				return;
//			}
        for (int i = 0; i < w.length; i++) {
            for (int j = 0; j < w[i].length; j++) {
                // check to see if there is already a connection
                src.getWeight(i, j).setStrength(w[i][j]);
            }
        }
    }

    public static void oneWayFull(Network container, Network src, Network tar) {
        for (int i = 0; i < src.getNeuronCount(); i++) {
            for (int j = 0; j < tar.getNeuronCount(); j++) {
                ClampedSynapse s = new ClampedSynapse();
                s.setSource(src.getNeuron(i));
                s.setTarget(tar.getNeuron(j));
                container.addWeight(s);
            }
        }
    }

    // Should return a weight matrix of connections between src and tar nteworks
    public static double[][] getWeights(Network src, Network tar) {
        double[][] ret = new double[tar.getNeuronCount()][src.getNeuronCount()];

        for (int i = 0; i < src.getNeuronCount(); i++) {
            for (int j = 0; j < tar.getNeuronCount(); j++) {
                Synapse s = Network.getWeight(src.getNeuron(i), tar.getNeuron(j));

                if (s != null) {
                    ret[j][i] = s.getStrength();
                } else {
                    ret[j][i] = 0;
                }

                //System.out.println("[" + i + "][" + j + "]" + ret[i][j]);
            }
        }

        return ret;
    }
}
