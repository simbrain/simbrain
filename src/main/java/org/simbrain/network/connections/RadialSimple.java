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
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;

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
 * scripts. // TODO: Add full repetoire of probabilities EE,EI,IE,II...
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
public class RadialSimple extends ConnectionStrategy implements EditableObject {

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
            minimumValue = 0, maximumValue = 1, increment = .1, order = 5)
    private double excitatoryProbability = .8;

    /**
     * Probability of designating a given synapse excitatory. If not, it's
     * inhibitory.
     */
    @UserParameter(label = "Inh. Probability", description = "Probability connections will be made to neighbor inhibitory neurons ",
        minimumValue = 0, maximumValue = 1, increment = .1, order = 6)
    private double inhibitoryProbability = .8;

    /**
     * The number of connections allowed with excitatory (or non-polar) neurons. If there
     * are sufficient excitatory (or non-polar) neurons in a given neuron's neighborhood this
     * will be how many connections are made.
     */
    @UserParameter(label = "Num. Exc. Connections", description = "Maximum # of connections with exc. neurons",
            minimumValue = 0, order = 7)
    private int excCons = 5;

    /**
     * Radius within which to connect excitatory excNeurons.
     */
    @UserParameter(label = "Exc. Radius", description = "Distance to search for excitatory neurons to connect to",
            minimumValue = 0, order = 3)
    private double excitatoryRadius = 100;

    /**
     * Template synapse for inhibitory synapses.
     */
    private Synapse baseInhibitorySynapse = Synapse.getTemplateSynapse();

    /**
     * Radius within which to connect inhibitory excNeurons.
     */
    @UserParameter(label = "Inh. Radius", description = "Distance to search for inhibitory neurons to connect to",
            minimumValue = 0, order = 4)
    private double inhibitoryRadius = 80;

    /**
     * The number of connections allowed with inhibitory neurons. If there
     * are sufficient inhibitory neurons in a given neuron's neighborhood this
     * will be how many connections are made.
     */
    @UserParameter(label = "Num. Inh. Connections", description = "Maximum # of connections with inh. neurons",
            minimumValue = 0, order = 8)
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

    /**
     * Default constructor.
     */
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
        nonPolarNeurons = excNeurons.stream().filter(neuron -> neuron.getPolarity()
                == SimbrainConstants.Polarity.BOTH).collect(Collectors.toList());
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
            if (Network.getLooseSynapse(neuron, otherNeu) != null) {
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
                    if (selectMethod == SelectionStyle.IN) {
                        synapse = new Synapse(otherNeu, neuron);
                        synapse.setStrength(Math.random());
                    } else {
                        synapse = new Synapse(neuron, otherNeu);
                    }

                    if (looseSynapses) {
                        network.addLooseSynapse(synapse);
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
                    network.addLooseSynapse(synapse);
                } else {
                    if (syns != null)
                        syns.add(synapse);
                }
                degreeCounter++;
                if(degreeCounter >= inhCons) {
//                    network.fireSynapsesUpdated(); // TODO: [event]
                    break;
                }
            }
            if (network != null) {
//                network.fireSynapsesUpdated(); // TODO: [event]
            }
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
            if (Network.getLooseSynapse(neuron, otherNeu) != null) {
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
                        network.addLooseSynapse(synapse);
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
                    network.addLooseSynapse(synapse);
                } else {
                    if (syns != null)
                        syns.add(synapse);
                }
                // degreeCounter++;
                // if(degreeCounter >= excCons) {
                //     network.fireSynapsesUpdated();
                //     break;
                // }
            }
            // if (network != null) {
            //     network.fireSynapsesUpdated();
            // }
        }
    }

    @Override
    public void connectNeurons(SynapseGroup synGroup) {
        // No implementation yet.
        List<Neuron> target = synGroup.getTargetNeurons();
        List<Neuron> source = synGroup.getSourceNeurons();
        List<Synapse> syns = new ArrayList<>();

        if(selectMethod == SelectionStyle.IN) {
            for(Neuron tar : target) {
                makeConnects(tar, source, syns);
            }
        } else {
            for(Neuron src : source) {
                makeConnects(src, target, syns);
            }
        }
        for(Synapse s : syns) {
            synGroup.addNewSynapse(s);
        }
    }

    /**
     * Makes connections between a neuron and some other neurons and returns a synapse list.
     * Accounts for connection and selection styles of various kinds and polarity.
     * @param neu
     * @param others
     * @param retList
     * @return
     */
    public List<Synapse> makeConnects(Neuron neu, List<Neuron> others, List<Synapse> retList) {

        others = getNeuronsInRadius(neu, others, getExcitatoryRadius());
        if(others.isEmpty()) {
            return retList;
        }

        if(conMethod == ConnectStyle.PROBABILISTIC) {
            double p = neu.getPolarity() != Polarity.INHIBITORY ?
                    excitatoryProbability : inhibitoryProbability;
            return connectProb(neu, others, retList, selectMethod, p);
        } else {
            int noCons = neu.getPolarity() != Polarity.INHIBITORY ?
                    excCons : inhCons;
            return connectDet(neu, others, retList, selectMethod, noCons);
        }

    }

    /**
     * Connects a neuron to a list of possible neurons to connect to probabilistically. Synapses
     * are assigned weight values based on source polarity.
     * @param n The neuron of interest. If selection style is IN, it is the neuron the others send
     *          connections to. If selection style is OUT, it is the neuron sending connections to the others.
     * @param others
     * @param retList
     * @param selectionStyle
     * @param p
     * @return
     */
    private static List<Synapse> connectProb(Neuron n, List<Neuron> others,
                                     List<Synapse> retList,
                                     SelectionStyle selectionStyle, double p) {
        for(Neuron o : others) {
            if(Math.random() < p) {
                if(selectionStyle == SelectionStyle.IN) {
                    retList.add(new Synapse(o, n, o.getPolarity().value(Math.random())));
                } else {
                    retList.add(new Synapse(n, o, n.getPolarity().value(Math.random())));
                }
            }
        }
        return retList;
    }

    /**
     * Connects a neuron to a list of possible neurons to connect to "deterministically".
     * An exact number of neurons to connect with are chosen randomly, but the number itself
     * is guaranteed. Synapses are assigned weight values based on source polarity.
     * @param n The neuron of interest. If selection style is IN, it is the neuron the others send
     *          connections to. If selection style is OUT, it is the neuron sending connections to the others.
     * @param others
     * @param retList
     * @param selectionStyle
     * @param N
     * @return
     */
    private static List<Synapse> connectDet(Neuron n, List<Neuron> others,
                                     List<Synapse> retList,
                                     SelectionStyle selectionStyle, int N) {
        if(N > others.size()) {
            N = others.size();
        } else {
            Collections.shuffle(others);
        }
        for(int ii=0; ii<N; ++ii) {
            Neuron o = others.get(ii);
            if (selectionStyle == SelectionStyle.IN) {
                retList.add(new Synapse(o, n, o.getPolarity().value(Math.random())));
            } else {
                retList.add(new Synapse(n, o, n.getPolarity().value(Math.random())));
            }
        }
        return retList;
    }


    @Override
    public List<Synapse> connectNeurons(Network network, List<Neuron> source, List<Neuron> target) {
        this.network = network;
        List<Synapse> createdSyns = new ArrayList<>();
        excNeurons = target.stream().filter(neuron -> neuron.getPolarity()
                == SimbrainConstants.Polarity.EXCITATORY).collect(Collectors.toList());
        inhNeurons = target.stream().filter(neuron -> neuron.getPolarity()
                == SimbrainConstants.Polarity.INHIBITORY).collect(Collectors.toList());
        nonPolarNeurons = target.stream().filter(neuron -> neuron.getPolarity()
                == SimbrainConstants.Polarity.BOTH).collect(Collectors.toList());

        for (Neuron src : source) {
            makeExcitatory(src, createdSyns, true);
            makeInhibitory(src, createdSyns, true);
        }
        return createdSyns;
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
            if (SimnetUtils.getEuclideanDist(source, neuron) < radius) {
                ret.add(neuron);
            }
        }
        return ret;
    }

    public void setConMethod(ConnectStyle conMethod) {
        this.conMethod = conMethod;
    }

    public ConnectStyle getConMethod() {
        return conMethod;
    }

    public boolean isAllowSelfConnections() {
        return allowSelfConnections;
    }

    public void setAllowSelfConnections(final boolean allowSelfConnections) {
        this.allowSelfConnections = allowSelfConnections;
    }

    public double getExcitatoryProbability() {
        return excitatoryProbability;
    }

    public void setExcitatoryProbability(final double excitatoryProbability) {
        this.excitatoryProbability = excitatoryProbability;
    }

    public double getExcitatoryRadius() {
        return excitatoryRadius;
    }

    public void setExcitatoryRadius(final double excitatoryRadius) {
        this.excitatoryRadius = excitatoryRadius;
    }

    public double getInhibitoryRadius() {
        return inhibitoryRadius;
    }

    public void setInhibitoryRadius(final double inhibitoryRadius) {
        this.inhibitoryRadius = inhibitoryRadius;
    }

    public double getInhibitoryProbability() {
        return inhibitoryProbability;
    }

    public void setInhibitoryProbability(final double inhibitoryProbability) {
        this.inhibitoryProbability = inhibitoryProbability;
    }

    public Synapse getBaseExcitatorySynapse() {
        return baseExcitatorySynapse;
    }

    public void setBaseExcitatorySynapse(Synapse baseExcitatorySynapse) {
        this.baseExcitatorySynapse = baseExcitatorySynapse;
    }

    public Synapse getBaseInhibitorySynapse() {
        return baseInhibitorySynapse;
    }

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