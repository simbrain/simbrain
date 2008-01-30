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
package org.simbrain.world.textworld;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * <b>TextWorldComponent</b> is the container for the world component.   Handles toolbar buttons, and serializing of world
 * data.  The main environment codes is in {@link TextWorld}.
 */
public class TextWorldComponent extends WorkspaceComponent<WorkspaceComponentListener> {
    /**
     * Creates a new frame of type TextWorld.
     * @param ws Workspace to add frame to
     */
    public TextWorldComponent(String name) {
        super(name);
    }

    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        // omit fields
        return xstream;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public TextWorldComponent open(final InputStream input) {
        return (TextWorldComponent) getXStream().fromXML(input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output) {
        getXStream().toXML(output);
    }
    
    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    public List<Consumer> getConsumers() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Producer> getProducers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void update() {
        // TODO Auto-generated method stub
    }
}
