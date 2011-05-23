package org.simbrain.world.odorworld;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * <b>OdorWorldPreferences</b> handles storage and retrieval of user
 * preferences. Currently not used.
 */
public final class OdorWorldPreferences {

    /** System specific file system separator. */
    public static final String FS = System.getProperty("file.separator");

    /**
     * Default constructor.
     */
    private OdorWorldPreferences() {

    };

    /**
     * The main user preference object.
     */
    private static final Preferences THEPREFS = Preferences.userRoot().node(
            "/org/simbrain/world/odorworld");

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
}
