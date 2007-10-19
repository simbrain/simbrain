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
package org.simbrain.workspace;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.Logger;

/**
 * Represents a window in the Simbrain desktop.   Services relating to
 * couplings and relations between are handled.  We may want to abstract
 * out some of the coupling management since much of this is focused on
 * the GUI aspects of the JInternalFrames.
 */
public abstract class WorkspaceComponent {

    private static final Logger LOGGER = Logger.getLogger(WorkspaceComponent.class);
    
    /** Log4j logger. */
    private Logger logger = Logger.getLogger(WorkspaceComponent.class);

    /** Whether this component has changed since last save. */
    private boolean changedSinceLastSave = false;

    /** The name of this component.  Used in the title, in saving, etc. */
    private String name  = "";

    /**
     * Construct a workspace component.
     */
    public WorkspaceComponent(String name) {
        this.name = name;
        logger.trace(this.getClass().getCanonicalName() + " created");
    }

    /**
     * If any initialization is needed after adding this component to workspace.
     *
     */
    public void postAddInit() {
    }

    /**
     * Used when saving a workspace.  All changed workspace components are saved using
     * this method.
     *
     * @param saveFile the file to save.
     */
    public abstract void save(File saveFile);

    /**
     * When workspaces are opened, a path to a file is passed in.
     * So, all components which can be saved should have this.
     *
     * @param openFile file representing saved component.
     */
    public abstract void open(File openFile);


    /**
     * The file extension for a component type, e.g. ".net".
     *
     * @return the file extension
     */
    public abstract String getFileExtension();

    /**
     * Perform cleanup after closing.
    */
    public abstract void close();

    /**
     * Update that goes beyond updating couplings.
     * Called when global workspace update is called.
     */
    public abstract void update();

    /**
     * Save vs. save-as.  Saves the currentfile.
     */
    public void save() {
        //TODO
//        save(currentFile);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

//    public void accceptProducers(final ArrayList<Producer> list) {
//
//    }

    /**
     * @param changedSinceLastSave the changedSinceLastSave to set
     */
    public void setChangedSinceLastSave(final boolean changedSinceLastSave) {
        LOGGER.debug("component changed");
        this.changedSinceLastSave = changedSinceLastSave;
    }

    /**
     * @return the changedSinceLastSave
     */
    public boolean isChangedSinceLastSave() {
        return changedSinceLastSave;
    }

    /**
     * Retrieves a simple version of a component name from its class,
     * e.g. "Network" from "org.simbrain.network.NetworkComponent"/
     *
     * @return the simple name.
     */
    public String getSimpleName() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Component")) {
            simpleName = simpleName.replaceFirst("Component", "");
        }
        return simpleName;
    }
    
    public Collection<? extends Consumer> getConsumers()
    {
        return Collections.emptySet();
    }
    
    public Collection<? extends Producer> getProducers()
    {
        return Collections.emptySet();
    }
}
