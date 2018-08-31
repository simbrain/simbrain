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

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor2.EditableObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * For each neuron, consider every neuron in an excitatory and inhibitory radius
 * from it, and make excitatory and inhibitory synapses with them according to
 * some probability. Inhibitory and excitatory synapses are created separately
 * and use separate parameters. Therefore the total number of connections that will
 * be made depends upon both sets of parameters. 
 * <p>
 * Currently this is not accessible in the GUI, and is only used by some
 * scripts.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
public class RadialSimple implements ConnectNeurons, EditableObject {

    /**
     * When connecting neurons within a radius of a given neuron they can be chosen
     * stochastically, based on some probability parameter ({@link #PROBABILISTIC}) or
     * deterministically based upon a predefined number of requested connections
     * ({@link #DETERMINISTIC}).
     */
    public enum ConnectStyle {

        PROBABILISTIC ("Probabilistic"),
        DETERMINISTIC("Deterministic");

        private final String description;

        ConnectStyle(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Are neurons within a given radius being connected <emp>to</emp> the neuron in
     * question ({@link #IN} or are they being connected <emp>from</emp> the neuron in
     * question ({@link #OUT})? Equivalently is the neuron for which are checking for
     * other neurons within a given distance the target of the connections that will
     * made or the source?
     */
    public enum SelectionStyle {

        OUT ("Outward"),
        IN ("Inward");
        private final String description;

        SelectionStyle(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Should connections be selected randomly from a given neighborhood or in a prescribed way
     * based on the in-degree?
     */
    @UserParameter(label = "Connection Method", description = "Make local connections based on a specified in-degree (determnistic) or randomly (probabilistic)",order = 1)
    private ConnectStyle conMethod = ConnectStyle.PROBABILISTIC;

    @UserParameter(label = "Inward / Outward", description = "Are the connections to be made 'inward' (connections sent in to each neuron) or 'outward' (connections radiating out from each neuron).",order = 2)
    private SelectionStyle selectMethod = SelectionStyle.IN;

    /**
     * Whether to allow self-connections.
     */
    @UserParameter(label = "Self-Connections Allowed ", description = "Can there exist synapses whose source and target are the same?",
            order = 9)
    private boolean allowSelfConnections = false;

    /**
     * Template synapse for excitatory synapses.
     */
    private Synapse baseExcitatorySynapse = Synapse.getTemplateSynapse();

    /**
     * Probability of making connections to neighboring excitatory neurons. Also used for
     * neurons with no polarity.
     */
    @UserParameter(label = "Exc. Probability", description = "Probability connections will be made to neighbor excitatory (or non-polar) neurons ",
            minimumValue = 0, defaultValue = "0.8", order = 5)
    private double excitatoryProbability = .8;

    /**
     * The number of connections allowed with excitatory (or non-polar) neurons. If there
     * are sufficient excitatory (or non-polar) neurons in a given neuron's neighborhood this
     * will be how many connections are made.
     */
    @UserParameter(label = "Num. Exc. Connections", description = "Maximum # of connections with exc. neurons",
            minimumValue = 0, defaultValue = "5", order = 7)
    private int excCons = 5;

    /**
     * Radius within which to connect excitatory excNeurons.
     */
    @UserParameter(label = "Exc. Radius", description = "Distance to search for excitatory neurons to connect to",
            minimumValue = 0, defaultValue = "100", order = 3)
    private double excitatoryRadius = 100;

    /**
     * Template synapse for inhibitory synapses.
     */
    private Synapse baseInhibitorySynapse = Synapse.getTemplateSynapse();

    /**
     * Radius within which to connect inhibitory excNeurons.
     */
    @UserParameter(label = "Inh. Radius", description = "Distance to search for inhibitory neurons to connect to",
            minimumValue = 0, defaultValue = "80", order = 4)
    private double inhibitoryRadius = 80;

    /**
     * Probability of designating a given synapse excitatory. If not, it's
     * inhibitory.
     */
    @UserParameter(label = "Inh. Probability", description = "Probability connections will be made to neighbor inhibitory neurons ",
            minimumValue = 0, defaultValue = "0.8", order = 6)
    private double inhibitoryProbability = .8;

    /**
     * The number of connections allowed with inhibitory neurons. If there
     * are sufficient inhibitory neurons in a given neuron's neighborhood this
     * will be how many connections are made.
     */
    @UserParameter(label = "Num. Inh. Connections", description = "Maximum # of connections with inh. neurons",
            minimumValue = 0, defaultValue = "5", order = 8)
    private int inhCons = 5;

    /**
     * Reference to network in which radial connections will be made on loose
     * synapses.
     */
    private Network network;

    /**
     * List containing neurons with excitatory polarity among the neurons selected
     * to form connections between
     */
    private List<Neuron> excNeurons;

    /**
     * List containing neurons with inhibitory polarity among the neurons selected
     * to form connections between
     */
    private List<Neuron> inhNeurons;

    /**
     * A list containing all non polar neurons among the neurons selected to form connections
     * between
     */
    private List<Neuron> nonPolarNeurons;

    //TODO
    public RadialSimple() {

    }

    /**
     * @param network       the network
     * @param neurons the neurons that radial connections are to be made between
     */
    public RadialSimple(Network network, List<Neuron> neurons) {
        super();
        this.network = network;
        excNeurons = neurons.stream().filter(neuron -> neuron.getPolarity()
                == SimbrainConstants.Polarity.EXCITATORY ||
                neuron.getPolarity()
                        == SimbrainConstants.Polarity.BOTH).collect(Collectors.toList());
        inhNeurons = neurons.stream().filter(neuron -> neuron.getPolarity()
                == SimbrainConstants.Polarity.INHIBITORY).collect(Collectors.toList());
    }


    /**
     * Make the connections.
     *
     * @param looseSynapses whether loose synapses are being connected.
     * @return the new synapses.
     */
    public List<Synapse> connectNeurons(final boolean looseSynapses) {
        ArrayList<Synapse> syns = new ArrayList<Synapse>();
        for (Neuron source : excNeurons) {
            makeExcitatory(source, syns, looseSynapses);
            makeInhibitory(source, syns, looseSynapses);
        }
        for (Neuron source : inhNeurons) {
            makeExcitatory(source, syns, looseSynapses);
            makeInhibitory(source, syns, looseSynapses);
        }
        return syns;
    }

    /**
     * Make an inhibitory neuron, in the sense of connecting this neuron with
     * surrounding excNeurons via excitatory connections.
     *
     * @param neuron
     */
    private void makeInhibitory(final Neuron neuron, List<Synapse> syns, boolean looseSynapses) {
        int degreeCounter = 0;
        List<Neuron> neusInRadius = getNeuronsInRadius(neuron, inhNeurons, inhibitoryRadius);
        neusInRadius.addAll(getNeuronsInRadius(neuron, nonPolarNeurons, inhibitoryRadius));
        if (conMethod == ConnectStyle.DETERMINISTIC) {
            Collections.shuffle(neusInRadius);
        }
        for (Neuron otherNeu : neusInRadius) {
            // Don't add a connection if there is already one present
            if (Network.getSynapse(neuron, otherNeu) != null) {
                continue;
            }
            if (!allowSelfConnections) {
                if (neuron == otherNeu) {
                    continue;
                }
            }
            if (conMethod == ConnectStyle.PROBABILISTIC) {
                if (Math.random() < inhibitoryProbability) {
                    Synapse synapse;
                    if (selectMethod == SelectionStyle.IN)
                        synapse = new Synapse(otherNeu, neuron);
                    else {
                        synapse = new Synapse(neuron, otherNeu);
                    }
                    synapse.setStrength(-Math.random());
                    if (looseSynapses) {
                        network.addSynapse(synapse);
                    } else {
                        if (syns != null)
                            syns.add(synapse);
                    }
                }
            } else {
                Synapse synapse;
                if (selectMethod == SelectionStyle.IN)
                    synapse = new Synapse(otherNeu, neuron);
                else {
                    synapse = new Synapse(neuron, otherNeu);
                }
                synapse.setStrength(-Math.random());
                if (looseSynapses) {
                    network.addSynapse(synapse);
                } else {
                    if (syns != null)
                        syns.add(synapse);
                }
                degreeCounter++;
                if(degreeCounter >= inhCons) {
                    network.fireSynapsesUpdated();
                    break;
                }
            }
            network.fireSynapsesUpdated();
        }
    }

    /**
     * Make an excitatory neuron, in the sense of connecting this neuron with
     * surrounding excNeurons via excitatory connections.
     *
     * @param neuron neuron neuron
     */
    private void makeExcitatory(final Neuron neuron, List<Synapse> syns, boolean looseSynapses) {
        int degreeCounter = 0;
        List<Neuron> neusInRadius = getNeuronsInRadius(neuron, excNeurons, excitatoryRadius);
        neusInRadius.addAll(getNeuronsInRadius(neuron, nonPolarNeurons, excitatoryRadius));
        if (conMethod == ConnectStyle.DETERMINISTIC) {
            Collections.shuffle(neusInRadius);
        }
        for (Neuron otherNeu : neusInRadius) {
            // Don't add a connection if there is already one present
            if (Network.getSynapse(neuron, otherNeu) != null) {
                continue;
            }
            if (!allowSelfConnections) {
                if (neuron == otherNeu) {
                    continue;
                }
            }
            if (conMethod == ConnectStyle.PROBABILISTIC) {
                if (Math.random() < excitatoryProbability) {
                    Synapse synapse;
                    if (selectMethod == SelectionStyle.IN)
                        synapse = new Synapse(otherNeu, neuron);
                    else {
                        synapse = new Synapse(neuron, otherNeu);
                    }
                    synapse.setStrength(Math.random());
                    if (looseSynapses) {
                        network.addSynapse(synapse);
                    } else {
                        if (syns != null)
                            syns.add(synapse);
                    }
                }
            } else {
                Synapse synapse;
                if (selectMethod == SelectionStyle.IN)
                    synapse = new Synapse(otherNeu, neuron);
                else {
                    synapse = new Synapse(neuron, otherNeu);
                }
                synapse.setStrength(Math.random());
                if (looseSynapses) {
                    network.addSynapse(synapse);
                } else {
                    if (syns != null)
                        syns.add(synapse);
                }
                degreeCounter++;
                if(degreeCounter >= excCons) {
                    network.fireSynapsesUpdated();
                    break;
                }
            }
            network.fireSynapsesUpdated();
        }
    }


    @Override
    public void connectNeurons(SynapseGroup synGroup) {
        // No implementation yet.
        List<Neuron> target = synGroup.getTargetNeurons();
        List<Neuron> source = synGroup.getSourceNeurons();
        List<Synapse> exSyns = new ArrayList<>();
        List<Synapse> inSyns = new ArrayList<>();
        excNeurons = target.stream().filter(neuron -> neuron.getPolarity()
                == SimbrainConstants.Polarity.EXCITATORY).collect(Collectors.toList());
        inhNeurons = target.stream().filter(neuron -> neuron.getPolarity()
                == SimbrainConstants.Polarity.INHIBITORY).collect(Collectors.toList());
        nonPolarNeurons = target.stream().filter(neuron -> neuron.getPolarity()
                == SimbrainConstants.Polarity.BOTH).collect(Collectors.toList());
        for (Neuron src : source) {
            makeExcitatory(src, exSyns, false);
            makeInhibitory(src, inSyns, false);
        }

        for(Synapse s : exSyns) {
            synGroup.addNewExcitatorySynapse(s);
        }

        for(Synapse s : inSyns) {
            synGroup.addNewInhibitorySynapse(s);
        }
    }

    @Override
    public List<Synapse> connectNeurons(Network network, List<Neuron> source, List<Neuron> target) {
        this.network = network;
        excNeurons = target.stream().filter(neuron -> neuron.getPolarity()
                == SimbrainConstants.Polarity.EXCITATORY).collect(Collectors.toList());
        inhNeurons = target.stream().filter(neuron -> neuron.getPolarity()
                == SimbrainConstants.Polarity.INHIBITORY).collect(Collectors.toList());
        nonPolarNeurons = target.stream().filter(neuron -> neuron.getPolarity()
                == SimbrainConstants.Polarity.BOTH).collect(Collectors.toList());

        for (Neuron src : source) {
            makeExcitatory(src, null, true);
            makeInhibitory(src, null, true);
        }
        return null;
    }

    /**
     * Return a list of excNeurons in a specific radius of a specified neuron.
     *
     * @param source the source neuron.
     * @param radius the radius to search within.
     * @return list of excNeurons in the given radius.
     */
    private List<Neuron> getNeuronsInRadius(Neuron source, List<Neuron> neighbors, double radius) {
        ArrayList<Neuron> ret = new ArrayList<Neuron>();
        for (Neuron neuron : neighbors) {
            if (network.getEuclideanDist(source, neuron) < radius) {
                ret.add(neuron);
            }
        }
        return ret;
    }

    /**
     * @return the allowSelfConnections
     */
    public boolean isAllowSelfConnections() {
        return allowSelfConnections;
    }

    /**
     * @param allowSelfConnections the allowSelfConnections to set
     */
    public void setAllowSelfConnections(final boolean allowSelfConnections) {
        this.allowSelfConnections = allowSelfConnections;
    }

    /**
     * @return the excitatoryProbability
     */
    public double getExcitatoryProbability() {
        return excitatoryProbability;
    }

    /**
     * @param excitatoryProbability the excitatoryProbability to set
     */
    public void setExcitatoryProbability(final double excitatoryProbability) {
        this.excitatoryProbability = excitatoryProbability;
    }

    /**
     * @return the excitatoryRadius
     */
    public double getExcitatoryRadius() {
        return excitatoryRadius;
    }

    /**
     * @param excitatoryRadius the excitatoryRadius to set
     */
    public void setExcitatoryRadius(final double excitatoryRadius) {
        this.excitatoryRadius = excitatoryRadius;
    }

    /**
     * @return the inhibitoryRadius
     */
    public double getInhibitoryRadius() {
        return inhibitoryRadius;
    }

    /**
     * @param inhibitoryRadius the inhibitoryRadius to set
     */
    public void setInhibitoryRadius(final double inhibitoryRadius) {
        this.inhibitoryRadius = inhibitoryRadius;
    }

    /**
     * @return the inhibitoryProbability
     */
    public double getInhibitoryProbability() {
        return inhibitoryProbability;
    }

    /**
     * @param inhibitoryProbability the inhibitoryProbability to set
     */
    public void setInhibitoryProbability(final double inhibitoryProbability) {
        this.inhibitoryProbability = inhibitoryProbability;
    }

    /**
     * @return the baseExcitatorySynapse
     */
    public Synapse getBaseExcitatorySynapse() {
        return baseExcitatorySynapse;
    }

    /**
     * @param baseExcitatorySynapse the baseExcitatorySynapse to set
     */
    public void setBaseExcitatorySynapse(Synapse baseExcitatorySynapse) {
        this.baseExcitatorySynapse = baseExcitatorySynapse;
    }

    /**
     * @return the baseInhibitorySynapse
     */
    public Synapse getBaseInhibitorySynapse() {
        return baseInhibitorySynapse;
    }

    /**
     * @param baseInhibitorySynapse the baseInhibitorySynapse to set
     */
    public void setBaseInhibitorySynapse(Synapse baseInhibitorySynapse) {
        this.baseInhibitorySynapse = baseInhibitorySynapse;
    }

    public int getExcCons() {
        return excCons;
    }

    public int getInhCons() {
        return inhCons;
    }

    public void setExcCons(int excCons) {
        this.excCons = excCons;
    }

    public void setInhCons(int inhCons) {
        this.inhCons = inhCons;
    }


    public SelectionStyle getSelectMethod() {
        return selectMethod;
    }

    public void setSelectMethod(SelectionStyle selectMethod) {
        this.selectMethod = selectMethod;
    }

    @Override
    public String getName() {
        return "Radial (Simple)";
    }

    @Override
    public String toString() {
        return getName();
    }
}