package org.simbrain.world.odorworld;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


public class OdorWorldPreferences {
    public static final String FS = System.getProperty("file.separator");

    //The main user preference object
    private static final Preferences thePrefs = Preferences.userRoot().node("/org/simbrain/world/odorworld");

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
    }

    //////////////////////////////////////////////////////////////////
    // Getters and setters for user preferences                     //
    // Note that default values for preferences are stored in the   //
    // second argument of the getter method                         //
    //////////////////////////////////////////////////////////////////
    public static void setCurrentDirectory(String dir) {
        thePrefs.put("CurrentDirectory", dir);
    }

    public static String getCurrentDirectory() {
        return thePrefs.get("CurrentDirectory", getDefaultCurrentDirectory());
    }

    public static String getDefaultCurrentDirectory() {
        return "." + FS + "simulations" + FS + "worlds";
    }
}
