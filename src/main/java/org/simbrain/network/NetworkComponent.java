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

import org.simbrain.network.core.Network;
import org.simbrain.network.events.NetworkEvents;
import org.simbrain.util.Utils;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.WorkspaceComponent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Network component.
 */
public final class NetworkComponent extends WorkspaceComponent {

    /**
     * Reference to root network, the main model network.
     */
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
     * @param name    name of network
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

        NetworkEvents event = network.getEvents();

        event.onNeuronAdded(n -> {
            setChangedSinceLastSave(true);
            fireAttributeContainerAdded(n);
        });

        event.onNeuronRemoved(n -> {
            setChangedSinceLastSave(true);
            fireAttributeContainerRemoved(n);
        });

//        event.onNeuronsUpdated(l -> setChangedSinceLastSave(true));

        event.onSynapseAdded(s -> {
            setChangedSinceLastSave(true);
            fireAttributeContainerAdded(s);
        });

        event.onSynapseRemoved(s -> {
            setChangedSinceLastSave(true);
            fireAttributeContainerRemoved(s);
        });
//
//        event.onTextAdded(t -> setChangedSinceLastSave(true));
//
//        event.onTextRemoved(t -> setChangedSinceLastSave(true));

        event.onNeuronGroupAdded(ng -> {
            setChangedSinceLastSave(true);
            fireAttributeContainerAdded(ng);
            ng.getNeuronList().forEach(this::fireAttributeContainerAdded);
        });

        event.onNeuronGroupRemoved(ng -> {
            setChangedSinceLastSave(true);
            ng.getNeuronList().forEach(this::fireAttributeContainerRemoved);
            fireAttributeContainerRemoved(ng);
        });

        event.onNeuronCollectionAdded(nc -> {
            setChangedSinceLastSave(true);
            fireAttributeContainerAdded(nc);
        });

        event.onNeuronCollectionRemoved(nc -> {
            setChangedSinceLastSave(true);
            fireAttributeContainerRemoved(nc);
        });

        event.onNeuronArrayAdded(na -> {
            setChangedSinceLastSave(true);
            fireAttributeContainerAdded(na);
        });

        event.onNeuronArrayRemoved(na -> {
            setChangedSinceLastSave(true);
            fireAttributeContainerRemoved(na);
        });

    }

    // TODO: Implement new stuff
    @Override
    public AttributeContainer getAttributeContainer(String objectKey) {
        if (objectKey.startsWith("Neuron_")) {
            return this.getNetwork().getLooseNeuron(objectKey);
        } else if (objectKey.startsWith("Synapse_")) {
            return this.getNetwork().getLooseSynapse(objectKey);
        }
        return null;
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> retList = new ArrayList<>();
        //retList.add(network);
        retList.addAll(network.getFlatNeuronList());
        retList.addAll(network.getLooseSynapses());
        //retList.addAll(network.getFlatGroupList()); // TODO
        retList.addAll(network.getNeuronCollectionSet());
        retList.addAll(network.getNaList());
        // TODO: Temp code to handle xstream backwards compatibility issues
        if (network.getWeightMatrices() != null) {
            retList.addAll(network.getWeightMatrices());
        }
        return retList;
    }

    public static NetworkComponent open(final InputStream input, final String name, final String format) {
        Network newNetwork = (Network) Utils.getSimbrainXStream().fromXML(input);
        return new NetworkComponent(name, newNetwork);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        network.preSaveInit();
        Utils.getSimbrainXStream().toXML(network, output);
        network.postSaveReInit();
    }

    /**
     * Returns a copy of this NetworkComponent.
     *
     * @return the new network component
     */
    public NetworkComponent copy() {
        NetworkComponent ret = new NetworkComponent("Copy of " + network.getName(), network.copy());
        return ret;
    }

    /**
     * Sets whether or not this component is marked as currently running...
     * meant to be false if only doing a one-off update
     *
     * @param running
     */
    @Override
    public void setRunning(boolean running) {
        super.setRunning(running);
        network.setOneOffRun(!running);
    }

    public Network getNetwork() {
        return network;
    }

    @Override
    public void update() {
        network.update();
    }

    @Override
    public void closing() {
    }

    @Override
    public String getXML() {
        return Utils.getSimbrainXStream().toXML(network);
    }
}
