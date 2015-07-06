/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.util;

import java.awt.Color;
import java.util.HashMap;
import java.util.prefs.Preferences;

/**
 * A wrapper for Java's preference framework, which encapsulates the main needs
 * of preferences in Simbrain:
 * <ol>
 * <li>Getting a preference value for a property</li>
 * <li>Setting a preference value for a property</li>
 * <li>Restoring a property's value to its default setting</li>
 * <li>Setting default values for a property</li>
 * </ol>
 * To do the last item 4 use the static initializer here. 1-3 as done using the
 * static methods of this class.
 * <p>
 * For sample usage see
 * {@link org.simbrain.network.desktop.DesktopNetworkDialog}
 * <p>
 * Note that if no default value is found in the default map no exception is
 * thrown and a "default-default" (a default for the default value) is assigned.
 * This is 0 for numbers and an empty string for string.
 *
 * @author Jeff Yoshimi
 *
 */
public class SimbrainPreferences {

    /** The main preference object. */
    private static final Preferences SIMBRAIN_PREFERENCES = Preferences
            .userRoot().node("/org/simbrain");

    /** Mapping from property names to default values. */
    private static final HashMap<String, Object> DEFAULT_MAP = new HashMap<String, Object>();

    /** System specific file separator. */
    private static final String FS = System.getProperty("file.separator");

    /** Initialize default map */
    static {
        DEFAULT_MAP.put("workspaceBaseDirectory", "." + FS
                + "simulations");
        DEFAULT_MAP.put("workspaceSimulationDirectory", "." + FS
                + "simulations" + FS + "workspaces");
        DEFAULT_MAP.put("workspaceNetworkDirectory", "." + FS + "simulations"
                + FS + "networks");
        DEFAULT_MAP.put("workspaceOdorWorldDirectory", "." + FS + "simulations"
                + FS + "worlds");
        DEFAULT_MAP.put("workspaceTableDirectory", "." + FS + "simulations"
                + FS + "tables");
        DEFAULT_MAP.put("workspaceScriptDirectory", "." + FS + "scripts"
                + FS + "scriptMenu");
        DEFAULT_MAP.put("networkBackgroundColor", Color.WHITE.getRGB());
        DEFAULT_MAP.put("networkLineColor", Color.BLACK.getRGB());
        DEFAULT_MAP.put("networkHotNodeColor",
                Color.RGBtoHSB(255, 0, 0, null)[0]);
        DEFAULT_MAP.put("networkCoolNodeColor",
                Color.RGBtoHSB(0, 0, 255, null)[0]);
        DEFAULT_MAP.put("networkSpikingColor", Color.YELLOW.getRGB());
        DEFAULT_MAP.put("networkExcitatorySynapseColor", Color.RED.getRGB());
        DEFAULT_MAP.put("networkInhibitorySynapseColor", Color.BLUE.getRGB());
        DEFAULT_MAP.put("networkZeroWeightColor", Color.LIGHT_GRAY.getRGB());
        DEFAULT_MAP.put("networkSynapseMinSize", 7);
        DEFAULT_MAP.put("networkSynapseMaxSize", 20);
        DEFAULT_MAP.put("networkNudgeAmount", 2d);
        DEFAULT_MAP.put("networkSynapseVisibilityThreshold", 200);
        DEFAULT_MAP.put("networkWandRadius", 40);
        DEFAULT_MAP.put("networkTableDirectory", "." + FS + "simulations" + FS
                + "tables");
        DEFAULT_MAP.put("projectorTolerance", .1);
        DEFAULT_MAP.put("projectorSammonPerturbationAmount", .1);
        DEFAULT_MAP.put("projectorSammonEpsilon", .5);
        DEFAULT_MAP.put("textWorldDictionaryDirectory", ".");
        DEFAULT_MAP.put("visionWorldDirectory", ".");
    }

