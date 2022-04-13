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
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.Utils;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.WorkspaceComponent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.simbrain.network.core.NetworkUtilsKt.getNetworkXStream;

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

        event.onModelAdded(m -> {
            setChangedSinceLastSave(true);
            if (m instanceof AttributeContainer) {
                fireAttributeContainerAdded((AttributeContainer) m);
            }
            if (m instanceof NeuronGroup) {
                ((NeuronGroup)m).getNeuronList().forEach(this::fireAttributeContainerAdded);
            }
        });

        event.onModelRemoved(m -> {
            setChangedSinceLastSave(true);
            if (m instanceof AttributeContainer) {
                fireAttributeContainerRemoved((AttributeContainer) m);
            }
            if (m instanceof NeuronGroup) {
                ((NeuronGroup)m).getNeuronList().forEach(this::fireAttributeContainerRemoved);
            }
        });

//        event.onNeuronsUpdated(l -> setChangedSinceLastSave(true));
//
//        event.onTextAdded(t -> setChangedSinceLastSave(true));
//
//        event.onTextRemoved(t -> setChangedSinceLastSave(true));


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
        return network.getAllModels().stream()
                .filter(m -> (m instanceof AttributeContainer))
                .map(m -> (AttributeContainer) m )
                .collect(Collectors.toList());
    }

    public static NetworkComponent open(final InputStream input, final String name, final String format) {
        Network newNetwork = (Network) getNetworkXStream().fromXML(input);
        return new NetworkComponent(name, newNetwork);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        getNetworkXStream().toXML(network, output);
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
    public String getXML() {
        return Utils.getSimbrainXStream().toXML(network);
    }

    @Override
    public void start() {
        // Stop any local network running that is occurring
        network.setRunning(false);
    }
}
