/*
 * Part of HiSee, a tool for visualizing high dimensional datasets
 * 
 * Copyright (C) 2004 Scott Hotton <http://www.math.smith.edu/~zeno/> and 
 * Jeff Yoshimi <www.jeffyoshimi.net>
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
 * <b>Settings</b> stores gauge parameters which must persist when the projection algorithm is changed,
 *  but which should not be static (which must be different when different instances of the Gauge
 *  class are created).  Examples include settings particular to a specific projection 
 *  algorithm.
 */
public class Settings {

	public static final String REFRESH = "Refresh";
	public static final String TRIANGULATE = "Triangulate";
	public static final String NN_SUBSPACE = "Nearest Neighbor Subspace";
	
	// List of available add methods
	public static String[] addMethods = {Settings.REFRESH, Settings.NN_SUBSPACE, Settings.TRIANGULATE};

	// General projection settings
	protected double tolerance = .005; //Distance within which added points are considered old and are thus not added
	protected double perturbationAmount = .1; //Amount by which to perturb overlapping points
	protected String addMethod = REFRESH;
	
	// Sammon Map Settings
	private double epsilon = GaugePreferences.getEpslion();	// epsilon or "magic factor"	
	
	// Coordinate Projection Settings
	private int hi_d1 = 0, hi_d2 = 1;
	private boolean autoFind = true;
	
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
	public int getHi_d1() {
		return hi_d1;
	}

	/**
	 * @return second coordinate projection axis for coordinate projection
	 */
	public int getHi_d2() {
		return hi_d2;
	}

	/**
	 * @param b whether coordinate projection is in auto-find mode
	 */
	public void setAutoFind(boolean b) {
		autoFind = b;
	}

	/**
	 * @param d epsilon value for Sammon map
	 */
	public void setEpsilon(double d) {
		epsilon = d;
	}

	/**
	 * @param i first coordinate projection axis for coordinate projection
	 */
	public void setHi_d1(int i) {
		hi_d1 = i;
	}

	/**
	 * @param i second coordinate projection axis for coordinate projection
	 */
	public void setHi_d2(int i) {
		hi_d2 = i;
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
	public void setPerturbationAmount(double d) {
		perturbationAmount = d;
	}

	/**
	 * @param d distance within which new points are not considered new.
	 */
	public void setTolerance(double d) {
		tolerance = d;
	}

	/**
	 * @return what method is being used to add new points
	 */
	public String getAddMethod() {
		return addMethod;
	}

	/**
	 * @param i what method to use to add new points
	 */
	public void setAddMethod(String i) {
		addMethod = i;
	}

}
