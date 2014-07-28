/*
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
import java.util.Collection;
import java.util.List;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.util.SimbrainConstants.Polarity;

/**
 * For each neuron, consider every neuron in an excitatory and inhibitory radius
 * from it, and make excitatory and inhibitory synapses with them.
 *
 * @author Zach Tosi
 *
 */
public class Radial extends Sparse {

    public static final double DEFAULT_DIST_CONST = 0.25;

    public static final double DEFAULT_EE_CONST = 0.3;

    public static final double DEFAULT_EI_CONST = 0.2;

    public static final double DEFAULT_IE_CONST = 0.4;

    public static final double DEFAULT_II_CONST = 0.1;

    public static final double DEFAULT_LAMBDA = 2.5;

    /** The connection constant for connections between 2 excitatory neurons. */
    private double eeDistConst = DEFAULT_EE_CONST;

    /**
     * The connection constant for connection from an excitatory to an
     * inhibitory neuron.
     */
    private double eiDistConst = DEFAULT_EI_CONST;

    /**
     * The connection constant for connection from an inhibitory to an
     * excitatory neuron.
     */
    private double ieDistConst = DEFAULT_IE_CONST;

    /** The connection constant for connections between 2 inhibitory neurons. */
    private double iiDistConst = DEFAULT_II_CONST;

    /**
     * The connection constant for general connections. Used in cases where
     * neurons have no explicit polarity.
     */
    private double distConst = DEFAULT_DIST_CONST;

    /**
     * A regulating constant governing overall connection density. Higher values
     * create denser connections. Lambda can be thought of as the average
     * connection distance.
     */
    private double lambda = DEFAULT_LAMBDA;

    private List<Synapse> removedList;

    private SynapseGroup synapseGroup;

    /**
     *
     * @param source
     * @param target
     * @param eeDistConst
     * @param eiDistConst
     * @param ieDistConst
     * @param iiDistConst
     * @param lambda
     * @param loose
     * @return
     */
    public static List<Synapse> connectRadialPolarized(
        final List<Neuron> source, final List<Neuron> target,
        double eeDistConst, double eiDistConst, double ieDistConst,
        double iiDistConst, double distConst, double lambda, boolean loose) {
        // Pre-allocating assuming that if one is using this as a connector
        // then they are probably not going to have greater than 25%
        // connectivity
        List<Synapse> synapses = new ArrayList<Synapse>(source.size()
            * target.size() / 4);
        for (Neuron src : source) {
            for (Neuron tar : target) {
                double randVal = Math.random();
                double probability;
                if (src.getPolarity() == Polarity.EXCITATORY) {
                    if (tar.getPolarity() == Polarity.EXCITATORY) {
                        probability = calcConnectProb(src, tar, eeDistConst,
                            lambda);
                    } else if (tar.getPolarity() == Polarity.INHIBITORY) {
                        probability = calcConnectProb(src, tar, eiDistConst,
                            lambda);
                    } else {
                        probability = calcConnectProb(src, tar, distConst,
                            lambda);
                    }
                } else if (src.getPolarity() == Polarity.INHIBITORY) {
                    if (tar.getPolarity() == Polarity.EXCITATORY) {
                        probability = calcConnectProb(src, tar, ieDistConst,
                            lambda);
                    } else if (tar.getPolarity() == Polarity.INHIBITORY) {
                        probability = calcConnectProb(src, tar, iiDistConst,
                            lambda);
                    } else {
                        probability = calcConnectProb(src, tar, distConst,
                            lambda);
                    }
                } else {
                    probability = calcConnectProb(src, tar, distConst,
                        lambda);
                }
                if (randVal < probability) {
                    Synapse s = new Synapse(src, tar);
                    synapses.add(s);
                    if (loose) {
                        src.getNetwork().addSynapse(s);
                    }
                }
            }
        }
        return synapses;
    }

    /**
     *
     * @param source
     * @param target
     * @param distConst
     * @param lambda
     * @param loose
     * @return
     */
    public static List<Synapse> connectRadialNoPolarity(
        final List<Neuron> source, final List<Neuron> target, double distConst,
        double lambda, boolean loose) {
        // Pre-allocating assuming that if one is using this as a connector
        // then they are probably not going to have greater than 25%
        // connectivity
        List<Synapse> synapses = new ArrayList<Synapse>(source.size()
            * target.size() / 4);
        for (Neuron src : source) {
            for (Neuron tar : target) {
                double randVal = Math.random();
                double probability = calcConnectProb(src, tar, distConst,
                    lambda);
                if (randVal < probability) {
                    Synapse s = new Synapse(src, tar);
                    synapses.add(s);
                    if (loose) {
                        src.getNetwork().addSynapse(s);
                    }
                }
            }
        }
        return synapses;
    }

    /**
     * Default constructor
     */
    public Radial() {
    }

    /**
     * @param distConst
     * @param lambda
     */
    public Radial(double distConst, double lambda) {
        this.distConst = distConst;
        this.lambda = lambda;
    }

    /**
     *
     * @param eeDistConst
     * @param eiDistConst
     * @param ieDistConst
     * @param iiDistConst
     * @param lambda
     */
    public Radial(double eeDistConst, double eiDistConst, double ieDistConst,
        double iiDistConst, double lambda) {
        this.eeDistConst = eeDistConst;
        this.eiDistConst = eiDistConst;
        this.ieDistConst = ieDistConst;
        this.iiDistConst = iiDistConst;
        this.lambda = lambda;
    }

