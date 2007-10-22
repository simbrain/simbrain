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

import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * <b>WorldPanel</b> is the container for the world component.   Handles toolbar buttons, and serializing of world
 * data.  The main environment codes is in {@link OdorWorldPanel}.
 */
public class OdorWorldComponent extends WorkspaceComponent<WorkspaceComponentListener> {

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
    public void update() {
        // TODO Auto-generated method stub
        
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
