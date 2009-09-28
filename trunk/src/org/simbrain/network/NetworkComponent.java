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

import org.simbrain.network.attributes.NeuronWrapper;
import org.simbrain.network.attributes.SynapseWrapper;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.listeners.NeuronListener;
import org.simbrain.network.listeners.SynapseListener;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
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
        addNeuronWrappers();
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

    /**
     * Set upper bound attribute to all neuron wrappers.
     */
    public void setUpperBoundAttributes(boolean upperBoundSelected) {
        NeuronWrapper.setUseUpperBoundAttribute(upperBoundSelected); 
        if (upperBoundSelected) {
            for (Consumer consumer : getConsumers()) {
                if (consumer instanceof NeuronWrapper) {
                    ((NeuronWrapper)consumer).addUpperBoundAttribute();
                    removeConsumer(consumer);
                    addConsumer(consumer);
                }
            }
            for (Producer producer: getProducers()) {
                if (producer instanceof NeuronWrapper) {
                    ((NeuronWrapper)producer).addUpperBoundAttribute();
                    removeProducer(producer);
                    addProducer(producer);
                }
            }
        } else {
            for (Consumer consumer : getConsumers()) {
                if (consumer instanceof NeuronWrapper) {
                    ((NeuronWrapper)consumer).removeUpperBoundAttribute();
                    removeConsumer(consumer);
                    addConsumer(consumer);
                }
            }
            for (Producer producer: getProducers()) {
                if (producer instanceof NeuronWrapper) {
                    ((NeuronWrapper)producer).removeUpperBoundAttribute();
                    removeProducer(producer);
                    addProducer(producer);
                }
            }
        }

    }

    /**
     * Set lower bound attribute to all neuron wrappers.
     */
    public void setLowerBoundAttributes(boolean lowerBoundSelected) {
        NeuronWrapper.setUseLowerBoundAttribute(lowerBoundSelected); 
        if (lowerBoundSelected) {
            for (Consumer consumer : getConsumers()) {
                if (consumer instanceof NeuronWrapper) {
                    ((NeuronWrapper)consumer).addLowerBoundAttribute();
                    removeConsumer(consumer);
                    addConsumer(consumer);
                }
            }
            for (Producer producer: getProducers()) {
                if (producer instanceof NeuronWrapper) {
                    ((NeuronWrapper)producer).addLowerBoundAttribute();
                    removeProducer(producer);
                    addProducer(producer);
                }
            }
        } else {
            for (Consumer consumer : getConsumers()) {
                if (consumer instanceof NeuronWrapper) {
                    ((NeuronWrapper)consumer).removeLowerBoundAttribute();
                    removeConsumer(consumer);
                    addConsumer(consumer);
                }
            }
            for (Producer producer: getProducers()) {
                if (producer instanceof NeuronWrapper) {
                    ((NeuronWrapper)producer).removeLowerBoundAttribute();
                    removeProducer(producer);
                    addProducer(producer);
                }
            }
        }
    }

    /**
     * Set target value attributes to all neuron wrappers.
     */
    public void setTargetValueAttributes(boolean targetValueSelected) {
        NeuronWrapper.setUseTargetValueAttribute(targetValueSelected); 
        if(targetValueSelected) {
            for (Consumer consumer : getConsumers()) {
                if (consumer instanceof NeuronWrapper) {
                    ((NeuronWrapper)consumer).addTargetValueAttribute();
                    removeConsumer(consumer);
                    addConsumer(consumer);
                }
            }
            for (Producer producer: getProducers()) {
                if (producer instanceof NeuronWrapper) {
                    ((NeuronWrapper)producer).addTargetValueAttribute();
                    removeProducer(producer);
                    addProducer(producer);
                }
            }
        } else {
            for (Consumer consumer : getConsumers()) {
                if (consumer instanceof NeuronWrapper) {
                    ((NeuronWrapper)consumer).removeTargetValueAttribute();
                    removeConsumer(consumer);
                    addConsumer(consumer);
                }
            }
            for (Producer producer: getProducers()) {
                if (producer instanceof NeuronWrapper) {
                    ((NeuronWrapper)producer).removeTargetValueAttribute();
                    removeProducer(producer);
                    addProducer(producer);
                }
            }
        }
    }

    /**
     * The synapse listener.
     */
    SynapseListener synapseListener = new SynapseListener() {
        /**
         * {@inheritDoc}
         */
        public void synapseAdded(NetworkEvent<Synapse> e) {
            SynapseWrapper wrapper = new SynapseWrapper(e.getObject(), NetworkComponent.this);
            addConsumer(wrapper);
            addProducer(wrapper);
        }

        /**
         * {@inheritDoc}
         */
        public void synapseTypeChanged(NetworkEvent<Synapse> e) {
            SynapseWrapper wrapper = (SynapseWrapper) getConsumer(e.getOldObject().getId());
            wrapper.setSynapse(e.getObject());
        }

        /**
         * {@inheritDoc}
         */
        public void synapseRemoved(NetworkEvent<Synapse> e) {
            for (Consumer consumer : getConsumers()) {
                if (consumer instanceof SynapseWrapper) {
                    if (((SynapseWrapper)consumer).getSynapse() == e.getObject()) {
                        removeConsumer(consumer);
                        break;
                    }
                }
            }
            for (Producer producer : getProducers()) {
                if (producer instanceof SynapseWrapper) {
                    if (((SynapseWrapper)producer).getSynapse() == e.getObject()) {
                        removeProducer(producer);
                    }
                }
            }
        }

        public void synapseChanged(NetworkEvent<Synapse> e) {
            // No implementation
        }
    };

    /**
     * Add synapse wrappers, as well as a listener so new synapse wrappers are
     * automatically created.
     *
     * @param useSynapseWrappers whether to use synapse attributes
     */
    public void setUsingSynapseWrappers(boolean useSynapseWrappers) {
        SynapseWrapper.setUsingSynapseAttributes(useSynapseWrappers);
        if (useSynapseWrappers) {
            // Add attributes for existing synapses
            for (Synapse synapse : rootNetwork.getFlatSynapseList()) {
                SynapseWrapper wrapper = new SynapseWrapper(synapse, this);
                addConsumer(wrapper);
                addProducer(wrapper);
            }
            // Add the synapse listener
            rootNetwork.addSynapseListener(synapseListener);
        } else {
            // Remove attributes for existing synapses
            this.removeConsumers(SynapseWrapper.class);
            this.removeProducers(SynapseWrapper.class);
            // Remove the synapse listener
            rootNetwork.removeSynapseListener(synapseListener);
        }
    }

    public ConsumingAttribute findConsumingActivationAttribute(Neuron neuron) {
        for (Consumer consumer : getConsumers()) {
            if (consumer instanceof NeuronWrapper) {
                if (((NeuronWrapper)consumer).getNeuron() == neuron) {
                    return consumer.getConsumingAttributes().get(0);
                }
            }
        }
        return null;
    }

    //TODO: Use this below in listener.  Apply template to odor world listenres?
    
    public ProducingAttribute findProducingActivationAttribute(Neuron neuron) {
        for (Producer producers : getProducers()) {
            if (producers instanceof NeuronWrapper) {
                if (((NeuronWrapper)producers).getNeuron() == neuron) {
                    return producers.getProducingAttributes().get(0);
                }
            }
        }
        return null;
    }

    /**
     * Initialize getConsumers(), getProducers(), and listener.
     */
    private void addNeuronWrappers() {

        for (Neuron neuron : rootNetwork.getFlatNeuronList()) {
            NeuronWrapper wrapper = new NeuronWrapper(neuron, this);
            addConsumer(wrapper);
            addProducer(wrapper);
        }

        // Add the neuron listener
        rootNetwork.addNeuronListener(new NeuronListener() {

            /**
             * {@inheritDoc}
             */
            public void neuronAdded(NetworkEvent<Neuron> e) {
                NeuronWrapper wrapper = new NeuronWrapper(e.getObject(), NetworkComponent.this);
                addConsumer(wrapper);
                addProducer(wrapper);
            }

            /**
             * {@inheritDoc}
             */
            public void neuronTypeChanged(NetworkEvent<Neuron> e) {
                NeuronWrapper wrapper = (NeuronWrapper) getConsumer(e.getOldObject().getId());
                wrapper.setNeuron(e.getObject());
            }

            /**
             * {@inheritDoc}
             */
            public void neuronMoved(NetworkEvent<Neuron> e) {
                // TODO Auto-generated method stub
            }

            /**
             * {@inheritDoc}
             */
            public void neuronRemoved(NetworkEvent<Neuron> e) {
                for (Consumer consumer : getConsumers()) {
                    if (consumer instanceof NeuronWrapper) {
                        if (((NeuronWrapper)consumer).getNeuron() == e.getObject()) {
                            removeConsumer(consumer);
                            break;
                        }
                    }
                }
                for (Producer producer : getProducers()) {
                    if (producer instanceof NeuronWrapper) {
                        if (((NeuronWrapper)producer).getNeuron() == e.getObject()) {
                            removeProducer(producer);
                        }
                    }
                }
            }
            public void neuronChanged(NetworkEvent<Neuron> e) {
                // TODO Auto-generated method stub
            }
        });


    }
}
