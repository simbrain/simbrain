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
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.listeners.NeuronListener;
import org.simbrain.network.listeners.SynapseListener;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
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

    /** Initialize attribute types and listeners. */
    private void init() {
        network.addNeuronListener(new NeuronListener() {
            public void neuronAdded(NetworkEvent<Neuron> e) {
                setChangedSinceLastSave(true);
                fireModelAdded(e);
            }

            public void neuronTypeChanged(NetworkEvent<NeuronUpdateRule> e) {
                setChangedSinceLastSave(true);
                fireModelChanged(e);
            }

            public void neuronMoved(NetworkEvent<Neuron> e) {
                setChangedSinceLastSave(true);
            }

            public void neuronRemoved(NetworkEvent<Neuron> e) {
                setChangedSinceLastSave(true);
                fireModelRemoved(e);
            }

            public void neuronChanged(NetworkEvent<Neuron> e) {
                setChangedSinceLastSave(true);
                fireModelChanged(e);
            }

            public void labelChanged(NetworkEvent<Neuron> e) {
                setChangedSinceLastSave(true);
            }
        });

        network.addSynapseListener(new SynapseListener() {
            public void synapseAdded(NetworkEvent<Synapse> networkEvent) {
                setChangedSinceLastSave(true);
                fireModelAdded(networkEvent.getObject());
            }

            public void synapseChanged(NetworkEvent<Synapse> networkEvent) {
                setChangedSinceLastSave(true);
                fireModelChanged(networkEvent.getObject());
            }

            public void synapseRemoved(NetworkEvent<Synapse> networkEvent) {
                setChangedSinceLastSave(true);
                fireModelRemoved(networkEvent.getObject());
            }

            public void synapseTypeChanged(NetworkEvent<SynapseUpdateRule> networkEvent) {
                setChangedSinceLastSave(true);
                fireModelChanged(networkEvent.getObject());
            }
        });
    }

    @Override
    public Object getObjectFromKey(String objectKey) {
        if (objectKey.startsWith("Neuron_")) {
            return this.getNetwork().getNeuron(objectKey);
        } else if (objectKey.startsWith("Synapse_")) {
            return this.getNetwork().getSynapse(objectKey);
        } else if (objectKey.startsWith("Group_")) {
            return this.getNetwork().getGroup(objectKey);
        }
        return null;
    }

    @Override
    public List<Object> getModels() {
        List<Object> retList = new ArrayList<Object>();
        retList.add(network);
        retList.addAll(network.getNeuronList());
        retList.addAll(network.getSynapseList());
        retList.addAll(network.getNeuronGroups());
        retList.addAll(network.getSynapseGroups());
        return retList;
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
     * Returns a copy of this NetworkComponent.
     *
     * @return the new network component
     */
    public NetworkComponent copy() {
        NetworkComponent ret = new NetworkComponent(
                "Copy of " + network.getName(), network.copy());
        return ret;
    }

    /**
     * Sets whether or not this component is marked as currently running... 
     * meant to be false if only doing a one-off update
     * @param running
     */
    @Override
    public void setRunning(boolean running) {
    	super.setRunning(running);
    	network.setOneOffRun(!running);
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
    public void closing() {}

    @Override
    public String getXML() {
        return Network.getXStream().toXML(network);
    }
}
