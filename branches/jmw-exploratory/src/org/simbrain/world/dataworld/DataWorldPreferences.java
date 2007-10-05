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
package org.simbrain.world.dataworld;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Preferences for the data world package.
 */
public final class DataWorldPreferences {

    /** System file separator property. */
    public static final String FS = System.getProperty("file.separator");

    /** The main user preference object. */
    private static final Preferences PREFERENCES = Preferences.userRoot().node("/org/simbrain/world/odorworld");


    /**
     * Private default constructor.
     */
    private DataWorldPreferences() {
        // empty
    }


    /**
     * Save all user preferences.
     */
    public static void saveAll() {
        try {
            PREFERENCES.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * Restore the default properties.
     */
    public static void restoreDefaults() {
        setCurrentDirectory(getDefaultCurrentDirectory());
    }

    /**
     * Set the current directory preference setting to <code>dir</code>.
     *
     * @param dir current directory preference setting
     */
    public static void setCurrentDirectory(final String dir) {
        PREFERENCES.put("CurrentDirectory", dir);
    }

    /**
     * Return the current directory preference setting.  Defaults to
     * the value returned by <code>getDefaultCurrentDirectory()</code>.
     *
     * @return the current directory preference setting
     */
    public static String getCurrentDirectory() {
        return PREFERENCES.get("CurrentDirectory", getDefaultCurrentDirectory());
    }

    /**
     * Return the default current directory.
     *
     * @return the default current directory
     */
    public static String getDefaultCurrentDirectory() {
        return "." + FS + "simulations" + FS + "worlds";
    }
}
