/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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

import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.simbrain.network.gui.NetworkPreferences;
import org.simbrain.network.interfaces.NetworkListener;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Network frame.
 */
public final class NetworkComponent extends WorkspaceComponent<NetworkListener> {

    private RootNetwork rootNetwork = new RootNetwork(this);
    
    /**
     * Create a new network component.
     */
    public NetworkComponent(String name) {
        super(name);
    }
    
    /**
     * Create a new network component.
     */
    public NetworkComponent(String name, RootNetwork network) {
        super(name);
        this.rootNetwork = network;
        rootNetwork.setParent(this);
        setChangedSinceLastSave(false);
    }

    
    public static NetworkComponent open(InputStream input, final String name, final String format) {
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
        return rootNetwork.getConsumers();
    }
    
    @Override
    public Collection<? extends Producer> getProducers() {
        return rootNetwork.getProducers();
    }
    
    @Override
    public String getXML() {
        return RootNetwork.getXStream().toXML(rootNetwork);
    }

    @Override
    public void setCurrentDirectory(final String currentDirectory) {
        super.setCurrentDirectory(currentDirectory);
        NetworkPreferences.setCurrentDirectory(currentDirectory);
    }

    @Override
    public String getCurrentDirectory() {
       return NetworkPreferences.getCurrentDirectory();
    }
    
    /**
     * Returns the listeners on this component.
     * 
     * @return The listeners on this component.
     */
    public Collection<? extends NetworkListener> getListeners() {
        return super.getListeners();
    }
}
