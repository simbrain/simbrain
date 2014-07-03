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
package org.simbrain.network.connections;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.randomizer.PolarizedRandomizer;

/**
 * Properties for quick connect, where a connection is applied using a key
 * command, without a dialog. In this way many connections can be quickly
 * created. Not thread-safe. This class is used by gui methods, and is not
 * intended to be used in settings where concurrent threads are simultaneously
 * building network objects.
 *
 * @author jyoshimi
 */
public class QuickConnectPreferences {

    public enum ConnectType {

        ALL_TO_ALL {

            @Override
            public String toString() {
                return "All to all";
            }

            @Override
            public List<Synapse> applyConnection(List<Neuron> source,
                List<Neuron> target) {
                List<Synapse> retList = AllToAll.connectAllToAll(source, target,
                    source.equals(target), AllToAll.DEFAULT_SELF_CONNECT_PREF,
                    true);
                ConnectionUtilities.polarizeSynapses(retList, 1.0);
                return retList;
            }

            @Override
            public ConnectNeurons getDefaultInstance() {
                return new AllToAll();
            }

        },
        SPARSE {

            @Override
            public String toString() {
                return "Sparse";
            }

            @Override
            public List<Synapse> applyConnection(List<Neuron> source,
                List<Neuron> target) {
                List<Synapse> retList = Sparse.connectSparse(source, target,
                    Sparse.DEFAULT_CONNECTION_DENSITY,
                    Sparse.DEFAULT_SELF_CONNECT_PREF,
                    Sparse.DEFAULT_FF_PREF, true);
                ConnectionUtilities.randomizeAndPolarizeSynapses(retList, .5);
                return retList;
            }

            @Override
            public ConnectNeurons getDefaultInstance() {
                return new Sparse();
            }

        },
        ONE_TO_ONE {

            @Override
            public String toString() {
                return "One to one";
            }

            @Override
            public List<Synapse> applyConnection(List<Neuron> source,
                List<Neuron> target) {
                List<Synapse> retList =  OneToOne.connectOneToOne(source, target,
                    OneToOne.DEFAULT_BIDIRECT_PREF, true);
                ConnectionUtilities.polarizeSynapses(retList, 1.0);
                return retList;
            }

            @Override
            public ConnectNeurons getDefaultInstance() {
                return new OneToOne();
            }

        },
        RADIAL {

            @Override
            public String toString() {
                return "Radial";
            }

            @Override
            public List<Synapse> applyConnection(List<Neuron> source,
                List<Neuron> target) {
                return new ArrayList<Synapse>();
            }

            @Override
            public ConnectNeurons getDefaultInstance() {
                return null;
            }

        };

        public abstract List<Synapse> applyConnection(List<Neuron> source,
            List<Neuron> target);

        /**
         * @return an instance of a the connector associated with this
         *         connection type with all default and/or static parameters.
         */
        public abstract ConnectNeurons getDefaultInstance();

    }

    /**
     * Default connection type for quick connect.
     */
    private static final ConnectType DEFAULT_CONNECTION =
        ConnectType.ALL_TO_ALL;

    private static final double DEFAULT_PERCENT_EXCITATORY = 1.0;

    private static final PolarizedRandomizer DEFAULT_EX_RAND =
        new PolarizedRandomizer(
            Polarity.EXCITATORY);

    private static final PolarizedRandomizer DEFAULT_IN_RAND =
        new PolarizedRandomizer(
            Polarity.INHIBITORY);

    private static final SynapseUpdateRule DEFAULT_PLASTICITY =
        new StaticSynapseRule();

    /**
     * Static set of connection types. These are modified and the current one is
     * used for quick connect.
     */
    private static final ConnectType[] connectionTypes = new ConnectType[] {
            ConnectType.ALL_TO_ALL, ConnectType.ONE_TO_ONE, ConnectType.SPARSE };
    // Radial is not currently entabled
    //private static final ConnectType[] connectionTypes = ConnectType.values();

    /**
     * Holds "current" connection object.
     */
    private static ConnectType currentConnection = DEFAULT_CONNECTION;

    public static double percentExcitatory = DEFAULT_PERCENT_EXCITATORY;

    public static PolarizedRandomizer excitatoryRandomizer = DEFAULT_EX_RAND;

    public static PolarizedRandomizer inhibitoryRandomizer = DEFAULT_IN_RAND;

    public static SynapseUpdateRule excitatoryPlasticity = DEFAULT_PLASTICITY;

    public static SynapseUpdateRule inhibitoryPlasticity = DEFAULT_PLASTICITY;

    public static boolean useExcitatoryRandomization;

    public static boolean useInhibitoryRandomization;

    /**
     * @return the currentConnection
     */
    public static ConnectType getCurrentConnection() {
        return currentConnection;
    }

    /**
     * @param currentConnection
     *            the currentConnection to set
     */
    public static void setCurrentConnection(ConnectType currentConnection) {
        QuickConnectPreferences.currentConnection = currentConnection;
    }

    /**
     * @return the connectiontypes
     */
    public static ConnectType[] getConnectiontypes() {
        return connectionTypes;
    }

    /**
     *
     * @param source
     * @param target
     * @return
     */
    public static List<Synapse> createConnections(List<Neuron> source,
        List<Neuron> target) {
        List<Synapse> synapses = currentConnection.applyConnection(source,
            target);
        ConnectionUtilities.polarizeSynapses(synapses, percentExcitatory);
        if (useExcitatoryRandomization && useInhibitoryRandomization) {
            ConnectionUtilities.randomizeSynapses(synapses,
                excitatoryRandomizer, inhibitoryRandomizer);
        } else if (useExcitatoryRandomization) {
            ConnectionUtilities.randomizeExcitatorySynapses(synapses,
                excitatoryRandomizer);
        } else if (useInhibitoryRandomization) {
            ConnectionUtilities.randomizeInhibitorySynapses(synapses,
                inhibitoryRandomizer);
        }
        return synapses;
    }

    /**
     *
     */
    public static void restoreDefaults() {
        percentExcitatory = DEFAULT_PERCENT_EXCITATORY;
        excitatoryRandomizer = DEFAULT_EX_RAND;
        inhibitoryRandomizer = DEFAULT_IN_RAND;
        useExcitatoryRandomization = false;
        useInhibitoryRandomization = false;
        excitatoryPlasticity = DEFAULT_PLASTICITY;
        inhibitoryPlasticity = DEFAULT_PLASTICITY;
    }

}
