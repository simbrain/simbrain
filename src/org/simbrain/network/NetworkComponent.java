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

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
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
    private Network network = new Network();

    /**
     * Create a new network component.
     *
     * @param name name
     */
    public NetworkComponent(final String name) {
        super(name);
        init();
    }

    /**
     * Create a new network component.
     *
     * @param name name of network
     * @param network the network being created
     */
    public NetworkComponent(final String name, final Network network) {
        super(name);
        this.network = network;
        init();
    }

    /**
     * Initialize attribute types and listeners.
     */
    private void init() {

        // Initialize attribute types and their default visibility
        addProducerType(new AttributeType(this, "Neuron Activation", "getActivation",
                double.class, true));
        addProducerType(new AttributeType(this, "Neuron Upper Bound", "getUpperBound",
                double.class, false));
        addProducerType(new AttributeType(this, "Neuron Lower Bound", "getLowerBound",
                double.class, false));
        addProducerType(new AttributeType(this, "Neuron Label", "getLabel",
                String.class, false));
        addProducerType(new AttributeType(this, "Synapse", "getStrength",
                double.class, false));
        addProducerType(new AttributeType(this, "NeuronGroupActivations",
                "getExternalActivations", double[].class, true));
        addProducerType(new AttributeType(this, "NeuronGroupSpikes",
                "getSpikeIndexes", double[].class, true));
        addProducerType(new AttributeType(this, "SynapseGroup",
                "getWeightVector", double[].class, true));

        addConsumerType(new AttributeType(this, "Neuron Input Value", "setInputValue",
                double.class, true));
        addConsumerType(new AttributeType(this, "Neuron Activation", "setActivation",
                double.class, false));
        addConsumerType(new AttributeType(this, "Neuron Upper Bound", "setUpperBound",
                double.class, false));
        addConsumerType(new AttributeType(this, "Neuron Lower Bound", "setLowerBound",
                double.class, false));
        addConsumerType(new AttributeType(this, "Neuron Label", "setLabel",
                String.class, false));
        addConsumerType(new AttributeType(this, "Synapse", "setStrength",
                double.class, false));
        addConsumerType(new AttributeType(this, "NeuronGroup",
                "setInputValues", double[].class, true));
        addConsumerType(new AttributeType(this, "SynapseGroup",
                "setWeightVector", double[].class, false));

        network.addNeuronListener(new NeuronListener() {
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

            @Override
            public void neuronChanged(NetworkEvent<Neuron> e) {
                setChangedSinceLastSave(true);
            }

            @Override
            public void labelChanged(NetworkEvent<Neuron> e) {
                setChangedSinceLastSave(true);
            }
        });

        network.addSynapseListener(new SynapseListener() {

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

            public void synapseTypeChanged(
                    NetworkEvent<SynapseUpdateRule> networkEvent) {
                setChangedSinceLastSave(true);
            }

        });

    }

    /**
     * Helper method for making neuron consumers, since it happens in a few
     * different places and is important to be consistent about.
     *
     * @param component network component
     * @param neuron the neuron that will consume activations
     * @param methodName the name of the method called by this consumer
     * @return the neuron consumer
     */
    public static PotentialConsumer getNeuronConsumer(
            NetworkComponent component, Neuron neuron, String methodName) {
        PotentialConsumer consumer = component.getAttributeManager()
                .createPotentialConsumer(neuron, methodName, double.class);
        consumer.setCustomDescription(neuron.getId() + ":" + methodName);
        return consumer;
    }

    /**
     * Helper method for making synapse consumers, since it happens in a few
     * different places and is important to be consistent about.
     *
     * @param component network component
     * @param synapse the synapse that will "consume" strengths
     * @param methodName the name of the method called by this consumer
     * @return the synapse consumer
     */
    public static PotentialConsumer getSynapseConsumer(
            NetworkComponent component, Synapse synapse, String methodName) {
        PotentialConsumer consumer = component.getAttributeManager()
                .createPotentialConsumer(synapse, methodName, double.class);
        consumer.setCustomDescription(synapse.getId() + ":" + methodName);
        return consumer;
    }

    /**
     * Helper method for making neuron group vector consumers, since it happens
     * in a few different places and is important to be consistent about.
     *
     * @param component network component
     * @param group the group that will "consume" activations
     * @param methodName the name of the method called by this consumer
     * @return the neuron group consumer
     */
    public static PotentialConsumer getNeuronGroupConsumer(
            NetworkComponent component, NeuronGroup group, String methodName) {
        PotentialConsumer consumer = component.getAttributeManager()
                .createPotentialConsumer(group, methodName, double[].class);
        consumer.setCustomDescription(group.getId() + ":" + methodName);
        return consumer;
    }

    @Override
    public List<PotentialConsumer> getPotentialConsumers() {
        List<PotentialConsumer> returnList = new ArrayList<PotentialConsumer>();
        for (AttributeType type : getVisibleConsumerTypes()) {
            if (type.getTypeName().startsWith("Neuron ")) {
                if (type.getTypeName().equalsIgnoreCase("Neuron Input Value")) {
                    for (Neuron neuron : network.getFlatNeuronList()) {
                        returnList.add(getNeuronConsumer(this, neuron,
                                type.getMethodName()));
                    }
                } else {
                    for (Neuron neuron : network.getFlatNeuronList()) {
                        String description = type.getDescription(neuron.getId());
                        PotentialConsumer consumer = getAttributeManager()
                                .createPotentialConsumer(neuron, type); 
                        consumer.setCustomDescription(description);
                        returnList.add(consumer);
                    }
                }
            } else if (type.getTypeName().equalsIgnoreCase("Synapse")) {
                for (Synapse synapse : network.getFlatSynapseList()) {
                    String description = type.getDescription(synapse.getId());
                    PotentialConsumer consumer = getAttributeManager()
                            .createPotentialConsumer(synapse, type);
                    consumer.setCustomDescription(description);
                    returnList.add(consumer);
                }
            } else if (type.getTypeName().equalsIgnoreCase("NeuronGroup")) {
                // Handle NeuronGroup attributes
                for (Group group : network.getFlatGroupList()) {
                    if (group instanceof NeuronGroup) {
                        PotentialConsumer consumer = getNeuronGroupConsumer(
                                this, (NeuronGroup) group, type.getMethodName());
                        returnList.add(consumer);

                    }
                }
            } else if (type.getTypeName().equalsIgnoreCase("SynapseGroup")) {
                // Handle SynapseGroup attributes
                for (Group group : network.getFlatGroupList()) {
                    if (group instanceof SynapseGroup) {
                        PotentialConsumer consumer = getAttributeManager()
                                .createPotentialConsumer(group,
                                        "setWeightVector", double[].class);
                        consumer.setCustomDescription("Synapse Group: "
                                + group.getLabel());
                        returnList.add(consumer);
                    }
                }
            }

        }
        return returnList;
    }

    /**
     * Helper method for making neuron producers, since it happens in a few
     * different places and is important to be consistent about.
     *
     * @param component network component
     * @param neuron the neuron that will produce activations
     * @param methodName the name of the method called by this producer
     * @return the neuron producer
     */
    public static PotentialProducer getNeuronProducer(
            NetworkComponent component, Neuron neuron, String methodName) {
        PotentialProducer producer = component.getAttributeManager()
                .createPotentialProducer(neuron, methodName, double.class);
        producer.setCustomDescription(neuron.getId() + ":" + methodName);
        return producer;
    }

    /**
     * Helper method for making neuron group producers, since it happens in a
     * few different places and is important to be consistent about.
     *
     * @param component network component
     * @param group the neuron group that will produce activations
     * @param methodName the name of the method called by this producer
     * @return the neuron group producer
     */
    public static PotentialProducer getNeuronGroupProducer(
            NetworkComponent component, NeuronGroup group, String methodName) {
        PotentialProducer producer = component.getAttributeManager()
                .createPotentialProducer(group, methodName, double[].class);
        producer.setCustomDescription(group.getId() + ":" + methodName);
        return producer;
    }

    /**
     * Helper method for making synapse producers, since it happens in a few
     * different places and is important to be consistent about.
     *
     * @param component network component
     * @param synapse the synapse that will "produce" strengths
     * @param methodName the name of the method called by this producer
     * @return the synapse producer
     */
    public static PotentialProducer getSynapseProducer(
            NetworkComponent component, Synapse synapse, String methodName) {
        PotentialProducer producer = component.getAttributeManager()
                .createPotentialProducer(synapse, methodName, double.class);
        producer.setCustomDescription(synapse.getId() + ":" + methodName);
        return producer;
    }

    @Override
    public List<PotentialProducer> getPotentialProducers() {
        List<PotentialProducer> returnList = new ArrayList<PotentialProducer>();
        for (AttributeType type : getVisibleProducerTypes()) {
            if (type.getTypeName().startsWith("Neuron ")) {
                if (type.getTypeName().equalsIgnoreCase("Neuron Activation")) {
                    for (Neuron neuron : network.getFlatNeuronList()) {
                        returnList.add(getNeuronProducer(this, neuron,
                                type.getMethodName()));
                    }
                } else {
                    for (Neuron neuron : network.getFlatNeuronList()) {
                        String description = type.getDescription(neuron.getId());
                        PotentialProducer producer = getAttributeManager()
                                .createPotentialProducer(neuron, type); 
                        producer.setCustomDescription(description);
                        returnList.add(producer);
                    }
                }
            } else if (type.getTypeName().equalsIgnoreCase("Synapse")) {
                for (Synapse synapse : network.getFlatSynapseList()) {
                    String description = type.getDescription(synapse.getId());
                    PotentialProducer producer = getAttributeManager()
                            .createPotentialProducer(synapse, type);
                    producer.setCustomDescription(description);
                    returnList.add(producer);
                }
            } else if (type.getTypeName().equalsIgnoreCase(
                    "NeuronGroupActivations")) {
                for (Group group : network.getFlatGroupList()) {
                    if (group instanceof NeuronGroup) {
                        returnList.add(getNeuronGroupProducer(this,
                                (NeuronGroup) group, type.getMethodName()));
                    }
                }
            } else if (type.getTypeName().equalsIgnoreCase("NeuronGroupSpikes")) {
                for (Group group : network.getFlatGroupList()) {
                    if (group instanceof NeuronGroup) {
                        returnList.add(getNeuronGroupProducer(this,
                                (NeuronGroup) group, type.getMethodName()));
                    }
                }
            } else if (type.getTypeName().equalsIgnoreCase("SynapseGroup")) {
                // Handle SynapseGroup attributes
                for (Group group : network.getFlatGroupList()) {
                    if (group instanceof SynapseGroup) {
                        PotentialProducer producer = getAttributeManager()
                                .createPotentialProducer(group,
                                        "getWeightVector", double[].class);
                        producer.setCustomDescription("Synapse Group: "
                                + group.getLabel());
                        returnList.add(producer);
                    }
                }
            }
        }

        return returnList;
    }

    @Override
    public Object getObjectFromKey(String objectKey) {
        if (objectKey.startsWith("Neuron_")) {
            return this.getNetwork().getNeuron(objectKey);
        } else if (objectKey.startsWith("Synapse_")) {
            return this.getNetwork().getSynapse(objectKey);
        } else if (objectKey.startsWith("NeuronGroup")) {
            return this.getNetwork().getGroup(objectKey.split(":")[1]);
        } else if (objectKey.startsWith("SynapseGroup")) {
            return this.getNetwork().getGroup(objectKey.split(":")[1]);
        }
        return null;
    }

    @Override
    public String getKeyFromObject(Object object) {
        if (object instanceof Neuron) {
            return ((Neuron) object).getId();
        } else if (object instanceof Synapse) {
            return ((Synapse) object).getId();
        } else if (object instanceof NeuronGroup) {
            return "NeuronGroup:" + ((NeuronGroup) object).getId();
        } else if (object instanceof SynapseGroup) {
            return "SynapseGroup:" + ((SynapseGroup) object).getId();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public static NetworkComponent open(final InputStream input,
            final String name, final String format) {
        Network newNetwork = (Network) Network.getXStream().fromXML(input);
        return new NetworkComponent(name, newNetwork);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        network.preSaveInit();
        Network.getXStream().toXML(network, output);
        network.postSaveReInit();
    }

    /**
     * Returns the root network.
     *
     * @return the root network
     */
    public Network getNetwork() {
        return network;
    }

    @Override
    public void update() {
        network.update();
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }

    @Override
    public String getXML() {
        return Network.getXStream().toXML(network);
    }

}
