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
package org.simbrain.network.gui;

import java.awt.Color;

/**
 * This class stores the settings for the network GUI. They are set to default
 * values which can be modified by the user. This class is used in two ways:
 *
 * 1) By itself: When the network is run as an applet, or separately from the
 * desktop environment, this class is used by itself. Settings last as long as
 * the application is open.
 *
 * 2) In conjunction with user preferences. When the network is run in the
 * simbrain desktop, these settings are overwritten by
 * org.simbrain.network.desktop.NetworkGuiPreferences, which uses the java user
 * preferences API. This means that settings last between sessions, and are
 * stored on a users local machine.
 */
public class NetworkGuiSettings {

    /** Color of all lines in Network panel. */
    private static Color lineColor =  Color.black;

    /** Color of background. */
    private static Color backgroundColor = Color.white;

    /** Color of "active" neurons, with positive values. */
    private static float hotColor = Color.RGBtoHSB(255, 0, 0, null)[0];

    /** Color of "inhibited" neurons, with negative values. */
    private static float coolColor = Color.RGBtoHSB(0, 0, 255, null)[0];

    /** Color of "excitatory" synapses, with positive values. */
    private static Color excitatoryColor = Color.red;

    /** Color of "inhibitory" synapses, with negative values. */
    private static Color inhibitoryColor = Color.blue;

    /** Color of "spiking" synapse. */
    private static Color spikingColor = Color.yellow;

    /** Color of "zero" weights. */
    private static Color zeroWeightColor = Color.gray;

    /** How much to nudge objects per key click. */
    private static double nudgeAmount = .1;

    /** Maximum diameter of the circle representing the synapse. */
    private static int maxDiameter = 20;

    /** Minimum diameter of the circle representing the synapse. */
    private static int minDiameter = 7;

    /**
     * @return Returns the lineColor.
     */
    public static Color getLineColor() {
        return lineColor;
    }

    /**
     * @param lc The lineColor to set.
     */
    public static void setLineColor(final Color lc) {
        lineColor = lc;
    }

    /**
     * @return the backgroundColor
     */
    public static Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * @param bgc the backgroundColor to set
     */
    public static void setBackgroundColor(final Color bgc) {
        backgroundColor = bgc;
    }

    /**
     * @return the hotColor
     */
    public static float getHotColor() {
        return hotColor;
    }

    /**
     * @param hc the hotColor to set
     */
    public static void setHotColor(final float hc) {
        hotColor = hc;
    }

    /**
     * @return the coolColor
     */
    public static float getCoolColor() {
        return coolColor;
    }

    /**
     * @param coolClr the coolColor to set
     */
    public static void setCoolColor(final float coolClr) {
        coolColor = coolClr;
    }

    /**
     * @return the excitatoryColor
     */
    public static Color getExcitatoryColor() {
        return excitatoryColor;
    }

    /**
     * @param exciteColor the excitatoryColor to set
     */
    public static void setExcitatoryColor(final Color exciteColor) {
        excitatoryColor = exciteColor;
    }

    /**
     * @return the inhibitoryColor
     */
    public static Color getInhibitoryColor() {
        return inhibitoryColor;
    }

    /**
     * @param inhibitColor the inhibitoryColor to set
     */
    public static void setInhibitoryColor(final Color inhibitColor) {
        inhibitoryColor = inhibitColor;
    }

    /**
     * @return the spikingColor
     */
    public static Color getSpikingColor() {
        return spikingColor;
    }

    /**
     * @param spikeColor the spikingColor to set
     */
    public static void setSpikingColor(final Color spikeColor) {
        spikingColor = spikeColor;
    }

    /**
     * @return the zeroWeightColor
     */
    public static Color getZeroWeightColor() {
        return zeroWeightColor;
    }

    /**
     * @param zeroSynColor the zeroWeightColor to set
     */
    public static void setZeroWeightColor(final Color zeroSynColor) {
        zeroWeightColor = zeroSynColor;
    }

    /**
     * @return the nudgeAmount
     */
    public static double getNudgeAmount() {
        return nudgeAmount;
    }

    /**
     * @param nudge the nudgeAmount to set
     */
    public static void setNudgeAmount(final double nudge) {
        nudgeAmount = nudge;
    }

    /**
     * @return the maxDiameter
     */
    public static int getMaxDiameter() {
        return maxDiameter;
    }

    /**
     * @param maxDiam the maxDiameter to set
     */
    public static void setMaxDiameter(final int maxDiam) {
        maxDiameter = maxDiam;
    }

    /**
     * @return the minDiameter
     */
    public static int getMinDiameter() {
        return minDiameter;
    }

    /**
     * @param minDiam the minDiameter to set
     */
    public static void setMinDiameter(final int minDiam) {
        minDiameter = minDiam;
    }

}
