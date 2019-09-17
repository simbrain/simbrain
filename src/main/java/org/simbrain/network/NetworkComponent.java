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

import org.simbrain.network.core.*;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronArrayNode;
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

        network.addPropertyChangeListener(
            evt -> {
                if ("neuronAdded".equals(evt.getPropertyName())) {
                     setChangedSinceLastSave(true);
                     fireAttributeContainerAdded((Neuron) evt.getNewValue());
                } else if ("neuronRemoved".equals(evt.getPropertyName())) {
                    setChangedSinceLastSave(true);
                    fireAttributeContainerRemoved((Neuron) evt.getNewValue());
                } else if ("neuronsUpdated".equals(evt.getPropertyName())) {
                    setChangedSinceLastSave(true);
                } else if ("synapseAdded".equals(evt.getPropertyName())) {
                    setChangedSinceLastSave(true);
                    fireAttributeContainerAdded((Synapse) evt.getNewValue());
                } else if ("synapseRemoved".equals(evt.getPropertyName())) {
                    setChangedSinceLastSave(true);
                    fireAttributeContainerRemoved((Synapse) evt.getNewValue());
                } else if ("textAdded".equals(evt.getPropertyName())) {
                    setChangedSinceLastSave(true);
                } else if ("textRemoved".equals(evt.getPropertyName())) {
                    setChangedSinceLastSave(true);
                } else if ("groupAdded".equals(evt.getPropertyName())) {
                    setChangedSinceLastSave(true);
                    fireAttributeContainerAdded((Group) evt.getNewValue());
                } else if ("groupRemoved".equals(evt.getPropertyName())) {
                    setChangedSinceLastSave(true);
                    fireAttributeContainerRemoved((Group) evt.getOldValue());
                } else if ("ncAdded".equals(evt.getPropertyName())) {
                    setChangedSinceLastSave(true);
                    fireAttributeContainerAdded((NeuronCollection) evt.getNewValue());
                } else if ("ncRemoved".equals(evt.getPropertyName())) {
                    setChangedSinceLastSave(true);
                    fireAttributeContainerRemoved((NeuronCollection) evt.getOldValue());
                } else if ("naRemoved".equals(evt.getPropertyName())) {
                    setChangedSinceLastSave(true);
                    fireAttributeContainerRemoved((NeuronArray) evt.getOldValue());
                } else if ("neuronArrayAdded".equals(evt.getPropertyName())) {
                    setChangedSinceLastSave(true);
                    fireAttributeContainerAdded((NeuronArray) evt.getNewValue());
                }
            }
        );

    }

    @Override
    public AttributeContainer getObjectFromKey(String objectKey) {
        if (objectKey.startsWith("Neuron_")) {
            return this.getNetwork().getLooseNeuron(objectKey);
        } else if (objectKey.startsWith("Synapse_")) {
            return this.getNetwork().getLooseSynapse(objectKey);
        } else if (objectKey.startsWith("Group_")) {
            return this.getNetwork().getGroup(objectKey);
        } else if (objectKey.startsWith("Group_")) {
            return this.getNetwork().getNeuronCollection(objectKey);
        }
        return null;
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> retList = new ArrayList<>();
        //retList.add(network);
        retList.addAll(network.getFlatNeuronList());
        retList.addAll(network.getSynapseList());
        retList.addAll(network.getNeuronGroups());
        retList.addAll(network.getSynapseGroups());
        retList.addAll(network.getNeuronCollectionSet());
        retList.addAll(network.getNaList());
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
