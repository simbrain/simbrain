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

import java.io.File;
import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

/**
 * <b>WorldPanel</b> is the container for the world component.   Handles toolbar buttons, and serializing of world
 * data.  The main environment codes is in {@link OdorWorldPanel}.
 */
public class OdorWorldComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    private OdorWorld world = new OdorWorld();
    
    /**
     * Default constructor.
     */
    public OdorWorldComponent(String name) {
        super(name);
    }

    /**
     * Read a world.
     *
     * @param theFile the wld file containing world information
     */
    public void open(final File theFile) {
        // TODO implement
    }

    OdorWorld getWorld() {
        return world;
    }
    
    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
//    private static XStream getXStream() {
//        XStream xstream = new XStream(new DomDriver());
//        xstream.setMode(XStream.ID_REFERENCES);
//        xstream.omitField(OdorWorldEntity.class, "theImage");
//        xstream.omitField(OdorWorldAgent.class, "effectorList");
//        xstream.omitField(OdorWorldAgent.class, "sensorList");
//        xstream.omitField(OdorWorld.class, "couplings");
//        return xstream;
//    }

    /**
     * Save a specified file  Called by "save".
     *
     * @param theFile the file to save to
     */
    public void save(final File theFile) {
     // TODO implement
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    @Override
    public String getFileExtension() {
        return "wld";
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

//    @Override
//    public void setCurrentDirectory(final String currentDirectory) {        
//        super.setCurrentDirectory(currentDirectory);
//        OdorWorldPreferences.setCurrentDirectory(currentDirectory);
//    }
//
//    @Override
//    public String getCurrentDirectory() {
//        return OdorWorldPreferences.getCurrentDirectory();
//    }

    /**
     * {@inheritDoc}
     */
//    public CouplingContainer getCouplingContainer() {
//        return worldPanel.getWorld();
//    }
}
