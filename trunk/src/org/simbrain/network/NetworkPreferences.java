/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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
package org.simbrain.network;

import java.awt.Color;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


/**
 * <b>NetworkPreferences</b> handles storage and retrieval of user preferences, e.g.  background color for the network
 * panel, default weight values, etc.
 */
public class NetworkPreferences {
    /** System specific file seperator. */
    private static final String FS = System.getProperty("file.separator");
    /**The main user preference object. */
    private static final Preferences NETWORK_PREFERENCES = Preferences.userRoot().node("/org/simbrain/network");

    /**
     * Save all user preferences.
     */
    public static void saveAll() {
        try {
            NETWORK_PREFERENCES.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * Restores defaults.
     *
     */
    public static void restoreDefaults() {
        setBackgroundColor(getDefaultBackgroundColor());
        setLineColor(getDefaultLineColor());
        setHotColor(getDefaultHotColor());
        setCoolColor(getDefaultCoolColor());
        setExcitatoryColor(getDefaultExcitatoryColor());
        setInhibitoryColor(getDefaultInhibitoryColor());
        setLassoColor(getDefaultLassoColor());
        setSelectionColor(getDefaultSelectionColor());
        setSignalColor(getDefaultSignalColor());
        setSpikingColor(getDefaultSpikingColor());
        setZeroWeightColor(getDefaultZeroWeightColor());
        setMaxDiameter(getDefaultMaxDiameter());
        setMinDiameter(getDefaultMinDiameter());
        setTimeStep(getDefaultTimeStep());
        setTimeUnits(getDefaultTimeUnits());
        setPrecision(getDefaultPrecision());
        setWeightValues(getDefaultWeightValues());
        setUsingIndent(getDefaultUsingIndent());
        setNudgeAmount(getDefaultNudgeAmount());
        setCurrentDirectory(getDefaultCurrentDirectory());
    }

    //////////////////////////////////////////////////////////////////
    // Getters and setters for user preferences                     //
    // Note that default values for preferences are stored in the   //
    // second argument of the getter method                         //
    //////////////////////////////////////////////////////////////////
    /**
     * Network background color.
     * @param rgbColor Color to be used as background
     */
    public static void setBackgroundColor(final int rgbColor) {
        NETWORK_PREFERENCES.putInt("NetworkBackgroundColor", rgbColor);
    }

    /**
     * Network background color.
     * @return Perferred background color
     */
    public static int getBackgroundColor() {
        return NETWORK_PREFERENCES.getInt("NetworkBackgroundColor", getDefaultBackgroundColor());
    }

    /**
     * Network background color.
     * @return Default background color
     */
    public static int getDefaultBackgroundColor() {
        return Color.WHITE.getRGB();
    }

    /**
     * Network line color.
     * @param rgbColor Color of line
     */
    public static void setLineColor(final int rgbColor) {
        NETWORK_PREFERENCES.putInt("NetworkLineColor", rgbColor);
    }

    /**
     * Network line color.
     * @return Perferred line color
     */
    public static int getLineColor() {
        return NETWORK_PREFERENCES.getInt("NetworkLineColor", getDefaultLineColor());
    }

    /**
     * Network line color.
     * @return Default line color
     */
    public static int getDefaultLineColor() {
        return Color.BLACK.getRGB();
    }

    /**
     * Network hot node color.
     * @param theColor Color of hot node
     */
    public static void setHotColor(final float theColor) {
        NETWORK_PREFERENCES.putFloat("NetworkHotColor", theColor);
    }

    /**
     * Network hot node color.
     * @return Perferred hot node color
     */
    public static float getHotColor() {
        return NETWORK_PREFERENCES.getFloat("NetworkHotColor", getDefaultHotColor());
    }

    /**
     * Network hot node color.
     * @return Default hot node color
     */
    public static float getDefaultHotColor() {
        return Color.RGBtoHSB(255, 0, 0, null)[0];
    }

    /**
     * Network cool node color.
     * @param theColor Color of cool node
     */
    public static void setCoolColor(final float theColor) {
        NETWORK_PREFERENCES.putFloat("NetworkCoolColor", theColor);
    }

    /**
     * Network cool node color.
     * @return Perferred cool node color
     */
    public static float getCoolColor() {
        return NETWORK_PREFERENCES.getFloat("NetworkCoolColor", getDefaultCoolColor());
    }

    /**
     * Network cool node color.
     * @return Default cool node color
     */
    public static float getDefaultCoolColor() {
        return Color.RGBtoHSB(0, 0, 255, null)[0];
    }

    /**
     * Network excitatory color.
     * @param rgbColor Excitatory neuron color
     */
    public static void setExcitatoryColor(final int rgbColor) {
        NETWORK_PREFERENCES.putInt("NetworkExcitatoryColor", rgbColor);
    }

    /**
     * Network excitatory color.
     * @return Perferred excitatory neuron color
     */
    public static int getExcitatoryColor() {
        return NETWORK_PREFERENCES.getInt("NetworkExcitatoryColor", getDefaultExcitatoryColor());
    }

    /**
     * Network excitatory color.
     * @return Default excitatory neuron color
     */
    public static int getDefaultExcitatoryColor() {
        return Color.RED.getRGB();
    }

    /**
     * Network inhibitory color.
     * @param rgbColor Inhibitory neuron color
     */
    public static void setInhibitoryColor(final int rgbColor) {
        NETWORK_PREFERENCES.putInt("NetworkInhibitoryColor", rgbColor);
    }

    /**
     * Network inhibitory color.
     * @return Perferred inhibitory neuron color
     */
    public static int getInhibitoryColor() {
        return NETWORK_PREFERENCES.getInt("NetworkInhibitoryColor", getDefaultInhibitoryColor());
    }

    /**
     * Network inhibitory color.
     * @return Default inhibitory color
     */
    public static int getDefaultInhibitoryColor() {
        return Color.BLUE.getRGB();
    }

    /**
     * Network lasso color.
     * @param rgbColor Color of lasso
     */
    public static void setLassoColor(final int rgbColor) {
        NETWORK_PREFERENCES.putInt("NetworkLassoColor", rgbColor);
    }

    /**
     * Network lasso color.
     * @return Perferred lasso color
     */
    public static int getLassoColor() {
        return NETWORK_PREFERENCES.getInt("NetworkLassoColor", getDefaultLassoColor());
    }

    /**
     * Network lasso color.
     * @return Default lasso color
     */
    public static int getDefaultLassoColor() {
        return Color.GREEN.getRGB();
    }

    /**
     * Network selection color.
     * @param rgbColor Color of selection
     */
    public static void setSelectionColor(final int rgbColor) {
        NETWORK_PREFERENCES.putInt("NetworkSelectionColor", rgbColor);
    }

    /**
     * Network selection color.
     * @return Perferred selection color
     */
    public static int getSelectionColor() {
        return NETWORK_PREFERENCES.getInt("NetworkSelectionColor", getDefaultSelectionColor());
    }

    /**
     * Network selection color.
     * @return Default selection color
     */
    public static int getDefaultSelectionColor() {
        return Color.GREEN.getRGB();
    }

    /**
     * Network signal synapse color.
     * @param rgbColor Color of signal synapse
     */
    public static void setSignalColor(final int rgbColor) {
        NETWORK_PREFERENCES.putInt("SignalSynapse", rgbColor);
    }

    /**
     * Network signal synapse color.
     * @return Perferred signal synapse color
     */
    public static int getSignalColor() {
        return NETWORK_PREFERENCES.getInt("SignalSynapse", getDefaultSignalColor());
    }

    /**
     * Network signal synapse color.
     * @return Default signal synapse color
     */
    public static int getDefaultSignalColor() {
        return Color.GREEN.getRGB();
    }

    /**
     * Network zero weight color.
     * @param rgbColor Color of zero weight
     */
    public static void setZeroWeightColor(final int rgbColor) {
        NETWORK_PREFERENCES.putInt("ZeroWeight", rgbColor);
    }

    /**
     * Network zero weight color.
     * @return Perferred zero weight color
     */
    public static int getZeroWeightColor() {
        return NETWORK_PREFERENCES.getInt("ZeroWeight", getDefaultZeroWeightColor());
    }

    /**
     * Network zero weight color.
     * @return Default zero weight color
     */
    public static int getDefaultZeroWeightColor() {
        return Color.LIGHT_GRAY.getRGB();
    }

    /**
     * Network max node radius.
     * @param sizeMax Maximum node radius
     */
    public static void setMaxDiameter(final int sizeMax) {
        NETWORK_PREFERENCES.putInt("NetworkSizeMax", sizeMax);
    }

    /**
     * Network max node radius.
     * @return Maximum node radius
     */
    public static int getMaxDiameter() {
        return NETWORK_PREFERENCES.getInt("NetworkSizeMax", getDefaultMaxDiameter());
    }

    /**
     * Network max node radius.
     * @return Default maximum node radius
     */
    public static int getDefaultMaxDiameter() {
        return 20;
    }

    /**
     * Network min node radius.
     * @param sizeMin Minimum node radius
     */
    public static void setMinDiameter(final int sizeMin) {
        NETWORK_PREFERENCES.putInt("NetworkSizeMin", sizeMin);
    }

    /**
     * Network min node radius.
     * @return Minumum node radius
     */
    public static int getMinDiameter() {
        return NETWORK_PREFERENCES.getInt("NetworkSizeMin", getDefaultMinDiameter());
    }

    /**
     * Network min node radius.
     * @return Default minimum node radius
     */
    public static int getDefaultMinDiameter() {
        return 15;
    }

    /**
     * Netowork time step.
     * @param step Time step
     */
    public static void setTimeStep(final double step) {
        NETWORK_PREFERENCES.putDouble("TimeStep", step);
    }

    /**
     * Netowork time step.
     * @return Perferred time step
     */
    public static double getTimeStep() {
        return NETWORK_PREFERENCES.getDouble("TimeStep", getDefaultTimeStep());
    }

    /**
     * Netowork time step.
     * @return Default time step
     */
    public static double getDefaultTimeStep() {
        return .01;
    }

    /**
     * Network time units.
     * @param units Time units
     */
    public static void setTimeUnits(final int units) {
        NETWORK_PREFERENCES.putInt("TimeUnits", units);
    }

    /**
     * Network time units.
     * @return Perferred time units
     */
    public static int getTimeUnits() {
        return NETWORK_PREFERENCES.getInt("TimeUnits", getDefaultTimeUnits());
    }

    /**
     * Network time units.
     * @return Default time units
     */
    public static int getDefaultTimeUnits() {
        return 0;
    }

    /**
     * Network precision.
     * @param precision Precision
     */
    public static void setPrecision(final int precision) {
        NETWORK_PREFERENCES.putInt("NetworkPrecision", precision);
    }

    /**
     * Network precision.
     * @return Perferred precision
     */
    public static int getPrecision() {
        return NETWORK_PREFERENCES.getInt("NetworkPrecision", getDefaultPrecision());
    }

    /**
     * Network precision.
     * @return Default precision
     */
    public static int getDefaultPrecision() {
        return 0;
    }

    /**
     * Network weight values.
     * @param weightValues Use weight values
     */
    public static void setWeightValues(final boolean weightValues) {
        NETWORK_PREFERENCES.putBoolean("NetworkWeightValues", weightValues);
    }

    /**
     * Network weight values.
     * @return Use weight values
     */
    public static boolean getWeightValues() {
        return NETWORK_PREFERENCES.getBoolean("NetworkWeightValues", getDefaultWeightValues());
    }

    /**
     * Network weight values.
     * @return Default use weight values
     */
    public static boolean getDefaultWeightValues() {
        return false;
    }

   /**
    * Network files indenting.
    * @param indent Use indenting
    */
    public static void setUsingIndent(final boolean indent) {
        NETWORK_PREFERENCES.putBoolean("NetworkIndent", indent);
    }

    /**
     * Network files indenting.
     * @return Indenet preference
     */
    public static boolean getUsingIndent() {
        return NETWORK_PREFERENCES.getBoolean("NetworkIndent", getDefaultUsingIndent());
    }

    /**
     * Network files indenting.
     * @return Default indenting
     */
    public static boolean getDefaultUsingIndent() {
        return true;
    }

    /**
     * Network nudging.
     * @param nudge Nudge amount
     */
    public static void setNudgeAmount(final double nudge) {
        NETWORK_PREFERENCES.putDouble("NetworkNudgeAmount", nudge);
    }

    /**
     * Network nudging.
     * @return Perferred nudge amount
     */
    public static double getNudgeAmount() {
        return NETWORK_PREFERENCES.getDouble("NetworkNudgeAmount", getDefaultNudgeAmount());
    }

    /**
     * Network nudging.
     * @return Default nudge amount
     */
    public static double getDefaultNudgeAmount() {
        return 2;
    }

    /**
     * Current network files directory.
     * @param dir Current directory
     */
    public static void setCurrentDirectory(final String dir) {
        NETWORK_PREFERENCES.put("CurrentDirectory", dir);
    }

    /**
     * Current network files directory.
     * @return Current directory
     */
    public static String getCurrentDirectory() {
        return NETWORK_PREFERENCES.get("CurrentDirectory", getDefaultCurrentDirectory());
    }

    /**
     * Current network files directory.
     * @return Default current directory
     */
    public static String getDefaultCurrentDirectory() {
        return "." + FS + "simulations" + FS + "networks";
    }

    /**
     * Current backprop files directory.
     * @param dir Current directory
     */
    public static void setCurrentBackpropDirectory(final String dir) {
        NETWORK_PREFERENCES.put("BackpropDirectory", dir);
    }

    /**
     * Current backprop files directory.
     * @return Current directory
     */
    public static String getCurrentBackpropDirectory() {
        return NETWORK_PREFERENCES.get("BackpropDirectory", getDefaultBackpropDirectory());
    }

    /**
     * Current backprop files directory.
     * @return Default backprop directory
     */
    public static String getDefaultBackpropDirectory() {
        return "." + FS + "simulations" + FS + "networks" + FS + "bp" + FS + "training";
    }

    /**
     * Current SOM files directory.
     * @param dir Current directory
     */
    public static void setCurrentSOMDirectory(final String dir) {
        NETWORK_PREFERENCES.put("SOMDirectory", dir);
    }

    /**
     * Current SOM files directory.
     * @return Current directory
     */
    public static String getCurrentSOMDirectory() {
        return NETWORK_PREFERENCES.get("SOMDirectory", getDefaultSOMDirectory());
    }

    /**
     * Current SOM files directory.
     * @return Default backprop directory
     */
    public static String getDefaultSOMDirectory() {
        return "." + FS + "simulations" + FS + "networks" + FS + "bp" + FS + "training";
    }

    /**
     * Sets the spiking syanapse color.
     * @param rgbColor Color to set spiking syanapse
     */
    public static void setSpikingColor(final int rgbColor) {
        NETWORK_PREFERENCES.putInt("SpikingColor", rgbColor);
    }

    /**
     * Returns the current spiking synapse color.
     * @return Current spiking synapse color
     */
    public static int getSpikingColor() {
        return NETWORK_PREFERENCES.getInt("SpikingColor", getDefaultSpikingColor());
    }

    /**
     * Returns the default spiking synapse color.
     * @return Default spiking syanpse color
     */
    public static int getDefaultSpikingColor() {
        return Color.YELLOW.getRGB();
    }
}
