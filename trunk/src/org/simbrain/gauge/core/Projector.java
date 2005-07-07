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

import com.Ostermiller.util.CSVParser;
/**
 * <b>Projector</b> is a class describing a projection algorithm, which contains
 * a high dimensional dataset (an "upstairs") and a low-dimensional projection of that
 * high dimensional data (a "downstairs").  Classes which extend this class
 * provide different ways of projecting the high dimensional space to the lowdimensional 
 * space. This class provides general methods for handling pairs of datasets and checking
 * their integrity.
 */
public abstract class Projector {

	// A set of hi-d datapoints, each of which is an array of doubles
	// The data to be projected
	protected Dataset upstairs;
	
	// A set of low-d datapoints, each of which is an array of doubles
	// The projection of the upstairs data
	protected Dataset downstairs;

	// Reference to an object which contains information which must persist between
	// re-inits of the projector	
	protected Settings theSettings;

	// Current selected add methods
	protected String addMethod;
		
	/**
	 * Initialize the projector with high and low-d data
	 * 
	 * @param up Reference to high dimensional dataset
	 * @param down Reference to low dimensional dataset.  Pass NULL if not available
	 */
	public void init(Dataset up, Dataset down) {
		
		//System.out.println("In projector.init(up, down)");
		
		upstairs = up;
		downstairs = down;
	
		addMethod = theSettings.getAddMethod();
		
		// For cases where the low-dimensional data has not been provided,
		// create a coordinate projection
		if (down == null) {			

			downstairs = new Dataset();
			downstairs.init(2, upstairs.getNumPoints());
				
			// Initialy use coordinate projection
			// Creates a new projector.
			ProjectCoordinate initialProjection = new ProjectCoordinate(theSettings);
			initialProjection.init(upstairs, downstairs);
			initialProjection.project();
				
			downstairs.setDataset(initialProjection.getDownstairs().getDataset());
		}
	
		checkDatasets();  
		
	}
	
	/**
	 * Check validity of datasets
	 */
	public void checkDatasets() {
		if ((upstairs == null) || (downstairs == null) || (upstairs.getNumPoints() == 0)) {
			//System.out.println("Could not invoke Projector.init()");
			return;
		}
		//System.out.println("In projector.init()");
		upstairs.init();
		downstairs.init();		
		compareDatasets();
	}
	
	/**
	 * Initilize a projector when only the dimension of the dataset is known
	 * 
	 * @param dims Dimensionality of the new dataset
	 */
	public void init(int dims) {
		Dataset up = new Dataset(dims, 0);
		init(up, null);	
	}
	
	/**
	 * Add new high-d datapoints and reinitialize the datasets
	 * 
	 * @param theFile file containing the high-d data, forwarded to a dataset method
	 */
	public void addUpstairs(File theFile) {
		String[][] values = null;
		CSVParser theParser = null;

		try {
			theParser =
				new CSVParser(new FileInputStream(theFile), "", "", "#");
			// # is a comment delimeter in net files
			values = theParser.getAllValues();
		} catch (Exception e) {
			System.out.println("Could not open file stream: " + e.toString());
		}

		String[] line;
		double[] dataPoint;
		for (int i = 0; i < values.length; i++) {
			line = values[i];
			dataPoint = new double[values[0].length];
			for (int j = 0; j < line.length; j++) {
				//System.out.print(" " + line[j]);
				dataPoint[j] = Double.parseDouble(line[j]);
			}
			//System.out.println();
			addDatapoint(dataPoint);
		}

		init(upstairs, downstairs);
	}

	
	/**	 
	 * Chcek the integrity of the two datasets by checking:
	 * 
	 * 				(1) That the low-d set is at least 2 dimensions 
	 * 				(2) That the low d space is lower dimensional than the hi d space
	 * 				(3) That both datasets have the same number of points
	 * 
	 * @param lowD is the dimension we want for our low dimensional space
	 * @return true if low dimensions are lower than hi dimensions and low dimension is less than one 
	 */
	public boolean compareDatasets() {


		if (downstairs.getDimensions() < 1) {
			System.out.println("WARNING: The dimension of the low dimensional data set");
			System.out.println("cannot be less than 1");
			return false;
		}

		if (downstairs.getDimensions()  > upstairs.getDimensions()) {
			System.out.println("WARNING: The dimension of the low dimensional data set");
			System.out.println("cannot be greater than the dimension of the hi");
			System.out.println("dimensional data set.\n");
			System.out.println("hiDimension = " + upstairs.getDimensions() + "\n");
			System.out.println("lowD = " + downstairs.getDimensions());
			return false;
		}
		
		if (downstairs.getNumPoints() != upstairs.getNumPoints()) {
			System.out.println("WARNING: The number of points in the hi-d set (" + upstairs.getNumPoints() + "" +
				") does not match that in the low-d set (" + downstairs.getNumPoints() + ")\n");
			return false;
		}
		return true;
	}
	
	
	/**
	 * Perform operations necessay to project the data.
	 * For iterable functions this is just a stub.
	 */
	public abstract void project();
	

