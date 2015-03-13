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

import java.util.List;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.randomizer.PolarizedRandomizer;

/**
 * Manage quick connection preferences, where a connection is applied using key
 * commands. Basically a repository of connection objects whose settings can be
 * changed.
 *
 * Not thread-safe. This class is used by gui methods, and is not intended to be
 * used in settings where concurrent threads are simultaneously building network
 * objects.
 *
 * @author Jeff Yoshimi
 */
public class QuickConnectionManager {

    /** Default percent of excitatory neurons. */
    private static final double DEFAULT_PERCENT_EXCITATORY = 1.0;

    /** The all to all connector. */
    private final AllToAll allToAll = new AllToAll();

    /** The one to one connector. */
    private final OneToOne oneToOne = new OneToOne();

    /** The sparse connector. */
    private final Sparse sparse = new Sparse();

    /** The current connection object. */
    private ConnectNeurons currentConnector;

    /** Whether excitatory connection should be randomized. */
    private boolean useExcitatoryRandomization = false;

    /** Whether excitatory connection should be randomized. */
    private boolean useInhibitoryRandomization = false;

    /** The current ratio of excitatory to inhibitory neurons. */
    private double excitatoryRatio = DEFAULT_PERCENT_EXCITATORY;

    /** The randomizer for excitatory synapses. */
    private PolarizedRandomizer exRandomizer = new PolarizedRandomizer(
            Polarity.EXCITATORY);

    /** The randomizer for inhibitory synapses. */
    private PolarizedRandomizer inRandomizer = new PolarizedRandomizer(
            Polarity.INHIBITORY);

    /**
     * Construct the quick connection manager.
     */
    public QuickConnectionManager() {
        currentConnector = allToAll;
    }

    /**
     * @return the connection objects
     */
    public ConnectNeurons[] getConnectors() {
        return new ConnectNeurons[] { allToAll, oneToOne, sparse };
    }

    /**
     * Apply the current connection object to indicated source and target
     * neurons.
     *
     * @param source source neurons
     * @param target target neurons
     */
    public void applyCurrentConnection(List<Neuron> source, List<Neuron> target) {
        List<Synapse> retList = null;
        if (currentConnector == allToAll) {
            retList = ((AllToAll) currentConnector).connectAllToAll(source,
                    target);
        } else if (currentConnector == oneToOne) {
            retList = ((OneToOne) currentConnector).connectOneToOne(source,
                    target);
        } else if (currentConnector == sparse) {
            retList = ((Sparse) currentConnector).connectSparse(source, target);
        }
        if (retList != null) {
            ConnectionUtilities.polarizeSynapses(retList, excitatoryRatio);
            if (this.isUseExcitatoryRandomization()) {
                ConnectionUtilities.randomizeExcitatorySynapses(retList,
                        exRandomizer);
            }
            if (this.isUseInhibitoryRandomization()) {
                ConnectionUtilities.randomizeInhibitorySynapses(retList,
                        exRandomizer);
            }
        }

    }

    /**
     * @return the currentConnector
     */
    public ConnectNeurons getCurrentConnector() {
        return currentConnector;
    }

    /**
     * @param currentConnector the currentConnector to set
     */
    public void setCurrentConnector(ConnectNeurons currentConnector) {
        this.currentConnector = currentConnector;
    }

    /**
     * @return the useExcitatoryRandomization
     */
    public boolean isUseExcitatoryRandomization() {
        return useExcitatoryRandomization;
    }

    /**
     * @param useExcitatoryRandomization the useExcitatoryRandomization to set
     */
    public void setUseExcitatoryRandomization(boolean useExcitatoryRandomization) {
        this.useExcitatoryRandomization = useExcitatoryRandomization;
    }

    /**
     * @return the useInhibitoryRandomization
     */
    public boolean isUseInhibitoryRandomization() {
        return useInhibitoryRandomization;
    }

    /**
     * @param useInhibitoryRandomization the useInhibitoryRandomization to set
     */
    public void setUseInhibitoryRandomization(boolean useInhibitoryRandomization) {
        this.useInhibitoryRandomization = useInhibitoryRandomization;
    }

    /**
     * @return the excitatoryRatio
     */
    public double getExcitatoryRatio() {
        return excitatoryRatio;
    }

    /**
     * @param excitatoryRatio the excitatoryRatio to set
     */
    public void setExcitatoryRatio(double excitatoryRatio) {
        this.excitatoryRatio = excitatoryRatio;
    }

    /**
     * @return the exRandomizer
     */
    public PolarizedRandomizer getExRandomizer() {
        return exRandomizer;
    }

    /**
     * @param exRandomizer the exRandomizer to set
     */
    public void setExRandomizer(PolarizedRandomizer exRandomizer) {
        this.exRandomizer = exRandomizer;
    }

    /**
     * @return the inRandomizer
     */
    public PolarizedRandomizer getInRandomizer() {
        return inRandomizer;
    }

    /**
     * @param inRandomizer the inRandomizer to set
     */
    public void setInRandomizer(PolarizedRandomizer inRandomizer) {
        this.inRandomizer = inRandomizer;
    }

    /**
     * @return the defaultPercentExcitatory
     */
    public static double getDefaultPercentExcitatory() {
        return DEFAULT_PERCENT_EXCITATORY;
    }

    /**
     * @return the allToAll
     */
    public AllToAll getAllToAll() {
        return allToAll;
    }

    /**
     * @return the oneToOne
     */
    public OneToOne getOneToOne() {
        return oneToOne;
    }

    /**
     * @return the sparse
     */
    public Sparse getSparse() {
        return sparse;
    }

}
