/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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

package org.simnet;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * <b>NetworkPreferences</b> handles storage and retrieval of User Preferences for the 
 * neural network.
 */
public class NetworkPreferences {

	//The main user preference object
	private static final Preferences thePrefs =
		Preferences.userRoot().node("/org/simbrain/simnet");

	/**
	 * Save all user preferences
	 */
	public static void saveAll() {
		try {
			thePrefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	//------------ Neuron Preferences --------------------// 	

	public static String getActivationFunction() {
		return thePrefs.get("activationFunction", "Linear");
	}
	public static void setActivationFunction(String s) {
		thePrefs.put("activationFunction", s);
	}	
	public static String getOutputFunction() {
		return thePrefs.get("outputFunction", "Threshold");
	}
	public static void setOutputFunction(String s) {
		thePrefs.put("outputFunction", s);
	}	
	public static double getActivation() {
		return thePrefs.getDouble("Activation", 0); // 0 is default
	}
	public static double getActivationThreshold() {
		return thePrefs.getDouble("activationThreshold", .5);
	}
	public static double getBias() {
		return thePrefs.getDouble("Bias", 0);
	}
	public static double getDecay() {
		return thePrefs.getDouble("Decay", 0);
	}
	public static double getNrnIncrement() {
		return thePrefs.getDouble("nrnIncrement", 1);
	}
	public static double getNrnLowerBound() {
		return thePrefs.getDouble("nrnLowerBound", -10);
	}
	public static double getOutputSignal() {
		return thePrefs.getDouble("outputSignal", 1);
	}
	public static double getOutputThreshold() {
		return thePrefs.getDouble("outputThreshold", .5);
	}
	public static double getNrnUpperBound() {
		return thePrefs.getDouble("nrnUpperBound", 10);
	}
	public static void setActivation(double act) {
		thePrefs.putDouble("Activation", act);
	}
	public static void setActivationThreshold(double d) {
		thePrefs.putDouble("activationThreshold", d);
	}	
	public static void setBias(double d) {
		thePrefs.putDouble("Bias", d);
	}
	public static void setDecay(double d) {
		thePrefs.putDouble("Decay", d);
	}
	public static void setNrnIncrement(double d) {
		thePrefs.putDouble("nrnIncrement", d);
	}
	public static void setNrnLowerBound(double d) {
		thePrefs.putDouble("nrnLowerBound", d);
	}
	public static void setOutputSignal(double d) {
		thePrefs.putDouble("outputSignal", d);
	}
	public static void setOutputThreshold(double d) {
		thePrefs.putDouble("outputThreshold", d);
	}
	public static void setNrnUpperBound(double d) {
		thePrefs.putDouble("nrnUpperBound", d);
	}

	//------------ Weight Preferences --------------------// 	

	public static String getLearningRule() {
		return thePrefs.get("learningRule", "Hebbian");
	}
	public static double getStrength() {
		return thePrefs.getDouble("strength", 10);
	}
	public static double getMomentum() {
		return thePrefs.getDouble("momentum", .2);
	}
	public static double getWtIncrement() {
		return thePrefs.getDouble("wtIncrement", 10);
	}
	public static double getWtLowerBound() {
		return thePrefs.getDouble("wtLowerBound", 10);
	}
	public static double getWtUpperBound() {
		return thePrefs.getDouble("wtUpperBound", 10);
	}
	public static void setLearningRule(String s) {
		thePrefs.put("learningRule", s);
	}
	public static void setMomentum(double d) {
		thePrefs.putDouble("momentum", d);
	}
	public static void setStrength(double d) {
		thePrefs.putDouble("strength", d);
	}
	public static void setWtIncrement(double d) {
		thePrefs.putDouble("wtIncrement", d);
	}
	public static void setWtLowerBound(double d) {
		thePrefs.putDouble("wtLowerBound", d);
	}
	public static void setWtUpperBound(double d) {
		thePrefs.putDouble("wtUpperBound", d);
	}

}
