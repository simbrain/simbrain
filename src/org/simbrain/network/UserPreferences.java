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

	//////////////////////////////////////////////////////////////////	
	// Getters and setters for user preferences						//
	// Note that default values for preferences are stored in the	//
	// second argument of the getter method							//
 	//////////////////////////////////////////////////////////////////	
	public static void setBackgroundColor(int rgbColor) {
		thePrefs.putInt("NetworkBackgroundColor", rgbColor );	
	}
	public static int getBackgroundColor(){
		return thePrefs.getInt("NetworkBackgroundColor", Color.BLACK.getRGB()); // Black is the default value
	}
}
