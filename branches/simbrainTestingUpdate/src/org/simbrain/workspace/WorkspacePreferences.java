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

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.simbrain.network.NetworkComponent;
import org.simbrain.world.dataworld.DataWorldComponent;
import org.simbrain.world.odorworld.OdorWorldComponent;

/**
 * <b>WorkspacePreferences</b> handles storage and retrieval of user
 * preferences, e.g. current directory. TODO: May need to be re-implemented for
 * compatibility with applet jars.
 */
public final class WorkspacePreferences {

    /** File system property. */
    private static final String FS = System.getProperty("file.separator");

    /** The main user preference object. */
    private static final Preferences THE_PREFS = Preferences.userRoot().node(
            "org/simbrain/workspace");

    /**
     * This class should not be instantiated.
     */
    private WorkspacePreferences() {
        /* no implementation */
    }

    /**
     * Save all user preferences.
     */
    public static void saveAll() {
        try {
            THE_PREFS.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * Restores user preferences to default values.
     */
    public static void restoreDefaults() {
        setCurrentDirectory(getDefaultCurrentDirectory());
        setDefaultFile(getDefaultDefaultFile());
    }

    // ////////////////////////////////////////////////////////////////
    // Getters and setters for user preferences //
    // Note that default values for preferences are stored in the //
    // second argument of the getter method //
    // ////////////////////////////////////////////////////////////////
    /**
     * Sets the current workspace directory.
     *
     * @param dir Directory to set
     */
    public static void setCurrentDirectory(final String dir) {
        THE_PREFS.put("currentDirectory", dir);
    }

    /**
     * Return the current workspace directory.
     *
     * @return return the current directory
     */
    public static String getCurrentDirectory() {
        return THE_PREFS.get("currentDirectory", getDefaultCurrentDirectory());
    }

    /**
     * Return the default current workspace directory.
     *
     * @return default current directory
     */
    public static String getDefaultCurrentDirectory() {
        return "." + FS + "simulations" + FS + "workspaces";
    }

    /**
     * Sets the default workspace file.
     *
     * @param file Default file to set
     */
    public static void setDefaultFile(final String file) {
        THE_PREFS.put("defaultFile", file);
    }

    /**
     * Return the default file.
     *
     * @return the default file
     */
    public static String getDefaultFile() {
        return THE_PREFS.get("defaultFile", getDefaultDefaultFile());
    }

    /**
     * Return the default default file.
     *
     * @return the default default file
     */
    public static String getDefaultDefaultFile() {
        return "." + FS + "workspace.zip";
    }

    /**
     * Returns the default directory for specific component types.
     *
     * @param componentType the component type
     * @return the directory
     */
    private static String getDefaultDirectory(
            Class<? extends WorkspaceComponent> componentType) {

        if (componentType == OdorWorldComponent.class) {
            return "." + FS + "simulations" + FS + "worlds";
        } else if (componentType == DataWorldComponent.class) {
            return "." + FS + "simulations" + FS + "tables";
        } else if (componentType == NetworkComponent.class) {
            return "." + FS + "simulations" + FS + "networks";
        } else {
            return "." + FS + "simulations";
        }
    }

    /**
     * Set the current directory (for saving and opening files) for a given
     * component type (network, data world, etc).
     *
     * @param componentType the WorkspaceComponent's class
     * @param directoryPath the directory, as a string
     */
    public static void setCurrentDirectory(
            Class<? extends WorkspaceComponent> componentType,
            String directoryPath) {
        THE_PREFS.put(componentType.getSimpleName(), directoryPath);

    }

    /**
     * Get the current directory (for saving and opening files) for a given
     * component type (network, data world, etc).
     *
     * @param componentType the WorkspaceComponent's class
     * @return the current directory, as a string
     */
    public static String getCurrentDirectory(
            Class<? extends WorkspaceComponent> componentType) {
        return THE_PREFS.get(componentType.getSimpleName(),
                getDefaultDirectory(componentType));
    }

}
