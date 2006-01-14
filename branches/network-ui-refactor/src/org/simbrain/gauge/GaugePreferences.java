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
package org.simbrain.gauge;

import java.awt.Color;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


/**
 * <b>GaugePreferences</b> handles storage and retrieval of user preferences, e.g.  background color for the gauge
 * panel, point size, etc.
 */
public class GaugePreferences {
    /** User preferences. */
    private static final Preferences thePrefs = Preferences.userRoot().node("/org/simbrain/gauge");
    /** File system seperator. */
    public static final String FS = System.getProperty("file.separator");

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
     * Restores general preferences to default values.
     */
    public static void restoreGeneralDefaults() {
        setPerturbationAmount(getDefaultPerturbationAmount());
        setTolerance(getDefaultTolerance());
        setAddMethod(getDefaultAddMethod());
        setDefaultProjector(getDefaultDefaultProjector());
        setCurrentDirectory(getDefaultCurrentDirectory());
    }

    /**
     * Restores graphics preferences to default values.
     */
    public static void restoreGraphicsDefaults() {
        setBackgroundColor(getDefaultBackgroundColor());
        setHotColor(getDefaultHotColor());
        setDefaultColor(getDefaultDefaultColor());
        setColorDataPoints(getDefaultColorDataPoints());
        setShowError(getDefaultShowError());
        setShowStatusBar(getDefaultShowStatusBar());
        setPointSize(getDefaultPointSize());
        setIterationsBetweenUpdates(getDefaultIterationsBetweenUpdates());
    }

    /**
     * Restores coordinate preferences to default values.
     *
     */
    public static void restoreCoordinateDefaults() {
        setHiDim1(getDefaultHiDim1());
        setHiDim2(getDefaultHiDim2());
        setAutoFind(getDefaultAutoFind());
    }

    /**
     * Restores sammon preferences to default values.
     */
    public static void restoreSammonDefaults() {
        setEpsilon(getDefaultEpsilon());
    }

    //////////////////////////////////////////////////////////////////
    // Getters and setters for user preferences                     //
    // Note that default values for preferences are stored in the   //
    // second argument of the getter method                         //
    //////////////////////////////////////////////////////////////////
    /**
     * @param rgbColor Sets Background color.
     */
    public static void setBackgroundColor(final int rgbColor) {
        thePrefs.putInt("BackgroundColor", rgbColor);
    }

    /**
     * @return Current background color.
     */
    public static int getBackgroundColor() {
        return thePrefs.getInt("BackgroundColor", getDefaultBackgroundColor());
    }

    /**
     * @return Default background color.
     */
    public static int getDefaultBackgroundColor() {
        return Color.WHITE.getRGB();
    }

    /**
     * @param rgbColor Sets hot color.
     */
    public static void setHotColor(final int rgbColor) {
        thePrefs.putInt("HotColor", rgbColor);
    }

    /**
     * @return Current hot color.
     */
    public static int getHotColor() {
        return thePrefs.getInt("HotColor", getDefaultHotColor());
    }

    /**
     * @return Default hot color.
     */
    public static int getDefaultHotColor() {
        return Color.RED.getRGB();
    }

    /**
     * @param rgbColor Sets default color.
     */
    public static void setDefaultColor(final int rgbColor) {
        thePrefs.putInt("DefaultColor", rgbColor);
    }

    /**
     * @return Default color.
     */
    public static int getDefaultColor() {
        return thePrefs.getInt("DefaultColor", getDefaultDefaultColor());
    }

    /**
     * @return Default default color.
     */
    public static int getDefaultDefaultColor() {
        return Color.BLUE.getRGB();
    }

    /**
     * @param tolerance Sets tolerance.
     */
    public static void setTolerance(final double tolerance) {
        thePrefs.putDouble("Tolerance", tolerance);
    }

    /**
     * @return Tolerance.
     */
    public static double getTolerance() {
        return thePrefs.getDouble("Tolerance", getDefaultTolerance());
    }

    /**
     * @return Default tolerance
     */
    public static double getDefaultTolerance() {
        return .05;
    }

    /**
     * @param amount Sets pertubation amount.
     */
    public static void setPerturbationAmount(final double amount) {
        thePrefs.putDouble("PerturbationAmount", amount);
    }

    /**
     * @return Current perturbation amount.
     */
    public static double getPerturbationAmount() {
        return thePrefs.getDouble("PerturbationAmount", getDefaultPerturbationAmount());
    }

    /**
     * @return Default perturbation amount.
     */
    public static double getDefaultPerturbationAmount() {
        return .1;
    }

    /**
     * @param error Sets show error.
     */
    public static void setShowError(final boolean error) {
        thePrefs.putBoolean("ShowError", error);
    }

    /**
     * @return Current show error value.
     */
    public static boolean getShowError() {
        return thePrefs.getBoolean("ShowError", getDefaultShowError());
    }

    /**
     * @return Default show error value.
     */
    public static boolean getDefaultShowError() {
        return false;
    }

    /**
     * @param statusBar Sets show status bar.
     */
    public static void setShowStatusBar(final boolean statusBar) {
        thePrefs.putBoolean("ShowStatusBar", statusBar);
    }

