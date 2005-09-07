/*
 * Part of HDV (High-Dimensional-Visualizer), a tool for visualizing high
 * dimensional datasets.
 * 
 * Copyright (C) 2005 Scott Hotton <http://www.math.smith.edu/~zeno/> and 
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
package org.simbrain.gauge;

import java.awt.Color;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * <b>GaugePreferences</b> handles storage and retrieval of user preferences, e.g. 
 * background color for the gauge panel, point size, etc.  
 */
public class GaugePreferences {

    private static final Preferences thePrefs =  Preferences.userRoot().node( "/org/simbrain/gauge" );
    
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
	    setProjector(getDefaultProjector());
	    setTolerance(getDefaultTolerance());
	    setPerturbationAmount(getDefaultPerturbationAmount());
	    setShowError(getDefaultShowError());
	    setShowStatusBar(getDefaultShowStatusBar());
	    setColorDataPoints(getDefaultColorDataPoints());
	    setPointSize(getDefaultPointSize());
	    setIterationsBetweenUpdates(getDefaultIterationsBetweenUpdates());
	    setEpsilon(getDefaultEpsilon());
	    setHiDim1(getDefaultHiDim1());
	    setHiDim2(getDefaultHiDim2());
	    setAutoFind(getDefaultAutoFind());
	}
	
	//////////////////////////////////////////////////////////////////	
	// Getters and setters for user preferences						//
	// Note that default values for preferences are stored in the	//
	// second argument of the getter method							//
 	//////////////////////////////////////////////////////////////////
	public static void setBackgroundColor(int rgbColor){
	    thePrefs.putInt("BackgroundColor", rgbColor);
	}
	public static int getBackgroundColor(){
	    return thePrefs.getInt("BackgroundColor", Color.BLACK.getRGB());
	}
	public static int getDefaultBackgroundColor(){
	    return Color.BLACK.getRGB();
	}
	
	public static void setHotColor(int rgbColor){
	    thePrefs.putInt("HotColor", rgbColor);
	}
	public static int getHotColor(){
	    return thePrefs.getInt("HotColor", Color.RED.getRGB());
	}
	public static int getDefaultHotColor(){
	    return Color.RED.getRGB();
	}
	
	public static void setDefaultColor(int rgbColor){
	    thePrefs.putInt("DefaultColor", rgbColor);
	}
	public static int getDefaultColor(){
	    return thePrefs.getInt("DefaultColor", Color.GREEN.getRGB());
	}
	public static int getDefaultDefaultColor(){
	    return Color.GREEN.getRGB();
	}
	
	public static void setProjector(String projector){
	    thePrefs.put("DefaultProjector", projector);
	}
	public static String getProjector(){
	    return thePrefs.get("DefaultProjector", "PCA");
	}
	public static String getDefaultProjector(){
	    return "PCA";
	}
	
	public static void setTolerance(double tolerance){
	    thePrefs.putDouble("Tolerance", tolerance);
	}
	public static double getTolerance(){
	    return thePrefs.getDouble("Tolerance", .05);
	}
	public static double getDefaultTolerance(){
	    return .05;
	}
	
	public static void setPerturbationAmount(double amount){
	    thePrefs.putDouble("PerturbationAmount", amount);
	}
	public static double getPerturbationAmount(){
	    return thePrefs.getDouble("PerturbationAmount", .1);
	}
	public static double getDefaultPerturbationAmount(){
	    return .1;
	}
	
	public static void setShowError(boolean error){
	    thePrefs.putBoolean("ShowError", error);
	}
	public static boolean getShowError(){
	    return thePrefs.getBoolean("ShowError", false);
	}
	public static boolean getDefaultShowError(){
	    return false;
	}
	
	public static void setShowStatusBar(boolean statusBar){
	    thePrefs.putBoolean("ShowStatusBar", statusBar);
	}
	public static boolean getShowStatusBar(){
	    return thePrefs.getBoolean("ShowStatusBar", true);
	}
	public static boolean getDefaultShowStatusBar(){
	    return true;
	}
	
	public static void setColorDataPoints(boolean dataPoints){
	    thePrefs.putBoolean("ColorDataPoints", dataPoints);
	}
	public static boolean getColorDataPoints(){
	    return thePrefs.getBoolean("ColorDataPoints", false);
	}
	public static boolean getDefaultColorDataPoints(){
	    return false;
	}
	
	public static void setPointSize(double size){
	    thePrefs.putDouble("PointSize", size);
	}
	public static double getPointSize(){
	    return thePrefs.getDouble("PointSize", 1);
	}
	public static double getDefaultPointSize(){
	    return 1;
	}
	
	public static void setIterationsBetweenUpdates(int iterations){
	    thePrefs.putInt("IterationsBetweenUpdates", iterations);
	}
	public static int getIterationsBetweenUpdates(){
	    return thePrefs.getInt("IterationsBetweenUptates", 10);
	}
	public static int getDefaultIterationsBetweenUpdates(){
	    return 10;
	}
	
	public static void setEpsilon(double epsilon){
	    thePrefs.putDouble("Epsilon", epsilon);
	}
	public static double getEpsilon(){
	    return thePrefs.getDouble("Epslion", 3);
	}
	public static double getDefaultEpsilon(){
	    return 3;
	}
	
	public static void setHiDim1(int dim){
	    thePrefs.putInt("HiDim1", dim);
	}
	public static int getHiDim1(){
	    return thePrefs.getInt("HiDim1", 0);
	}
	public static int getDefaultHiDim1(){
	    return 0;
	}
	
	public static void setHiDim2(int dim){
	    thePrefs.putInt("HiDim2", dim);
	}
	public static int getHiDim2(){
	    return thePrefs.getInt("HiDim2", 1);
	}
	public static int getDefaultHiDim2(){
	    return 1;
	}
	
	public static void setAutoFind(boolean autoFind){
	    thePrefs.putBoolean("AutoFind", autoFind);
	}
	public static boolean getAutoFind(){
	    return thePrefs.getBoolean("AutoFind", true);
	}
	public static boolean getDefaultAutoFind(){
	    return true;
	}
}