    /**
     * {@inheritDoc} Specifically: Connects neurons based on a probability
     * function related to their distance from one another, which exponentially
     * decays with distance.
     */
    @Override
    public void connectNeurons(SynapseGroup synGroup) {
        this.synapseGroup = synGroup;
        List<Neuron> source = synGroup.getSourceNeurons();
        List<Neuron> target = synGroup.getTargetNeurons();
        List<Synapse> synapses = connectRadialPolarized(source, target,
            eeDistConst, eiDistConst, ieDistConst, iiDistConst, distConst,
            lambda, false);
        for (Synapse s : synapses) {
            synGroup.addNewSynapse(s);
        }
        if (synGroup.isRecurrent()) {
            connectionDensity = (double) synapses.size() /
                (synGroup.getSourceNeuronGroup().size()
                * (synGroup.getSourceNeuronGroup().size() - 1));
        } else {
            connectionDensity = (double) synapses.size() /
                (synGroup.getSourceNeuronGroup().size()
                * synGroup.getTargetNeuronGroup().size());
        }
        source = null;
        target = null;
        synapses = null;
        Runtime.getRuntime().gc();
    }

    public void adjustConnectivity(double density) {

    }

    private void removeToDensity(double density) {
        if (connectionDensity == 0)
            return;

    }

    private void addToDensity(double density) {
        if (connectionDensity == 1)
            return;
    }

    /**
     *
     * @param src
     * @param tar
     * @param distConst
     * @param lambda
     * @return
     */
    private static double calcConnectProb(Neuron src, Neuron tar,
        double distConst, double lambda) {
        double dist = -getRawDist(src, tar);
        double exp = Math.exp(dist / (lambda * lambda));
        if (exp == 1.0) { // Same location--same neuron: cheapest way to
            // prevent self connections
            exp = 0.0;
        }
        return distConst * exp;
    }

    /**
     *
     * @param n1
     * @param n2
     * @return
     */
    private static double getRawDist(Neuron n1, Neuron n2) {
        double x2 = (n1.getX() - n2.getX());
        x2 *= x2;
        double y2 = (n1.getY() - n2.getY());
        y2 *= y2;
        double z2 = (n1.getZ() - n2.getZ());
        z2 *= z2;
        return x2 + y2 + z2;
    }

    @Override
    public String toString() {
        return "Radial";
    }

    public double getEeDistConst() {
        return eeDistConst;
    }

    public void setEeDistConst(double eeDistConst) {
        this.eeDistConst = eeDistConst;
    }

    public double getEiDistConst() {
        return eiDistConst;
    }

    public void setEiDistConst(double eiDistConst) {
        this.eiDistConst = eiDistConst;
    }

    public double getIeDistConst() {
        return ieDistConst;
    }

    public void setIeDistConst(double ieDistConst) {
        this.ieDistConst = ieDistConst;
    }

    public double getIiDistConst() {
        return iiDistConst;
    }

    public void setIiDistConst(double iiDistConst) {
        this.iiDistConst = iiDistConst;
    }

    public double getDistConst() {
        return distConst;
    }

    public void setDistConst(double distConst) {
        this.distConst = distConst;
    }

    public double getLambda() {
        return lambda;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public double getConnectionDensity() {
        return 0;
    }

    @Override
    public Collection<Synapse> setConnectionDensity(double connectionDensity) {
        // TODO Auto-generated method stub
        return null;
    }

    public class DensityEstimator implements Runnable {

        private double estimateDensity;

        @Override
        public void run() {
            int count = 0;
            for (Neuron src : synapseGroup.getSourceNeurons()) {
                for (Neuron tar : synapseGroup.getTargetNeurons()) {
                    double randVal = Math.random();
                    double probability;
                    if (src.getPolarity() == Polarity.EXCITATORY) {
                        if (tar.getPolarity() == Polarity.EXCITATORY) {
                            probability =
                                calcConnectProb(src, tar, eeDistConst,
                                    lambda);
                        } else if (tar.getPolarity() == Polarity.INHIBITORY) {
                            probability =
                                calcConnectProb(src, tar, eiDistConst,
                                    lambda);
                        } else {
                            probability = calcConnectProb(src, tar, distConst,
                                lambda);
                        }
                    } else if (src.getPolarity() == Polarity.INHIBITORY) {
                        if (tar.getPolarity() == Polarity.EXCITATORY) {
                            probability =
                                calcConnectProb(src, tar, ieDistConst,
                                    lambda);
                        } else if (tar.getPolarity() == Polarity.INHIBITORY) {
                            probability =
                                calcConnectProb(src, tar, iiDistConst,
                                    lambda);
                        } else {
                            probability = calcConnectProb(src, tar, distConst,
                                lambda);
                        }
                    } else {
                        probability = calcConnectProb(src, tar, distConst,
                            lambda);
                    }
                    if (randVal < probability) {
                        count++;
                    }
                }
            }
            if (synapseGroup.isRecurrent()) {
                estimateDensity = (double) count / (synapseGroup
                    .getSourceNeuronGroup().size()
                    * (synapseGroup.getSourceNeuronGroup().size() - 1));
            } else {
                estimateDensity = (double) count / (synapseGroup.
                    getSourceNeuronGroup().size() * synapseGroup
                    .getTargetNeuronGroup().size());
            }
            synchronized (this) {
                notify();
            }
        }

        public double getDensityEsitmate() {
            return estimateDensity;
        }
    }

}