    /**
     * Reverts a setting to its default value.
     *
     * @param key the name of the property whose value should be reverted
     */
    public static void restoreDefaultSetting(String key) {
        Object object = DEFAULT_MAP.get(key);
        //System.out.println(key + "--" + object);
        if (object != null) {
            if (object instanceof String) {
                SIMBRAIN_PREFERENCES.put(key, (String) object);
            } else if (object instanceof Double) {
                SIMBRAIN_PREFERENCES.putDouble(key, (Double) object);
            } else if (object instanceof Integer) {
                SIMBRAIN_PREFERENCES.putInt(key, (Integer) object);
            } else if (object instanceof Float) {
                SIMBRAIN_PREFERENCES.putFloat(key, (Float) object);
            }
        }
    }

    /**
     * Set a property whose value is a String.
     *
     * @param property the property name
     * @param val the string value
     */
    public static void putString(String property, String val) {
        SIMBRAIN_PREFERENCES.put(property, val);
    }

    /**
     * Retrieve a property whose value is a string.
     *
     * @param property the property name
     * @return the retrieved value
     * @throws PropertyNotFoundException exception thrown when the property is
     *             not found
     */
    public static String getString(String property)
            throws PropertyNotFoundException {
        String defVal = (String) DEFAULT_MAP.get(property);
        if (defVal == null) {
            defVal = "";
        }
        String value = SIMBRAIN_PREFERENCES.get(property, defVal);
        if (value == null) {
            throw new PropertyNotFoundException("Property " + property
                    + "not found.");
        } else {
            return value;
        }
    }

    /**
     * Set a property whose value is a double.
     *
     * @param property the property name
     * @param val the double value
     */
    public static void putDouble(String property, double val) {
        SIMBRAIN_PREFERENCES.putDouble(property, val);
    }

    /**
     * Retrieve a property whose value is a double.
     *
     * @param property the property name
     * @return the retrieved value
     * @throws PropertyNotFoundException exception thrown when the property is
     *             not found
     */
    public static double getDouble(String property)
            throws PropertyNotFoundException {
        Double defVal = (Double) DEFAULT_MAP.get(property);
        if (defVal == null) {
            defVal = 0d;
        }
        Double value = SIMBRAIN_PREFERENCES.getDouble(property, defVal);
        if (value == null) {
            throw new PropertyNotFoundException("Property " + property
                    + "not found.");
        } else {
            return value;
        }
    }

    /**
     * Set a property whose value is a integer.
     *
     * @param property the property name
     * @param val the int value
     */
    public static void putInt(String property, int val) {
        SIMBRAIN_PREFERENCES.putInt(property, val);
    }

    /**
     * Retrieve a property whose value is an integer.
     *
     * @param property the property name
     * @return the retrieved value
     * @throws PropertyNotFoundException exception thrown when the property is
     *             not found
     */
    public static int getInt(String property) throws PropertyNotFoundException {
        Integer defVal = (Integer) DEFAULT_MAP.get(property);
        if (defVal == null) {
            defVal = 0;
        }
        Integer value = SIMBRAIN_PREFERENCES.getInt(property, defVal);
        if (value == null) {
            throw new PropertyNotFoundException("Property " + property
                    + "not found.");
        } else {
            return value;
        }
    }

    /**
     * Set a property whose value is a float.
     *
     * @param property the property name
     * @param val the float value
     */
    public static void putFloat(String property, float val) {
        SIMBRAIN_PREFERENCES.putFloat(property, val);
    }

    /**
     * Retrieve a property whose value is a float.
     *
     * @param property the property name
     * @return the retrieved value
     * @throws PropertyNotFoundException exception thrown when the property is
     *             not found
     */
    public static float getFloat(String property)
            throws PropertyNotFoundException {
        Float defVal = (Float) DEFAULT_MAP.get(property);
        if (defVal == null) {
            defVal = 0f;
        }
        Float value = SIMBRAIN_PREFERENCES.getFloat(property, defVal);
        if (value == null) {
            throw new PropertyNotFoundException("Property " + property
                    + "not found.");
        } else {
            return value;
        }
    }

    /**
     * Thrown when a property cannot be found.
     */
    public static class PropertyNotFoundException extends Exception {

        /**
         * Construct the exception with an error message, indicating the
         * property name that could not be found.
         *
         * @param string the property name that could not be found.
         */
        public PropertyNotFoundException(String string) {
            super(string);
        }

    }

}
