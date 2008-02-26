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
package org.simbrain.world.odorworld;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.world.dataworld.DataModel;
import org.simbrain.world.dataworld.DataWorldComponent;
import org.simbrain.world.gameworld2d.GameWorld2DComponent;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * <b>WorldPanel</b> is the container for the world component.   Handles toolbar buttons, and serializing of world
 * data.  The main environment codes is in {@link OdorWorldPanel}.
 */
public class OdorWorldComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    /** Reference to model world. */
    private OdorWorld world = new OdorWorld(this);
    
    /**
     * Default constructor.
     */
    public OdorWorldComponent(final String name) {
        super(name);
    }
    
    @SuppressWarnings("unchecked")
    private OdorWorldComponent(final String name, final OdorWorld world) {
        super(name);
        this.world = world;
        world.setParent(this);
    }


    /**
     * Recreates an instance of this class from a saved component.
     * 
     * @param input
     * @param name
     * @param format
     * @return
     */
    public static OdorWorldComponent open(InputStream input, String name, String format) {
        OdorWorld newWorld = (OdorWorld) OdorWorld.getXStream().fromXML(input);
        return new OdorWorldComponent(name, newWorld);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        OdorWorld.getXStream().toXML(world, output);
    }
    
    OdorWorld getWorld() {
        return world;
    }
    

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }
    
    @Override
    public List<? extends Consumer> getConsumers() {
        return world.getConsumers();
    }
    
    @Override
    public List<? extends Producer> getProducers() {
        return world.getProducers();
    }

    @Override
    protected void update() {
        /* no implementation */
    }
}
