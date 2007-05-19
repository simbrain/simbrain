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
package org.simbrain.gauge.core;

import org.simbrain.gauge.GaugePreferences;


/**
 * <b>Settings</b> stores gauge parameters which must persist when the projection algorithm is changed, but which
 * should not be static (which must be different when different instances of the Gauge class are created).  Examples
 * include settings particular to a specific projection  algorithm.
 */
public class Settings {
    /** Method for adding new datapoints. */
    public static final String REFRESH = "Refresh";
    /** Method for adding new datapoints. */
    public static final String TRIANGULATE = "Triangulate";
    /** Method for adding new datapoints. */
    public static final String NN_SUBSPACE = "Nearest Neighbor Subspace";

    /** List of available add methods. */
    private static String[] addMethods = {Settings.REFRESH, Settings.NN_SUBSPACE, Settings.TRIANGULATE };

    /**
     *  General projection settings.
     */
    /** Distance within which added points are considered old and are thus not added. */
    protected double tolerance = GaugePreferences.getTolerance();
    /** Amount by which to perturb overlapping points. */
    protected double perturbationAmount = GaugePreferences.getPerturbationAmount();
    /** Method to add new datapoints. */
    protected String addMethod = GaugePreferences.getAddMethod();

    /**
     * Sammon Map Settings.
     * epsilon or "magic factor"
     */
    private double epsilon = GaugePreferences.getEpsilon();

    /** Coordinate Projection Settings. */
    private int hiD1 = GaugePreferences.getHiDim1();

    /** Coordinate Projection Settings. */
    private int hiD2 = GaugePreferences.getHiDim2();
    /** Automatically use most variant dimensions. */
    private boolean autoFind = GaugePreferences.getAutoFind();

    /**
     * @return whether coordinate projection is in auto-find mode
     */
    public boolean isAutoFind() {
        return autoFind;
    }

    /**
     * @return epsilon value for Sammon map
     */
    public double getEpsilon() {
        return epsilon;
    }

    /**
     * @return first coordinate projection axis for coordinate projection
     */
    public int getHiD1() {
        return hiD1;
    }

    /**
     * @return second coordinate projection axis for coordinate projection
     */
    public int getHiD2() {
        return hiD2;
    }

    /**
     * @param b whether coordinate projection is in auto-find mode
     */
    public void setAutoFind(final boolean b) {
        autoFind = b;
    }

    /**
     * @param d epsilon value for Sammon map
     */
    public void setEpsilon(final double d) {
        epsilon = d;
    }

    /**
     * @param i first coordinate projection axis for coordinate projection
     */
    public void setHiD1(final int i) {
        hiD1 = i;
    }

    /**
     * @param i second coordinate projection axis for coordinate projection
     */
    public void setHiD2(final int i) {
        hiD2 = i;
    }

    /**
     * @return how much to perturb overlapping points
     */
    public double getPerturbationAmount() {
        return perturbationAmount;
    }

    /**
     * @return distance within which added points are not considered new
     */
    public double getTolerance() {
        return tolerance;
    }

    /**
     * @param d how much to perturn overlapping points
     */
    public void setPerturbationAmount(final double d) {
        perturbationAmount = d;
    }

    /**
     * @param d distance within which new points are not considered new.
     */
    public void setTolerance(final double d) {
        tolerance = d;
    }

    /**
     * @return what method is being used to add new points
     */
    public String getAddMethod() {
        return addMethod;
    }

    /**
     * @param i method to use to add new points
     */
    public void setAddMethod(final String i) {
        addMethod = i;
    }

    /**
     * @return addMethods string.
     */
    public static String[] getAddMethods() {
        return addMethods;
    }
}
