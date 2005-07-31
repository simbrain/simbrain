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
 
package org.simbrain.network;

import java.awt.Color;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * <b>NetworkPreferences</b> handles storage and retrieval of user preferences, e.g. 
 * background color for the network panel, default weight values, etc.  
 */
public class UserPreferences {

	//The main user preference object
	private static final Preferences thePrefs =  Preferences.userRoot().node( "/org/simbrain/sim/network" );
	
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
	
	public static void restoreDefaults(){
	    setBackgroundColor(getDefaultBackgroundColor());
	    setLineColor(getDefaultLineColor());
	    setHotColor(getDefaultHotColor());
	    setCoolColor(getDefaultCoolColor());
	    setExcitatoryColor(getDefaultExcitatoryColor());
	    setInhibitoryColor(getDefaultInhibitoryColor());
	    setMaxRadius(getDefaultMaxRadius());
	    setMinRadius(getDefaultMinRadius());
	    setPrecision(getDefaultPrecision());
	    setWeightValues(getDefaultWeightValues());
	}

	//////////////////////////////////////////////////////////////////	
	// Getters and setters for user preferences						//
	// Note that default values for preferences are stored in the	//
	// second argument of the getter method							//
 	//////////////////////////////////////////////////////////////////	
	public static void setBackgroundColor(int rgbColor) {
		thePrefs.putInt("NetworkBackgroundColor", rgbColor );	
	}
	public static int getBackgroundColor(){
		return thePrefs.getInt("NetworkBackgroundColor", Color.WHITE.getRGB());
	}
	public static int getDefaultBackgroundColor(){
	    return Color.WHITE.getRGB();
	}
	
	public static void setLineColor(int rgbColor) {
		thePrefs.putInt("NetworkLineColor", rgbColor );	
	}
	public static int getLineColor(){
		return thePrefs.getInt("NetworkLineColor", Color.BLACK.getRGB()); 
	}
	public static int getDefaultLineColor(){
	    return Color.BLACK.getRGB();
	}
	
	public static void setHotColor(float theColor) {
		thePrefs.putFloat("NetworkHotColor", theColor );	
	}
	public static float getHotColor(){
		return thePrefs.getFloat("NetworkHotColor", Color.RGBtoHSB(255,0,0,null)[0]);
	}
	public static float getDefaultHotColor(){
	    return Color.RGBtoHSB(255,0,0,null)[0];
	}
	
	public static void setCoolColor(float theColor) {
		thePrefs.putFloat("NetworkCoolColor", theColor );	
	}
	public static float getCoolColor(){
		return thePrefs.getFloat("NetworkCoolColor", Color.RGBtoHSB(0,0,255,null)[0]); 
	}
	public static float getDefaultCoolColor(){
	    return Color.RGBtoHSB(0,0,255,null)[0];
	}
	
	public static void setExcitatoryColor(int rgbColor) {
		thePrefs.putInt("NetworkExcitatoryColor", rgbColor );	
	}
	public static int getExcitatoryColor(){
		return thePrefs.getInt("NetworkExcitatoryColor", Color.RED.getRGB());
	}
	public static int getDefaultExcitatoryColor(){
	    return Color.RED.getRGB();
	}
	
	public static void setInhibitoryColor(int rgbColor) {
		thePrefs.putInt("NetworkInhibitoryColor", rgbColor );	
	}
	public static int getInhibitoryColor(){
		return thePrefs.getInt("NetworkInhibitoryColor", Color.BLUE.getRGB());
	}
	public static int getDefaultInhibitoryColor(){
	    return Color.BLUE.getRGB();
	}
	
	public static void setMaxRadius(int sizeMax) {
		thePrefs.putInt("NetworkSizeMax", sizeMax );	
	}
	public static int getMaxRadius(){
		return thePrefs.getInt("NetworkSizeMax", 16);
	}
	public static int getDefaultMaxRadius(){
	    return 16;
	}
	
	public static void setMinRadius(int sizeMin) {
		thePrefs.putInt("NetworkSizeMin", sizeMin );	
	}
	public static int getMinRadius(){
		return thePrefs.getInt("NetworkSizeMin", 7); 
	}
	public static int getDefaultMinRadius(){
	    return 7;
	}
	
	public static void setPrecision(int precision) {
		thePrefs.putInt("NetworkPrecision", precision);	
	}
	public static int getPrecision(){
		return thePrefs.getInt("NetworkPrecision", 0);
	}
	public static int getDefaultPrecision(){
	    return 0;
	}
	
	public static void setWeightValues(boolean weightValues) {
		thePrefs.putBoolean("NetworkWeightValues", weightValues );	
	}
	public static boolean getWeightValues(){
		return thePrefs.getBoolean("NetworkWeightValues", false);
	}
	public static boolean getDefaultWeightValues(){
	    return false;
	}
	
	public static void setRounding(boolean rounding) {
		thePrefs.putBoolean("NetworkRounding", rounding );	
	}
	public static boolean getRounding(){
		return thePrefs.getBoolean("NetworkRounding", false); // False is the default value
	}
}
