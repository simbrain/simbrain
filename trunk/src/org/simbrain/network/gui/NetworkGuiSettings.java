package org.simbrain.network.gui;

import java.awt.Color;

public class NetworkGuiSettings {

    /** Color of all lines in rootNetwork panel. */
    private static Color lineColor =  Color.black;

    /** Color of background. */
    private static Color backgroundColor = Color.white;

    /** Color of "active" neurons, with positive values. */
    private static float hotColor = Color.red.getRGB();

    /** Color of "inhibited" neurons, with negative values. */
    private static float coolColor = Color.blue.getRGB();

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

    /** Maximum diameter of the circle representing the synapse. */
    private static int minDiameter = 5;
    
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