    /**
     * @return Current set status bar value.
     */
    public static boolean getShowStatusBar() {
        return thePrefs.getBoolean("ShowStatusBar", getDefaultShowStatusBar());
    }

    /**
     * @return Default show status bar value.
     */
    public static boolean getDefaultShowStatusBar() {
        return true;
    }

    /**
     * @param dataPoints Sets color data points.
     */
    public static void setColorDataPoints(final boolean dataPoints) {
        thePrefs.putBoolean("ColorDataPoints", dataPoints);
    }

    /**
     * @return Current color data points value.
     */
    public static boolean getColorDataPoints() {
        return thePrefs.getBoolean("ColorDataPoints", getDefaultColorDataPoints());
    }

    /**
     * @return Default color data points value.
     */
    public static boolean getDefaultColorDataPoints() {
        return false;
    }

    /**
     * @param size Sets the size of data points.
     */
    public static void setPointSize(final double size) {
        thePrefs.putDouble("PointSize", size);
    }

    /**
     * @return Current point size.
     */
    public static double getPointSize() {
        return thePrefs.getDouble("PointSize", getDefaultPointSize());
    }

    /**
     * @return Default point size.
     */
    public static double getDefaultPointSize() {
        return 1;
    }

    /**
     * @param iterations Sets number of iterations between updates.
     */
    public static void setIterationsBetweenUpdates(final int iterations) {
        thePrefs.putInt("IterationsBetweenUpdates", iterations);
    }

    /**
     * @return Current number of iterations between updates.
     */
    public static int getIterationsBetweenUpdates() {
        return thePrefs.getInt("IterationsBetweenUptates", getDefaultIterationsBetweenUpdates());
    }

    /**
     * @return Default number of iterations between updates.
     */
    public static int getDefaultIterationsBetweenUpdates() {
        return 10;
    }

    /**
     * @param epsilon Sets epsilon value.
     */
    public static void setEpsilon(final double epsilon) {
        thePrefs.putDouble("Epsilon", epsilon);
    }

    /**
     * @return Current epslion value.
     */
    public static double getEpsilon() {
        return thePrefs.getDouble("Epsilon", getDefaultEpsilon());
    }

    /**
     * @return Defalut epsilon value.
     */
    public static double getDefaultEpsilon() {
        return 3;
    }

    /**
     * @param dim Sets high dimension 1 value.
     */
    public static void setHiDim1(final int dim) {
        thePrefs.putInt("HiDim1", dim);
    }

    /**
     * @return Current high dimension 1 value.
     */
    public static int getHiDim1() {
        return thePrefs.getInt("HiDim1", getDefaultHiDim1());
    }

    /**
     * @return Default high dimension 1 value.
     */
    public static int getDefaultHiDim1() {
        return 0;
    }

    /**
     * @param dim Sets high dimension 2 value.
     */
    public static void setHiDim2(final int dim) {
        thePrefs.putInt("HiDim2", dim);
    }

    /**
     * @return Current high dimension 2 value.
     */
    public static int getHiDim2() {
        return thePrefs.getInt("HiDim2", getDefaultHiDim2());
    }

    /**
     * @return Default high dimension 2 value.
     */
    public static int getDefaultHiDim2() {
        return 1;
    }

    /**
     * @param autoFind Sets auto find value.
     */
    public static void setAutoFind(final boolean autoFind) {
        thePrefs.putBoolean("AutoFind", autoFind);
    }

    /**
     * @return Current auto find value.
     */
    public static boolean getAutoFind() {
        return thePrefs.getBoolean("AutoFind", getDefaultAutoFind());
    }

    /**
     * @return Defalut auto find value.
     */
    public static boolean getDefaultAutoFind() {
        return true;
    }

    /**
     * @param addMethod Sets current add method value.
     */
    public static void setAddMethod(final String addMethod) {
        thePrefs.put("AddMethod", addMethod);
    }

    /**
     * @return Current add method value.
     */
    public static String getAddMethod() {
        return thePrefs.get("AddMethod", getDefaultAddMethod());
    }

    /**
     * @return Default add method value.
     */
    public static String getDefaultAddMethod() {
        return "Refresh";
    }

    /**
     * @param defaultProjector Sets default projector value.
     */
    public static void setDefaultProjector(final String defaultProjector) {
        thePrefs.put("DefaultProjector", defaultProjector);
    }

    /**
     * @return Current default projector value.
     */
    public static String getDefaultProjector() {
        return thePrefs.get("DefaultProjector", getDefaultDefaultProjector());
    }

    /**
     * @return Default default projector value.
     */
    public static String getDefaultDefaultProjector() {
        return "PCA";
    }

    /**
     * @param dir Sets current directory value.
     */
    public static void setCurrentDirectory(final String dir) {
        thePrefs.put("CurrentDirectory", dir);
    }

    /**
     * @return Current directory value.
     */
    public static String getCurrentDirectory() {
        return thePrefs.get("CurrentDirectory", getDefaultCurrentDirectory());
    }

    /**
     * @return Default current directory.
     * 
     */
    public static String getDefaultCurrentDirectory() {
        return "." + FS + "simulations" + FS + "gauges";
    }
}
