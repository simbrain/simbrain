package org.simbrain.world.odorworld;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * <b>OdorWorldPreferences</b> handles storage and retrieval of user preferences,
 * e.g. the current directory.
 */
public final class OdorWorldPreferences {
    /** System specific file system seperator. */
    public static final String FS = System.getProperty("file.separator");

    /**
     * Default constructor.
     */
    private OdorWorldPreferences() {

    };

    /**
     * The main user preference object.
     */
    private static final Preferences THEPREFS = Preferences.userRoot().node("/org/simbrain/world/odorworld");

    /**
     * Save all user preferences.
     */
    public static void saveAll() {
        try {
            THEPREFS.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * Restore defaults.
     */
    public static void restoreDefaults() {
        setCurrentDirectory(getDefaultCurrentDirectory());
    }

    //////////////////////////////////////////////////////////////////
    // Getters and setters for user preferences                     //
    // Note that default values for preferences are stored in the   //
    // second argument of the getter method                         //
    //////////////////////////////////////////////////////////////////

    /**
     * Sets the current directory where worlds are saved.
     *
     * @param dir Location to save worlds
     */
    public static void setCurrentDirectory(final String dir) {
        THEPREFS.put("CurrentDirectory", dir);
    }

    /**
     * Return the current directory.
     *
     * @return the current directory
     */
    public static String getCurrentDirectory() {
        return THEPREFS.get("CurrentDirectory", getDefaultCurrentDirectory());
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