	/**
	 * Iterate, if it is iterable.  For non iterable-projectors this is just a stub
	 * 
	 * @return the current error
	 */		
	public abstract double iterate();

	/**
	 * @return true if this projection algorithm is iterable, false otherwise
	 */	
	public abstract boolean isIterable();
	
	/**
	 * @return true if this projection algorithm accepts new new points, false otherwise
	 */
	public abstract boolean isExtendable();
	
	/**
	 * @return the high-dimensional dataset
	 */
	public Dataset getUpstairs() {
		return upstairs;
	}

	/**
	 * @return the low dimensional dataset
	 */
	public Dataset getDownstairs() {
		return downstairs;
	}
	
	
	/**
	 * Add a datapoint to the upstairs dataset, and a corresponding point to the
	 * downstairs dataset.  Add the new point using the currently selected method
	 * 
	 * @param point point to be added
	 */
	public void addDatapoint(double[] point) {
		
		if (upstairs.addPoint(point,theSettings.getTolerance()) == true) {
			
			//Add the upstairs point
			upstairs.init();
			
			//Add the downstairs point differently depending on the add method
			double[] newPoint;
			if (point.length == 1) {
				newPoint = new double[] {point[0], 0};	
				downstairs.addPoint(newPoint);
				downstairs.init();		
			} else if (theSettings.getAddMethod() == Settings.REFRESH) {		
				newPoint = AddData.coordinate(theSettings.getHi_d1(), theSettings.getHi_d2(),point);
				downstairs.addPoint(newPoint);
				downstairs.init();
				this.project();
			} else if (theSettings.getAddMethod() == Settings.TRIANGULATE) {
				newPoint = AddData.triangulate(upstairs, downstairs, point);
				downstairs.addPoint(newPoint);
				downstairs.init();
			} else if (theSettings.getAddMethod() == Settings.NN_SUBSPACE) {
				newPoint = AddData.nn_subspace(upstairs, downstairs, point);
				downstairs.addPoint(newPoint);
				downstairs.init();
		}
			
		}
	}
	
	
	//	/**
	//	 * Iterate dataset until error is below minimError
	//	 *
	//	 * @param minimumError Minimum error, below which the gauge stops being iterated
	//	 */
	//	public void iterate_to_error(int minimumError) {
	//		while (true) {
	//			if (theProjector.iterate() < minimumError) {
	//				break;
	//			}
	//		}
	//	}



	/**
	 * @return distance within which points are considered unique
	 */
	public double getTolerance() {
		return theSettings.getTolerance();
	}

	/**
	 * @param d distance within which points are considered unique
	 */
	public void setTolerance(double d) {
		theSettings.setTolerance(d);
	}

	/**
	 * @return amount by which to perturb datapoints which overlap
	 */
	public double getPerturbationAmount() {
		return theSettings.getPerturbationAmount();
	}

	/**
	 * @param d amount by which to perturb datapoints which overlap
	 */
	public void setPerturbationAmount(double d) {
		theSettings.setPerturbationAmount(d);
	}

	/**
	 * @return reference to object which contains preferences which persist 
	 * 			when the projector is re-initialized
	 */
	public Settings getTheSettings() {
		return theSettings;
	}

	/**
	 * @param settings reference to object which contains preferences which persist 
	 * 			when the projector is re-initialized
	 */
	public void setTheSettings(Settings settings) {
		theSettings = settings;
	}

	/**
	 * @return name of the add method currently selected
	 */
	public String getAddMethod() {
		return theSettings.getAddMethod();
	}

	/**
	 * Indices sed in populating combo box in general dialog
	 * 
	 * @return index of the add method currently selected
	 */
	public int getAddMethodIndex() {
		if (getAddMethod().equals(Settings.REFRESH)) {
			return 0;
		} else if (getAddMethod().equals(Settings.NN_SUBSPACE)) {
			return 1;
		} else if (getAddMethod().equals(Settings.TRIANGULATE)) {
			return 2;
		}
		return 0;
	}
	
	
	
	/**
	 * @param string
	 */
	public void setAddMethod(String string) {
		theSettings.setAddMethod(string);
	}

}
