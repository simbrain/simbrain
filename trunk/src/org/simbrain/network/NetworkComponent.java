/*
h * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
import org.simbrain.network.listeners.SynapseListener;
import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.PotentialAttribute;
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
     * Iniitalize attribute types and listeners.
     */
    private void init() {

        // Initialize attribute types and their default visibility
        addProducerType(new AttributeType(this, "Neuron", "Activation", double.class, true));
        addProducerType(new AttributeType(this, "Neuron", "UpperBound", double.class, false));
        addProducerType(new AttributeType(this, "Neuron", "LowerBound", double.class, false));
        addProducerType(new AttributeType(this, "Neuron", "Label", String.class, false));
        addProducerType(new AttributeType(this, "Synapse", "Strength", double.class, false));
        
        addConsumerType(new AttributeType(this, "Neuron", "InputValue", double.class, true));
        addConsumerType(new AttributeType(this, "Neuron", "Activation", double.class, false));
        addConsumerType(new AttributeType(this, "Neuron", "UpperBound", double.class, false));
        addConsumerType(new AttributeType(this, "Neuron", "LowerBound", double.class, false));
        addConsumerType(new AttributeType(this, "Neuron", "Label", String.class, false));
        addConsumerType(new AttributeType(this, "Synapse", "Strength", double.class, false));

        rootNetwork.addNeuronListener(new NeuronListener() {
            /**
             * {@inheritDoc}
             */
            public void neuronAdded(NetworkEvent<Neuron> e) {
                firePotentialAttributesChanged();
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
                firePotentialAttributesChanged();
                fireAttributeObjectRemoved(e.getObject());
            }

            public void neuronChanged(NetworkEvent<Neuron> e) {
            }
        });

        rootNetwork.addSynapseListener(new SynapseListener() {

            public void synapseAdded(NetworkEvent<Synapse> networkEvent) {
                firePotentialAttributesChanged();
            }

            public void synapseChanged(NetworkEvent<Synapse> networkEvent) {
                firePotentialAttributesChanged();
            }

            public void synapseRemoved(NetworkEvent<Synapse> networkEvent) {
                firePotentialAttributesChanged();
            }

            public void synapseTypeChanged(NetworkEvent<Synapse> networkEvent) {
            }

        });

    }

    @Override
    public List<PotentialConsumer> getPotentialConsumers() {
        List<PotentialConsumer> returnList = new ArrayList<PotentialConsumer>();
        for (AttributeType type : getConsumerTypes()) {
            if (type.isVisible()) {
                if (type.getTypeID().equalsIgnoreCase("Neuron")) {
                    for (Neuron neuron : rootNetwork.getFlatNeuronList()) {
                        returnList.add(new PotentialConsumer(neuron, neuron.getId(), type));
                    }
                } else if (type.getTypeID().equalsIgnoreCase("Synapse")) {
                    for (Synapse synapse : rootNetwork.getFlatSynapseList()) {
                        returnList.add(new PotentialConsumer(synapse, synapse.getId(), type));
                    }
                }

            }
        }
        return returnList;
    }

    @Override
    public List<PotentialProducer> getPotentialProducers() {
        List<PotentialProducer> returnList = new ArrayList<PotentialProducer>();
        for (AttributeType type : getVisibleProducerTypes()) {
            if (type.getTypeID().equalsIgnoreCase("Neuron")) {
                for (Neuron neuron : rootNetwork.getFlatNeuronList()) {
                    returnList.add(new PotentialProducer(neuron, neuron.getId(), type));
                }
            } else if (type.getTypeID().equalsIgnoreCase("Synapse")) {
                for (Synapse synapse : rootNetwork.getFlatSynapseList()) {
                    returnList.add(new PotentialProducer(synapse, synapse.getId(), type));
                }
            }
        }
        return returnList;
    }

    @Override
    public Object getObjectFromKey(String objectKey) {
        if (objectKey.startsWith("Neuron")) {
            return this.getRootNetwork().getNeuron(objectKey);
        } else if (objectKey.startsWith("Synapse")) {
            return this.getRootNetwork().getSynapse(objectKey);
        }
        return null;
    }

    @Override
    public String getKeyFromObject(Object object) {
        if (object instanceof Neuron) {
            return ((Neuron)object).getId();
        } else if (object instanceof Synapse) {
            return ((Synapse)object).getId();
        }
        return null;
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
