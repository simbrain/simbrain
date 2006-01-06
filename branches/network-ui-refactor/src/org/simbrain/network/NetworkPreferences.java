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
    private static final Preferences thePrefs = Preferences.userRoot().node("/org/simbrain/network");

    /**
     * Save all user preferences.
     */
    public static void saveAll() {
        try {
            thePrefs.flush();
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
        setMaxRadius(getDefaultMaxRadius());
        setMinRadius(getDefaultMinRadius());
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
        thePrefs.putInt("NetworkBackgroundColor", rgbColor);
    }

    /**
     * Network background color.
     * @return Perferred background color
     */
    public static int getBackgroundColor() {
        return thePrefs.getInt("NetworkBackgroundColor", getDefaultBackgroundColor());
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
        thePrefs.putInt("NetworkLineColor", rgbColor);
    }

    /**
     * Network line color.
     * @return Perferred line color
     */
    public static int getLineColor() {
        return thePrefs.getInt("NetworkLineColor", getDefaultLineColor());
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
        thePrefs.putFloat("NetworkHotColor", theColor);
    }

    /**
     * Network hot node color.
     * @return Perferred hot node color
     */
    public static float getHotColor() {
        return thePrefs.getFloat("NetworkHotColor", getDefaultHotColor());
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
        thePrefs.putFloat("NetworkCoolColor", theColor);
    }

    /**
     * Network cool node color.
     * @return Perferred cool node color
     */
    public static float getCoolColor() {
        return thePrefs.getFloat("NetworkCoolColor", getDefaultCoolColor());
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
        thePrefs.putInt("NetworkExcitatoryColor", rgbColor);
    }

    /**
     * Network excitatory color.
     * @return Perferred excitatory neuron color
     */
    public static int getExcitatoryColor() {
        return thePrefs.getInt("NetworkExcitatoryColor", getDefaultExcitatoryColor());
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
        thePrefs.putInt("NetworkInhibitoryColor", rgbColor);
    }

    /**
     * Network inhibitory color.
     * @return Perferred inhibitory neuron color
     */
    public static int getInhibitoryColor() {
        return thePrefs.getInt("NetworkInhibitoryColor", getDefaultInhibitoryColor());
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
        thePrefs.putInt("NetworkLassoColor", rgbColor);
    }

    /**
     * Network lasso color.
     * @return Perferred lasso color
     */
    public static int getLassoColor() {
        return thePrefs.getInt("NetworkLassoColor", getDefaultLassoColor());
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
        thePrefs.putInt("NetworkSelectionColor", rgbColor);
    }

    /**
     * Network selection color.
     * @return Perferred selection color
     */
    public static int getSelectionColor() {
        return thePrefs.getInt("NetworkSelectionColor", getDefaultSelectionColor());
    }

    /**
     * Network selection color.
     * @return Default selection color
     */
    public static int getDefaultSelectionColor() {
        return Color.GREEN.getRGB();
    }

    /**
     * Network max node radius.
     * @param sizeMax Maximum node radius
     */
    public static void setMaxRadius(final int sizeMax) {
        thePrefs.putInt("NetworkSizeMax", sizeMax);
    }

    /**
     * Network max node radius.
     * @return Maximum node radius
     */
    public static int getMaxRadius() {
        return thePrefs.getInt("NetworkSizeMax", getDefaultMaxRadius());
    }

    /**
     * Network max node radius.
     * @return Default maximum node radius
     */
    public static int getDefaultMaxRadius() {
        return 16;
    }

    /**
     * Network min node radius.
     * @param sizeMin Minimum node radius
     */
    public static void setMinRadius(final int sizeMin) {
        thePrefs.putInt("NetworkSizeMin", sizeMin);
    }

    /**
     * Network min node radius.
     * @return Minumum node radius
     */
    public static int getMinRadius() {
        return thePrefs.getInt("NetworkSizeMin", getDefaultMinRadius());
    }

    /**
     * Network min node radius.
     * @return Default minimum node radius
     */
    public static int getDefaultMinRadius() {
        return 7;
    }

    /**
     * Netowork time step.
     * @param step Time step
     */
    public static void setTimeStep(final double step) {
        thePrefs.putDouble("TimeStep", step);
    }

    /**
     * Netowork time step.
     * @return Perferred time step
     */
    public static double getTimeStep() {
        return thePrefs.getDouble("TimeStep", getDefaultTimeStep());
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
        thePrefs.putInt("TimeUnits", units);
    }

    /**
     * Network time units.
     * @return Perferred time units
     */
    public static int getTimeUnits() {
        return thePrefs.getInt("TimeUnits", getDefaultTimeUnits());
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
        thePrefs.putInt("NetworkPrecision", precision);
    }

    /**
     * Network precision.
     * @return Perferred precision
     */
    public static int getPrecision() {
        return thePrefs.getInt("NetworkPrecision", getDefaultPrecision());
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
        thePrefs.putBoolean("NetworkWeightValues", weightValues);
    }

    /**
     * Network weight values.
     * @return Use weight values
     */
    public static boolean getWeightValues() {
        return thePrefs.getBoolean("NetworkWeightValues", getDefaultWeightValues());
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
        thePrefs.putBoolean("NetworkIndent", indent);
    }

    /**
     * Network files indenting.
     * @return Indenet preference
     */
    public static boolean getUsingIndent() {
        return thePrefs.getBoolean("NetworkIndent", getDefaultUsingIndent());
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
        thePrefs.putDouble("NetworkNudgeAmount", nudge);
    }

    /**
     * Network nudging.
     * @return Perferred nudge amount
     */
    public static double getNudgeAmount() {
        return thePrefs.getDouble("NetworkNudgeAmount", getDefaultNudgeAmount());
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
        thePrefs.put("CurrentDirectory", dir);
    }

    /**
     * Current network files directory.
     * @return Current directory
     */
    public static String getCurrentDirectory() {
        return thePrefs.get("CurrentDirectory", getDefaultCurrentDirectory());
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
        thePrefs.put("BackpropDirectory", dir);
    }

    /**
     * Current backprop files directory.
     * @return Current directory
     */
    public static String getCurrentBackpropDirectory() {
        return thePrefs.get("BackpropDirectory", getDefaultBackpropDirectory());
    }

    /**
     * Current backprop files directory.
     * @return Default backprop directory
     */
    public static String getDefaultBackpropDirectory() {
        return "." + FS + "simulations" + FS + "networks";
    }

    /**
     * Sets the spiking syanapse color.
     * @param rgbColor Color to set spiking syanapse
     */
    public static void setSpikingColor(final int rgbColor) {
        thePrefs.putInt("SpikingColor", rgbColor);
    }

    /**
     * Returns the current spiking synapse color.
     * @return Current spiking synapse color
     */
    public static int getSpikingColor() {
        return thePrefs.getInt("SpikingColor", getDefaultSpikingColor());
    }

    /**
     * Returns the default spiking synapse color.
     * @return Default spiking syanpse color
     */
    public static int getDefaultSpikingColor() {
        return Color.YELLOW.getRGB();
    }
}
