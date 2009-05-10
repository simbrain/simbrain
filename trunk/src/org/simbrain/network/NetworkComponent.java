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
package org.simbrain.network;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.simbrain.network.attributes.NeuronWrapper;
import org.simbrain.network.interfaces.Group;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.NetworkEvent;
import org.simbrain.network.interfaces.NetworkListener;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.workspace.Attribute;
import org.simbrain.workspace.AttributeHolder;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.dataworld.ConsumingColumn;
import org.simbrain.world.dataworld.ProducingColumn;

/**
 * Network frame.
 */
public final class NetworkComponent extends WorkspaceComponent<NetworkListener> {

    /** Reference to root network, the main model network. */
    private RootNetwork rootNetwork = new RootNetwork(this);
    
    /** Consumer list. */
    private List<Consumer> consumers = new CopyOnWriteArrayList<Consumer>();

    /** Producer list. */
    private List<Producer> producers = new CopyOnWriteArrayList<Producer>();
    
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
        rootNetwork.setParent(this);
        setChangedSinceLastSave(false);
        init();
    }
    

    
    public static NetworkComponent open(final InputStream input, final String name, final String format) {
        RootNetwork newNetwork = (RootNetwork) RootNetwork.getXStream().fromXML(input);
        return new NetworkComponent(name, newNetwork);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        RootNetwork.getXStream().toXML(rootNetwork, output);
    }
    
    public RootNetwork getRootNetwork() {
        return rootNetwork;
    }

    @Override
    public void update() {
        rootNetwork.updateRootNetwork();
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }
    
    @Override
    public Collection<? extends Consumer> getConsumers() {
        return consumers;
    }
    
    @Override
    public Collection<? extends Producer> getProducers() {
        return producers;
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
     * Returns the listeners on this component.
     * 
     * @return The listeners on this component.
     */
    public Collection<? extends NetworkListener> getListeners() {
        return super.getListeners();
    }
    
    /**
     * Initialize consumers, producers, and listener.
     */
    private void init() {

        for(Neuron neuron : rootNetwork.getFlatNeuronList()) {
            NeuronWrapper wrapper = new NeuronWrapper(neuron, this);
            consumers.add(wrapper);
            producers.add(wrapper);
        }
        
        this.addListener(new NetworkListener() {

            public void clampBarChanged() {
                // TODO Auto-generated method stub
                
            }

            public void clampMenuChanged() {
                // TODO Auto-generated method stub
                
            }

            public void groupAdded(NetworkEvent<Group> event) {
                // TODO Auto-generated method stub
                
            }

            public void groupChanged(NetworkEvent<Group> event) {
                // TODO Auto-generated method stub
                
            }

            public void groupRemoved(NetworkEvent<Group> event) {
                // TODO Auto-generated method stub
                
            }

            public void networkChanged() {
                // TODO Auto-generated method stub
                
            }

            public void neuronAdded(NetworkEvent<Neuron> e) {
                NeuronWrapper wrapper = new NeuronWrapper(e.getObject(), NetworkComponent.this);
                consumers.add(wrapper);
                producers.add(wrapper);
            }

            public void neuronChanged(NetworkEvent<Neuron> e) {
                // TODO Auto-generated method stub
                
            }

            public void neuronMoved(NetworkEvent<Neuron> e) {
                // TODO Auto-generated method stub
                
            }

            public void neuronRemoved(NetworkEvent<Neuron> e) {
                for (Consumer consumer : consumers) {
                    if (consumer instanceof NeuronWrapper) {
                        if (((NeuronWrapper)consumer).getNeuron() == e.getObject()) {
                            consumers.remove(consumer);
                            break;
                        }
                    }
                }
                for (Producer producer : producers) {
                    if (producer instanceof NeuronWrapper) {
                        if (((NeuronWrapper)producer).getNeuron() == e.getObject()) {
                            producers.remove(producer);
                        }
                    }
                }
            }

            public void subnetAdded(NetworkEvent<Network> e) {
                // TODO Auto-generated method stub
                
            }

            public void subnetRemoved(NetworkEvent<Network> e) {
                // TODO Auto-generated method stub
                
            }

            public void synapseAdded(NetworkEvent<Synapse> e) {
                // TODO Auto-generated method stub
                
            }

            public void synapseChanged(NetworkEvent<Synapse> e) {
                // TODO Auto-generated method stub
                
            }

            public void synapseRemoved(NetworkEvent<Synapse> e) {
                // TODO Auto-generated method stub
                
            }

            public void attributeRemoved(AttributeHolder parent,
                    Attribute attribute) {
                // TODO Auto-generated method stub
                
            }

            public void componentUpdated() {
                // TODO Auto-generated method stub
                
            }

            public void setTitle(String name) {
                // TODO Auto-generated method stub
                
            }
            
        });
    }

    
    
}
