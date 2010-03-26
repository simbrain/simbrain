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

import org.simbrain.network.interfaces.*;
import org.simbrain.workspace.AttributeID;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
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
    }

    /**
     * Create a new network component.
     */
    public NetworkComponent(final String name, final RootNetwork network) {
        super(name);
        this.rootNetwork = network;
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


    @Override
    public List<AttributeID> getPotentialConsumers() {

        List<AttributeID> returnList = new ArrayList<AttributeID>();
        for (Neuron neuron : rootNetwork.getFlatNeuronList()) {
            //TODO: Add check for visibility
            AttributeID consumerID = new AttributeID(this, neuron.getId());
            consumerID.setSubtype("activation");
            returnList.add(consumerID);
        }

        return returnList;
    }

    @Override
    public List<AttributeID> getPotentialProducers() {

        List<AttributeID> returnList = new ArrayList<AttributeID>();
        for (Neuron neuron : rootNetwork.getFlatNeuronList()) {
            //TODO: Add check for visibility
            AttributeID producerID = new AttributeID(this, neuron.getId());
            producerID.setSubtype("activation");
            returnList.add(producerID);
        }

        return returnList;
    }

    @Override
    public Consumer createConsumer(final AttributeID id) {

        final Neuron neuron = rootNetwork.getNeuron(id.getID());

        if (neuron != null) {
            if (id.getSubtype().equalsIgnoreCase("activation")) {
                return new Consumer() {

                    public void setValue(Object value) {
                        neuron.setInputValue(((Double) value).doubleValue());
                    }

                    public String getDescription() {
                        return id.getDescription();
                    }

                    public WorkspaceComponent getParentComponent() {
                        return NetworkComponent.this;
                    }

                };
            }
        }

        return null;
    }

    @Override
    public Producer createProducer(final AttributeID id) {

        final Neuron neuron = rootNetwork.getNeuron(id.getID());

        if (neuron != null) {
            if (id.getSubtype().equalsIgnoreCase("activation")) {
                return new Producer() {

                    public String getDescription() {
                        return id.getDescription();
                    }

                    public WorkspaceComponent getParentComponent() {
                        return NetworkComponent.this;
                    }

                    public Object getValue() {
                        return neuron.getActivation();
                    }

                };
            }
        }

        return null;
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
