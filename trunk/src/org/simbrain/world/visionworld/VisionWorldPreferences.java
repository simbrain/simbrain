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
package org.simbrain.world.visionworld;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * <b>VisionWorldPreferences</b> handles storage and retrieval of user preferences, e.g. current directory.
 */
public class VisionWorldPreferences {
    /** System specific file separator. */
    private static final String FS = System.getProperty("file.separator");
    /**The main user preference object. */
    private static final Preferences VISION_WORLD_PREFERENCES = Preferences.userRoot().node("/org/simbrain/worlds");

    /**
     * Private no-arg constructor.
     */
    private VisionWorldPreferences() {
        // empty
    }

    /**
     * Save all user preferences.
     */
    public static void saveAll() {
        try {
            VISION_WORLD_PREFERENCES.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * Restores defaults.
     *
     */
    public static void restoreDefaults() {
        setCurrentDirectory(getDefaultDirectory());
    }

    /**
     * Sets the current vision world directory.
     * @param dir Directory to set as current
     */
    public static void setCurrentDirectory(final String dir) {
        VISION_WORLD_PREFERENCES.put("CurrentDirectory", dir);
    }

    /**
     * The current vision world directory.
     * @return Current directory
     */
    public static String getCurrentDirectory() {
        return VISION_WORLD_PREFERENCES.get("CurrentDirectory", getDefaultDirectory());
    }

    /**
     * The default directory, called when current directory has not been set.
     * @return Default directory
     */
    public static String getDefaultDirectory() {
        return "." + FS + "simulations" + FS + "worlds";
    }
}

