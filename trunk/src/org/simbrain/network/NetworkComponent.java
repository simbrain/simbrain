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
import org.simbrain.network.interfaces.Group;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.NetworkEvent;
import org.simbrain.network.interfaces.NetworkListener;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Network frame.
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
     * Initialize getConsumers(), getProducers(), and listener.
     */
    private void init() {

        for(Neuron neuron : rootNetwork.getFlatNeuronList()) {
            NeuronWrapper wrapper = new NeuronWrapper(neuron, this);
            addConsumer(wrapper);
            addProducer(wrapper);
        }
        
        rootNetwork.addListener(new NetworkListener() {

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
                addConsumer(wrapper);
                addProducer(wrapper);
            }

            public void neuronChanged(NetworkEvent<Neuron> e) {
                // TODO Auto-generated method stub
                
            }

            public void neuronMoved(NetworkEvent<Neuron> e) {
                // TODO Auto-generated method stub
                
            }

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
        });
    }

    
    
}
