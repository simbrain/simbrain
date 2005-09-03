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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.simbrain.gauge.graphics.GaugePanel;
import org.simbrain.gauge.GaugePreferences;
import org.simbrain.gauge.GaugedVariables;

/**
 * <b>Gauge</b> is the main class of the high dimensional visualizer, which 
 * provides methods for changing and initializing
 * various projection algorithms.
 */
public class Gauge {
	
	//Reference to object containing projection settings
	Settings projectorSettings = new Settings();
	
	//Reference to variables this gauge gauges
	private GaugedVariables gaugedVars;

	
	//References to projection objects
	private Projector currentProjector = new ProjectPCA(projectorSettings);
		
	//Application parameters
	private double error = 0;
	boolean isOn = true;
	
	// TO ADD A NEW PROJECTION ALGORITHM:
	// Create a projection class modeled on any of the Project_ classes, 
	// which implements Projector, and make appropriate places in locations
	// ONE, TWO, and THREE below.  You must also change the updateProjectionMenu() method
	// in gaugePanel.
	// If there is a dialog box associated with this projector, then changes will have
	// to be made to org.hisee.graphics.GaugePanel.handlePreferenceDialogs() as well

	//List of available projection algorithms
	public static final String[] PROJECTOR_LIST =
	{
		//ONE: Add name of new projection algorithm
		"Sammon", "PCA", "Coordinate"
	};
	
	public Gauge() {
		gaugedVars = new GaugedVariables(this);
	}
	
	/**
	 * Update the projector; used when loading a dataset or changing projection methods
	 */
	public void updateProjector() {
		
		if ((currentProjector == null) || (getUpstairs() == null)) {
			return;
		}
		
		currentProjector.checkDatasets();
		currentProjector.project();
		
	}
	
	
	/**
	 * Initialize gauge to accept data of a specified dimension.
	 * 
	 * @param dims dimensionality of the high dimensional dataset
	 */
	public void init(int dims) {
		currentProjector.init(dims);
	}	
	
	public void openHighDDataset(File file) {
		Dataset data = new Dataset();
		data.readData(file);
		getCurrentProjector().init(data, null);
		updateProjector();
	}
	
	/**
	 * Add a new point to the dataset, using the currently selected add method.
	 * 
	 * @param point the point to add
	 */
	public void addDatapoint(double[] point) {
		
		if ((currentProjector == null) || (getUpstairs() == null)) {
			return;
		}
		
		
		if (isOn() == true) {
			currentProjector.addDatapoint(point);
			//This is needed to invoke the current projector's init function
			if (currentProjector.isIterable()) {
				currentProjector.init(getUpstairs(), getDownstairs()); 
			}
			error = 0;
		}
	}
	
	/**
	 * Iterate the dataset some fixed number of times
	 *
	 * @param num_times Number of times to iterate the gauge
	 */
	public void iterate(int num_times) {
				
		if(currentProjector.isIterable() == false) {
			return;
		}
		
		int iterations = 0;
		
		while (iterations < num_times) {
			error = currentProjector.iterate();
			iterations++;
		}

	}

		
	/**
	 * @return list of projector types, by name
	 */
	public static String[] getProjectorList() {
		return PROJECTOR_LIST;
	}
	
	/**
	 * @param string the name of the projection algorithm to switch to
	 */
	public void setCurrentProjector(String proj) {
		
		if (proj == null) {
			return;
		}
		
		setCurrentProjector(getProjectorByName(proj));
	}
	
	public Projector getProjectorByName(String name) {
	
		//THREE: Add code below to associate a projector with its name
		Projector ret = null;
		if (name.equalsIgnoreCase("Sammon")) {
			ret = new ProjectSammon(projectorSettings);
		} else if (name.equalsIgnoreCase("Coordinate")) {
			ret = new ProjectCoordinate(projectorSettings);
		} else if (name.equalsIgnoreCase("PCA")) {
			ret = new ProjectPCA(projectorSettings);
		} 	
		return ret;
	}
	
	public Projector getCurrentProjectorC() {
		return getCurrentProjector();
	}
	/**
	 * Used by Castor
	 * @param proj
	 */
	public void setCurrentProjectorC(Projector proj) {
		currentProjector = proj;
	}
	
	/**
	 * @param string the new projection algorithm
	 */
	public void setCurrentProjector(Projector proj) {

		if ((proj == null) || (getUpstairs() == null)) {
			return;
		}
		
		//Initialize the new projector with the datasets of the current projector
		proj.init(getUpstairs(), getDownstairs());
		currentProjector = proj;	
		updateProjector();
	}
	
	/**
	 * @return the current projection algorithm
	 */
	public Projector getCurrentProjector() {
		return currentProjector;
	}

	/**
	 *  Convenience method to get upstairs dataset
	 * 
	 *  @return hi-dimensional dataset associated with current projector
	 */
	public Dataset getUpstairs() {
		if (currentProjector == null) {
			return null;
		}
		return currentProjector.getUpstairs();
	}

	/**
	 *  Convenience method to get downstairs dataset
	 * 
	 *  @return low-dimensional dataset associated with current projector
	 */
	public Dataset getDownstairs() {
		if (currentProjector == null) {
			return null;
		}

		return currentProjector.getDownstairs();
	}


	/**
	 * Returns error, which is only set by some projection functions
	 * 
	 * @return current error
	 */
	public double getError() {
		return error;
	}

	/**
	 * If the gauge is on it should actively represent changing states of the network
	 * 
	 * @return true if the gauge is on
	 */
	public boolean isOn() {
		return isOn;
	}

	/**
	 * Turn the gauge on and off; i.e., allow new data or not.  Used mainly
	 * when the Gauge is a component in another application
	 * 
	 * @param b 
	 */
	public void setOn(boolean b) {
		isOn = b;
	}
	
	public GaugedVariables getGaugedVars() {
		return gaugedVars;
	}

	public void setGaugedVars(GaugedVariables gaugedVars) {
		this.gaugedVars = gaugedVars;
	}

}
