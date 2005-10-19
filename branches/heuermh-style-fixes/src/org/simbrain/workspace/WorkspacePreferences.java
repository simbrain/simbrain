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
 * <b>WorkspacePreferences</b>
 */
public class WorkspacePreferences {
    private static final String FS = System.getProperty("file.separator");

    //The main user preference object
    private static final Preferences thePrefs = Preferences.userRoot().node("org/simbrain/workspace");

    /**
     * Save all user preferences
     */
    public static void saveAll() {
        try {
            thePrefs.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    public static void restoreDefaults() {
        setCurrentDirectory(getDefaultCurrentDirectory());
        setDefaultFile(getDefaultDefaultFile());
    }

    //////////////////////////////////////////////////////////////////  
    // Getters and setters for user preferences                     //
    // Note that default values for preferences are stored in the   //
    // second argument of the getter method                         //
    //////////////////////////////////////////////////////////////////  
    public static void setCurrentDirectory(String dir) {
        thePrefs.put("currentDirectory", dir);
    }

    public static String getCurrentDirectory() {
        return thePrefs.get("currentDirectory", getDefaultCurrentDirectory());
    }

    public static String getDefaultCurrentDirectory() {
        return "." + FS + "simulations" + FS + "sims";
    }

    public static void setDefaultFile(String file) {
        thePrefs.put("defaultFile", file);
    }

    public static String getDefaultFile() {
        return thePrefs.get("defaultFile", getDefaultDefaultFile());
    }

    public static String getDefaultDefaultFile() {
        return "." + FS + "simulations" + FS + "sims" + FS + "two_agents.xml";
    }
}
