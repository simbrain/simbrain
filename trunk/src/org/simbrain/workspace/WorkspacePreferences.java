/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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


/**
 * <b>WorkspacePreferences</b> handles storage and retrieval of user preferences, e.g. current directory.
 */
public class WorkspacePreferences {
    /** File system property. */
    private static final String FS = System.getProperty("file.separator");

    /** The main user preference object. */
    private static final Preferences THE_PREFS = Preferences.userRoot().node("org/simbrain/workspace");

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
     * Restors user preferences to default values.
     */
    public static void restoreDefaults() {
        setCurrentDirectory(getDefaultCurrentDirectory());
        setDefaultFile(getDefaultDefaultFile());
    }

    //////////////////////////////////////////////////////////////////
    // Getters and setters for user preferences                     //
    // Note that default values for preferences are stored in the   //
    // second argument of the getter method                         //
    //////////////////////////////////////////////////////////////////
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
        return "." + FS + "simulations" + FS + "sims";
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
     * Return the default defalut file.
     *
     * @return the default default file
     */
    public static String getDefaultDefaultFile() {
        return "." + FS + "simulations" + FS + "sims" + FS + "vehicles" + FS  + "two_agents.sim";
    }
}
