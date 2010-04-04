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
package org.simbrain.network;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.listeners.NeuronListener;
import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Network component.
 */
public final class NetworkComponent extends WorkspaceComponent {

    /** Reference to root network, the main model network. */
    private RootNetwork rootNetwork = new RootNetwork();

    /**
     * Create a new network component.
     */
    public NetworkComponent(final String name) {
        super(name);
        init();
    }

    /**
     * Create a new network component.
     */
    public NetworkComponent(final String name, final RootNetwork network) {
        super(name);
        this.rootNetwork = network;
        init();
    }

    /**
     * By default, neuronwrappers are all that is added.
     */
    private void init() {

        getAttributeTypes().add(new AttributeType("Neuron", "activation", true, double.class));
        getAttributeTypes().add(new AttributeType("Neuron", "upperBound", false, double.class));
        getAttributeTypes().add(new AttributeType("Neuron", "lowerBound", false, double.class));
        getAttributeTypes().add(new AttributeType("Neuron", "label", false, String.class));
        getAttributeTypes().add(new AttributeType("Synapse", "strength", false, double.class));

        rootNetwork.addNeuronListener(new NeuronListener() {
            /**
             * {@inheritDoc}
             */
            public void neuronAdded(NetworkEvent<Neuron> e) {
                NetworkComponent.this.firePotentialAttributeUpdateEvent(NetworkComponent.this);
            }

            /**
             * {@inheritDoc}
             */
            public void neuronTypeChanged(NetworkEvent<Neuron> e) {
            }

            /**
             * {@inheritDoc}
             */
            public void neuronMoved(NetworkEvent<Neuron> e) {
            }

            /**
             * {@inheritDoc}
             */
            public void neuronRemoved(NetworkEvent<Neuron> e) {
                NetworkComponent.this.firePotentialAttributeUpdateEvent(NetworkComponent.this);
            }

            public void neuronChanged(NetworkEvent<Neuron> e) {
            }
        });
    }

    /**
     * {@inheritDoc}
     */
     public static NetworkComponent open(final InputStream input, final String name, final String format) {
        RootNetwork newNetwork = (RootNetwork) RootNetwork.getXStream().fromXML(input);
        return new NetworkComponent(name, newNetwork);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        RootNetwork.getXStream().toXML(rootNetwork, output);
    }

    /**
     * Returns the root network.
     *
     * @return the root network
     */
    public RootNetwork getRootNetwork() {
        return rootNetwork;
    }

    @Override
    public void update() {
        rootNetwork.update();
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }

    @Override
    public String getXML() {
        return RootNetwork.getXStream().toXML(rootNetwork);
    }


    @Override
    public List<PotentialConsumer<?>> getPotentialConsumers() {

        List<PotentialConsumer<?>> returnList = new ArrayList<PotentialConsumer<?>>();

        for (AttributeType type : this.getAttributeTypes()) {
            if (type.isVisible()) {
                if (type.getTypeID().equalsIgnoreCase("Neuron")) {
                    if (type.getSubtype().equalsIgnoreCase("activation")) {
                        for (Neuron neuron : rootNetwork.getFlatNeuronList()) {
                            PotentialConsumer<Double> potentialConsumer = new PotentialConsumer<Double>(
                                    type, this, neuron, "setInputValue");
                            returnList.add(potentialConsumer);
                        }
                    } else if (type.getSubtype().equalsIgnoreCase("upperBound")) {
                        for (Neuron neuron : rootNetwork.getFlatNeuronList()) {
                            PotentialConsumer<Double> potentialConsumer = new PotentialConsumer<Double>(
                                    type, this, neuron, "setUpperBound");
                            returnList.add(potentialConsumer);
                        }
                    } else if (type.getSubtype().equalsIgnoreCase("lowerBound")) {
                        for (Neuron neuron : rootNetwork.getFlatNeuronList()) {
                            PotentialConsumer<Double> potentialConsumer = new PotentialConsumer<Double>(
                                    type, this, neuron, "setLowerBound");
                            returnList.add(potentialConsumer);
                        }
                    } else if (type.getSubtype().equalsIgnoreCase("label")) {
                        for (Neuron neuron : rootNetwork.getFlatNeuronList()) {
                            PotentialConsumer<String> potentialConsumer = new PotentialConsumer<String>(
                                    type, this, neuron, "setLabel");
                            returnList.add(potentialConsumer);
                        }
                    }
                } else if (type.getTypeID().equalsIgnoreCase("Synapse")) {
                    if (type.getSubtype().equalsIgnoreCase("strength")) {
                        for (Synapse synapse : rootNetwork.getFlatSynapseList()) {
                            PotentialConsumer<Double> potentialConsumer = new PotentialConsumer<Double>(
                                    type, this, synapse, "setStrength");
                            returnList.add(potentialConsumer);
                        }

                    }

                }

            }
        }

        return returnList;
    }

    @Override
    public List<PotentialProducer<?>> getPotentialProducers() {

        List<PotentialProducer<?>> returnList = new ArrayList<PotentialProducer<?>>();
        for (AttributeType type : this.getAttributeTypes()) {
            if (type.isVisible() == true) {
                if(type.getTypeID().equalsIgnoreCase("Neuron")) {
                    if(type.getSubtype().equalsIgnoreCase("activation")) {
                        for (Neuron neuron : rootNetwork.getFlatNeuronList()) {
                            PotentialProducer<Double> potentialProducer = new PotentialProducer<Double>(
                                   type, this, neuron, "getActivation");
                            returnList.add(potentialProducer);
                        }
                    }
                    if(type.getSubtype().equalsIgnoreCase("upperBound")) {
                        for (Neuron neuron : rootNetwork.getFlatNeuronList()) {
                            PotentialProducer<Double> potentialProducer = new PotentialProducer<Double>(
                                   type, this, neuron, "getUpperBound");
                            returnList.add(potentialProducer);
                        }
                    }
                    if(type.getSubtype().equalsIgnoreCase("lowerBound")) {
                        for (Neuron neuron : rootNetwork.getFlatNeuronList()) {
                            PotentialProducer<Double> potentialProducer = new PotentialProducer<Double>(
                                   type, this, neuron, "getLowerBound");
                            returnList.add(potentialProducer);
                        }
                    }
                    if(type.getSubtype().equalsIgnoreCase("label")) {
                        for (Neuron neuron : rootNetwork.getFlatNeuronList()) {
                            PotentialProducer<String> potentialProducer = new PotentialProducer<String>(
                                   type, this, neuron, "getLabel");
                            returnList.add(potentialProducer);
                        }
                    }
                }
            }

        }
        return returnList;
    }

    // TODO: Link to NetworkSettings.
//    @Override
//    public void setCurrentDirectory(final String currentDirectory) {
//        super.setCurrentDirectory(currentDirectory);
////        NetworkPreferences.setCurrentDirectory(currentDirectory);
//    }
//
//    @Override
//    public String getCurrentDirectory() {
////       return NetworkPreferences.getCurrentDirectory();
//        return null;
//    }


}
