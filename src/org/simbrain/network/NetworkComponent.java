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
import org.simbrain.network.interfaces.NeuronUpdateRule;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.interfaces.SynapseUpdateRule;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.listeners.NeuronListener;
import org.simbrain.network.listeners.SynapseListener;
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
     * Initialize attribute types and listeners.
     */
    private void init() {

        // Set root networks' id to the component's name
        rootNetwork.setId(super.getName());

        // Initialize attribute types and their default visibility
        addProducerType(new AttributeType(this, "Neuron", "getActivation", double.class, true));
        addProducerType(new AttributeType(this, "Neuron", "getUpperBound", double.class, false));
        addProducerType(new AttributeType(this, "Neuron", "getLowerBound", double.class, false));
        addProducerType(new AttributeType(this, "Neuron", "getLabel", String.class, false));
        addProducerType(new AttributeType(this, "Synapse", "getStrength", double.class, false));

        addConsumerType(new AttributeType(this, "Neuron", "setInputValue", double.class, true));
        addConsumerType(new AttributeType(this, "Neuron", "setActivation", double.class, false));
        addConsumerType(new AttributeType(this, "Neuron", "setUpperBound", double.class, false));
        addConsumerType(new AttributeType(this, "Neuron", "setLowerBound", double.class, false));
        addConsumerType(new AttributeType(this, "Neuron", "setLabel", String.class, false));
        addConsumerType(new AttributeType(this, "Synapse", "setStrength", double.class, false));

        rootNetwork.addNeuronListener(new NeuronListener() {
            /**
             * {@inheritDoc}
             */
            public void neuronAdded(NetworkEvent<Neuron> e) {
                setChangedSinceLastSave(true);
                firePotentialAttributesChanged();
            }

            /**
             * {@inheritDoc}
             */
            public void neuronTypeChanged(NetworkEvent<NeuronUpdateRule> e) {
                setChangedSinceLastSave(true);
            }

            /**
             * {@inheritDoc}
             */
            public void neuronMoved(NetworkEvent<Neuron> e) {
                setChangedSinceLastSave(true);
            }

            /**
             * {@inheritDoc}
             */
            public void neuronRemoved(NetworkEvent<Neuron> e) {
                setChangedSinceLastSave(true);
                firePotentialAttributesChanged();
                fireAttributeObjectRemoved(e.getObject());
            }

            public void neuronChanged(NetworkEvent<Neuron> e) {
                setChangedSinceLastSave(true);
           }
        });

        rootNetwork.addSynapseListener(new SynapseListener() {

            public void synapseAdded(NetworkEvent<Synapse> networkEvent) {
                setChangedSinceLastSave(true);
                firePotentialAttributesChanged();
            }

            public void synapseChanged(NetworkEvent<Synapse> networkEvent) {
                setChangedSinceLastSave(true);
                firePotentialAttributesChanged();
            }

            public void synapseRemoved(NetworkEvent<Synapse> networkEvent) {
                setChangedSinceLastSave(true);
                firePotentialAttributesChanged();
            }

            public void synapseTypeChanged(NetworkEvent<SynapseUpdateRule> networkEvent) {
                setChangedSinceLastSave(true);
            }

        });

    }

    @Override
    public List<PotentialConsumer> getPotentialConsumers() {
        List<PotentialConsumer> returnList = new ArrayList<PotentialConsumer>();
        for (AttributeType type : getVisibleConsumerTypes()) {
            if (type.getTypeName().equalsIgnoreCase("Neuron")) {
                for (Neuron neuron : rootNetwork.getFlatNeuronList()) {
                    String description = type.getDescription(neuron.getId());
                    returnList.add(getAttributeManager().createPotentialConsumer(neuron, type, description));
                }
            } else if (type.getTypeName().equalsIgnoreCase("Synapse")) {
                for (Synapse synapse : rootNetwork.getFlatSynapseList()) {
                    String description = type.getDescription(synapse.getId());
                    returnList.add(getAttributeManager().createPotentialConsumer(synapse, type, description));
                }
            }

        }
        return returnList;
    }

    @Override
    public List<PotentialProducer> getPotentialProducers() {
        List<PotentialProducer> returnList = new ArrayList<PotentialProducer>();
        for (AttributeType type : getVisibleProducerTypes()) {
            if (type.getTypeName().equalsIgnoreCase("Neuron")) {
                for (Neuron neuron : rootNetwork.getFlatNeuronList()) {
                    String description = type.getDescription(neuron.getId());
                    returnList.add(getAttributeManager().createPotentialProducer(neuron, type, description));
                }
            } else if (type.getTypeName().equalsIgnoreCase("Synapse")) {
                for (Synapse synapse : rootNetwork.getFlatSynapseList()) {
                    String description = type.getDescription(synapse.getId());
                    returnList.add(getAttributeManager().createPotentialProducer(synapse, type, description));
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
