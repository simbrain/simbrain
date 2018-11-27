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

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.propertyeditor2.EditableObject;

import java.util.List;

/**
 * Maintains a specific strategy for creating connections between two groups
 * of neurons.  Subclasses correspond to specific types of
 * connection strategy. Applied using static methods for loose
 * neurons and synapse groups.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
public abstract class ConnectionStrategy implements EditableObject {

    /**
     * Default ratio of excitatory neurons (from 0 to 1).
     */
    private static final double DEFAULT_EXCITATORY_RATIO = 1.0;

    /**
     * Whether excitatory connection should be randomized.
     */
    private boolean useExcitatoryRandomization = false;

    /**
     * Whether inhibitory connection should be randomized.
     */
    private boolean useInhibitoryRandomization = false;

    /**
     * The normalized ratio of excitatory to inhibitory neurons.
     * A value between 0 (all inhibitory) and 1 (all excitatory).
     */
    private double excitatoryRatio = DEFAULT_EXCITATORY_RATIO;

    /**
     * The randomizer for excitatory synapses.
     */
    private ProbabilityDistribution exRandomizer =
        UniformDistribution.builder()
            .polarity(SimbrainConstants.Polarity.EXCITATORY)
            .build();

    /**
     * The randomizer for inhibitory synapses.
     */
    private ProbabilityDistribution inRandomizer =
        UniformDistribution.builder()
            .polarity(SimbrainConstants.Polarity.INHIBITORY)
            .build();

    /**
     * Apply connection to a synapse group using specified parameters.
     *
     * @param synGroup synapse group
     */
    public abstract void connectNeurons(final SynapseGroup synGroup);

    /**
     * Apply connection to a set of loose neurons.
     *
     * @param network parent network loose neuron
     * @param source  source neurons
     * @param target  target neurons
     * @return the resulting list of synapses, which are sometimes needed for
     * other operations
     */
    public abstract List<Synapse> connectNeurons(Network network, List<Neuron> source, List<Neuron> target);


    public boolean isUseExcitatoryRandomization() {
        return useExcitatoryRandomization;
    }

    public void setUseExcitatoryRandomization(boolean useExcitatoryRandomization) {
        this.useExcitatoryRandomization = useExcitatoryRandomization;
    }

    public boolean isUseInhibitoryRandomization() {
        return useInhibitoryRandomization;
    }

    public void setUseInhibitoryRandomization(boolean useInhibitoryRandomization) {
        this.useInhibitoryRandomization = useInhibitoryRandomization;
    }

    public double getExcitatoryRatio() {
        return excitatoryRatio;
    }

    public void setExcitatoryRatio(double excitatoryRatio) {
        this.excitatoryRatio = excitatoryRatio;
    }

    public ProbabilityDistribution getExRandomizer() {
        return exRandomizer;
    }

    public ProbabilityDistribution getInRandomizer() {
        return inRandomizer;
    }

    public void setExRandomizer(ProbabilityDistribution exRandomizer) {
        this.exRandomizer = exRandomizer;
    }

    public void setInRandomizer(ProbabilityDistribution inRandomizer) {
        this.inRandomizer = inRandomizer;
    }
}