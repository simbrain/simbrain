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
import java.util.Properties;

import org.simbrain.gauge.graphics.GaugePanel;
import org.simbrain.gauge.GaugePreferences;

/**
 * <b>Gauge</b> is the main class of the high dimensional visualizer, which manages both command
 * -line and GUI interfaces, and provides methods for changing and initializing
 * various projection algorithms.
 */
public class Gauge {

	// Application parameters
	private boolean usingGraphics = false;
	private boolean useHotPoint = false;
	
	//Reference to object containing projection settings
	Settings projectorSettings = new Settings();
	
	//References to projection objects
	private Projector currentProjector = new ProjectPCA(projectorSettings);
	
	//Reference to gauge panel
	protected GaugePanel gp; 
	
	//Name of gauge,for title bar
	private String name;

	String localDir = new String();

	//Defaault directory
	private String FS = System.getProperty("file.separator");
	private String default_dir =  "." + FS + "simulations" + FS + "gauges";
	
	//Application parameters
	private double error = 0;
	boolean isOn = true;
	private boolean usingOnOff = true;	
	
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
		gp = new GaugePanel(this);
		setUsingGraphics(true);
		setUsingOnOff(true, gp);
		setUsingHotPoint(true);
		setProperties(gp);

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
		
		if (usingGraphics == true) {
			gp.initGaugePanel();
			gp.updateGauge();
			gp.autoscale();
			gp.repaint();
		}
	}
	
	
	/**
	 * Initialize gauge to accept data of a specified dimension.
	 * 
	 * @param dims dimensionality of the high dimensional dataset
	 */
	public void init(int dims) {
		currentProjector.init(dims);
		if (usingGraphics == true) {	
			gp.initGaugePanel();
			gp.updateGauge();
		}
	}
	
	/**
	 * Set default values using hisee.properties file
	 *
	 */
	public void setProperties(GaugePanel gp) {

	    // Read properties file.

	    currentProjector = getProjectorByName(GaugePreferences.getProjector());
	    projectorSettings.setAutoFind(GaugePreferences.getAutoFind());
	    projectorSettings.setEpsilon(GaugePreferences.getEpslion());
	    projectorSettings.setHi_d1(GaugePreferences.getHiDim1());
	    projectorSettings.setHi_d2(GaugePreferences.getHiDim2());
	    projectorSettings.setPerturbationAmount(GaugePreferences.getPerturbationAmount());
	    projectorSettings.setTolerance(GaugePreferences.getTolerance());
	    gp.setShowError(GaugePreferences.getShowError());
	    gp.setShowStatus(GaugePreferences.getShowStatusBar());
	    gp.setColorMode(GaugePreferences.getColorDataPoints());
	    gp.setMinimumPointSize(GaugePreferences.getMinPointSize());
	    gp.setScale(GaugePreferences.getMarginSize());
	    gp.setNumIterationsBetweenUpdate(GaugePreferences.getIterationsBetweenUpdates());
	}
	
	
	public void openHighDDataset(File file) {
		Dataset data = new Dataset();
		data.readData(file);
		getProjector().init(data, null);
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
			
		if (usingGraphics == true) {
			gp.initGaugePanel();
			gp.setHotPoint(getUpstairs().getClosestIndex(point));
			gp.updateGauge();
			gp.autoscale();
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
	public Projector getProjector() {
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
	 * @return name of gauge
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param string name of gauge
	 */
	public void setName(String string) {
		name = string;
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

	/**
	 * 
	 * @return true if the Gauge is in GUI mode, false otherwise
	 */
	public boolean isUsingGraphics() {
		return usingGraphics;
	}

	/**
	 * @param b sets whether the Gauge is using graphics or not
	 */
	public void setUsingGraphics(boolean b) {
		usingGraphics = b;
	}


	/**
	 * Turn off "hot point" capability on or off, which sets the "current" state of
	 * some dataset to a specified color, e.g red
	 *
	 * @param b sets whether hot points are being used or not
	 */
	public void setUsingHotPoint(boolean b) {
		useHotPoint = b;
	}

	/**
	 * @return true if hot points are being used, false otherwise
	 */
	public boolean isUsingHotPoint() {
		return useHotPoint;
	}

	/**
	 * @return the default directory where datasets are stored
	 */
	public String getDefaultDir() {
		return default_dir;
	}

	/**
	 * @param string the default directory where dataset are stored
	 */
	public void setDefaultDir(String string) {
		default_dir = string;
	}

	/**
	 * @return reference to gauge panel, the main GUI component
	 */
	public GaugePanel getGp() {
		return gp;
	}

	/**
	 * The onOff button is only used with certain components, where the ability to add
	 * new data is important.
	 * 
	 * @return whether the onOff button is being being used
	 */
	public boolean isUsingOnOff() {
		return usingOnOff;
	}

	/**
	 * The onOff button is only used with certain components, where the ability to add
	 * new data is important.
	 * 
	 * @param whether the onOff button is being being used
	 */
	public void setUsingOnOff(boolean b, GaugePanel g) {
		if (g.getOnOffBox() != null) {
					g.getOnOffBox().setVisible(b);
		}
		usingOnOff = b;
	}

	/**
	 * @param gp The gp to set.
	 */
	public void setGp(GaugePanel gp) {
		this.gp = gp;
	}
}
