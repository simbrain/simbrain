/*
 * Part of HiSee, a tool for visualizing high dimensional datasets.
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
import java.io.FileOutputStream;
import java.util.ArrayList;

import Jama.Matrix;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;


/**
 * <b>Dataset</b> represents a set of n-dimensional points. Both the low and high dimensional
 * data of the current {@link org.hisee.core.Projector} are instances of this class. Dataset provides methods for 
 * working with such sets (e.g. open dataset up, adding points, checking their integrity, 
 * finding nearest neighbors of a point, calculating
 * their interpoint distances, etc.). It is assumed that all points in a dataset have
 * the same dimensionality.
 */
public class Dataset {

	 //The data
	 private ArrayList dataset = new ArrayList();

	 // Number of dimensions in the dataset
	 private int dimensions;
	 // Number of points in the dataset
	 private int numPoints;
	 
	 // Matrix of interpoint distances
	 private double[][] distances;

	public Dataset() {	
	}
	
	public Dataset (ArrayList data) {
		dataset = data;
		init();
	}
	
	public Dataset (int ndims,  int npoints) {
		numPoints = npoints;
		dimensions = ndims;
	}
	
	/**
	 * Initialize the dataset, setting the main variables to the property values.
	 * Assumes the dataset already exists, but that it has changed. 
	 */
	public void init() {
		//System.out.println("In init() for " + dimensions + "-d");
		numPoints = dataset.size();
		dimensions = ((double[])dataset.get(0)).length;
		checkConsistentDimensions();
		calculateDistances(); 
	}
	
	/**
	 * Re-initialize a dataset to a specific number of dimensions and number of points.
	 * Populates the dataset with stubs.
	 * 
	 * @param dims Dimensions of the dataset
	 * @param numpoints Number of datapoints in the dataset
	 */
	public void init(int dims, int numpoints) {
		clear();
		dimensions = dims;
		for (int i = 0; i < numpoints; i++) {
			double[] point = new double[dims];
			dataset.add(point);
		}
		numPoints = dataset.size();
	}
	
	/**
	 * Clear all data, high and low dimensional
	 */
	public void clear() {
		dataset.clear();
		dimensions = 0;
		numPoints = 0;
	}
	

	/**
	 * Check that all the vectors in the dataset have
	 * the same dimension
	 */
	public boolean checkConsistentDimensions() {

		double[] point = getPoint(0);
		for (int j = 1; j < numPoints; ++j) {
			point = getPoint(j);
			if (point.length != dimensions) {
				return false;
				// ended up here so there is a point whose dimension
			} // is different from the first point. 
		}
		return true; // made it through so all points have the same dimension
	}

	/**
	 * Randomize dataset to a value between
	 * 0 and upperBound
	 */
	public void randomize(int upperBound) {

		for (int i = 0; i < numPoints; i++) {
			double[] point = new double[dimensions];
			for (int j = 0; j < dimensions; j++) {
				point[j] = Math.random() * upperBound;
			}
			setPoint(i, point);
		}
		calculateDistances();
	}
	
	/**
	 * Calculate inter-point distancese
	 */
	public void calculateDistances() {
				
		distances = new double[numPoints][numPoints];
		double[]  Y_i, Y_j; // temporary variables	
		for (int i = 0; i < numPoints; i++) { // and the sum of dstar[i][j]
			Y_i = (double[]) getPoint(i);
			for (int j = i + 1; j < numPoints; j++) {
				Y_j = (double[]) getPoint(j);
				distances[i][j] = 0.00;
				for (int k = 0; k < dimensions; ++k) {
					distances[i][j] += (Y_i[k] - Y_j[k]) * (Y_i[k] - Y_j[k]);
				}
				distances[i][j] = Math.sqrt(distances[i][j]);
				//Extra copy of distances in lower-triangle sometimes helpful, at slight memory cost
				distances[j][i] = distances[i][j];
			}
		}
			
		//		for (int i = 0; i < distances.length; i++) {
		//			for (int j = 0; j < distances.length; j++) {
		//				System.out.print(" " + distances[i][j]);
		//			}
		//			System.out.println(""); 
		//		}
		//System.out.println("---- distances: " + dimensions); 
	
	}
	
	/**
	 * Get the minimum interpoint distance between points in the dataset.
	 * 
	 * @return minimum distance between any two points in the low-d dataset
	 */
	public double getMinimumDistance() {
		


		if (distances == null) {
			//System.out.println("In getMinimumDistance() for " + dimensions + "-d");
			calculateDistances();
		}
		double l = Double.MAX_VALUE;
		for (int i = 0; i < numPoints; i++ ) {
			for(int j = i+1; j < numPoints; j++) {
				if(distances[i][j] < l) {
					l = distances[i][j];	
				}
			}
		}
		return l;
	}
	
	/**
	 * Get the maximimum interpoint distance between points in the dataset.
	 * 
	 * @return maximum distance between any two points in the low-d dataset
	 */
	public double getMaximumDistance() {

		
		if (distances == null) {
                        //System.out.println("In getMaximumDistance() for " + dimensions + "-d");
			calculateDistances();
		}
		double l = 0;
		for (int i = 0; i < numPoints; i++ ) {
			for(int j = i+1; j < numPoints; j++) {
				if(distances[i][j] > l) {
					l = distances[i][j];	
				}
			}
		}
		return l;
	}
	
	/**
	 * Read in  stored dataset file
	 * 
	 * @param file_name Name of file to read in
	 * @param gauge Reference to the gauge to be created from the stored dataset
	 */
	public void readData(File file) {

		String[][] values = null;
		CSVParser theParser = null;

		clear();

		try {
			theParser =
				new CSVParser(new FileInputStream(file), "", "", "#");
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
			dataset.add(dataPoint);
		}

		init();
	}
	
	/**
	 * Save the current datast to a stored file
	 * 
	 * @param theFile the file where data should be saved
	 */
	public void saveData(File theFile) {
			
		FileOutputStream f = null;
		try {
			f = new FileOutputStream(theFile);
		} catch (Exception e) {
			System.out.println("Could not open file stream: " + e.toString());
		}

		if (f == null) {
			return;
		}
		
		CSVPrinter thePrinter = new CSVPrinter(f);

		thePrinter.printlnComment("");
		thePrinter.printlnComment("File: " + theFile.getName());
		thePrinter.printlnComment("");
		thePrinter.println();
		thePrinter.println(this.getDoubleStrings());

		thePrinter.println();

	}
	
	/**
	 * Find repeated points and perturb them slightly so they don't overlap
	 */
	public void perturbOverlappingPoints(double factor) {
    
		double distance;
		boolean repeat;
		  for (int i = 0; i < numPoints; i++) {
			  repeat = false; // look for repeated points by computing distance to previous points
			  for (int j = i + 1; j < numPoints ; j++) { 
				  distance = getDistance(i,j);				
				  if ((distance == 0) || (Double.isNaN(distance))) {
				  	  //System.out.println("i,j " + i + "  " + j);
					  repeat = true;
					  continue;
				  }
			  }
			  // if point is repeated assume a random perturbation will fix it
			  if (repeat == false) {
				  continue;
			  } else { 			 
				//System.out.println("Perturbing");
			  	for (int k = 0; k < dimensions; k++) { 
					  setComponent(i,k, getComponent(i,k) + (Math.random() - 0.5) * factor);
				  }
			  }
		  }
	}
	
	/**
	 * Print out low dimensional points so maple can plot them
	 * Just does low dimension = 2
	 */
	public void results_to_maple() {
		double[] Y;
		System.out.println("with(plots):");
		System.out.println("points := [");
		for (int i = 0; i < numPoints; i++) {
			Y = (double[]) getPoint(i);
			System.out.println("[" + Y[0] + "," + Y[1] + "],");
		}
		System.out.println("]:");
		System.out.println(
			"plotsetup(ps,plotoutput=`plot.ps`,plotoptions=`portrait,noborder,width=6.0in,height=6.0in`):");
		System.out.println("plot(points, style=POINT,symbol=CIRCLE);");
	}
	
	/**
	 * Get a specificed point in the dataset
	 * 
	 * @param i index of the point to get
	 * @return the n-dimensional datapoint
	 */
	public double[] getPoint(int i) {
		if (i > dataset.size()) {
			System.out.println("Error: requested datapoint outside of dataset range");
			return null;
		}
		return (double[])dataset.get(i);
	}
	
	/**
	 * Set a specified point in the dataset
	 * 
	 * @param i the point to set
	 * @param point the  new n-dimensional point
	 */
	public void setPoint(int i, double[] point) {

		if((i < 0) || (i > dataset.size())) {
			System.out.println("Error: trying to set a datapoint which does not exist");
			return;
		}
		
		dataset.set(i, point);
	}
	
	/**
	 * Get a specific coordinate of a specific datapoint.  Say, the second component
	 * of the third datapoint in a 5-dimensional dataset with 50 points.
	 * 
	 * @param datapoint_number index of the point to get
	 * @param dimension dimension of the desired component
	 * @return the value of of n'th component of the specified datapoint
	 */
	public double getComponent(int datapoint_number, int dimension ) {
	
		//check dimension < dimensions
		
		double[] point = getPoint(datapoint_number);
		return point[dimension];
		
	}
	
	/**
	 * Set a specific coordinate of a specific datapoint.  Say, the second component
	 * of the third datapoint in a 5-dimensional dataset with 50 points.
	 * 
	 * @param datapoint_number index of the point to get
	 * @param dimension dimension of the desired component
	 * @param new_value the new value of the n'th component of the specified datapoint
	 */
	public void setComponent(int datapoint_number, int dimension, double new_value ) {
		
		//check dimension < dimensions
		getPoint(datapoint_number)[dimension] = new_value;
			
	}
		
	/**
	 * Add a new datapoint to the dataset
	 *
	 * @param row A point in the high dimensional space
	 * @param tolerance forwarded to isUniquePoint; if -1 then add point regardless of whether it is unique or not
	 * @return true if point added, false otherwise
	 */
	public boolean addPoint(double[] row, double tolerance) {
	
		if(row.length != dimensions) {
					System.out.println("Error: Dataset is " +
						dimensions + "dimensional, added data is " + row.length + " dimensional");
					return false;
		}
		
		if (isUniquePoint(row, tolerance) == true) {
			dataset.add(row);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Add datapoint without checking whether it is unique  or not
	 * @param row point to be added
	 */
	public void addPoint(double[] row) {
		if(row.length != dimensions) {
					System.out.println("Error: Dataset is " +
						dimensions + "dimensional, added data is " + row.length + " dimensional");
		}
		dataset.add(row);
	}
	
	/**
	 * Check that a given point is "new", that is, that it is not already in the dataset. 
	 * 
	 * @param point the point to check
	 * @param tolerance distance within which a point is considered old, and outside of which it is considered new
	 * @return true if the point is new, false otherwise
	 */
	public boolean isUniquePoint(double[] point, double tolerance) {		
		
		for (int i = 0; i < numPoints; i++) {
			//System.out.println("Distance = " + getClosestDistance(point));
			if (getClosestDistance(point) < tolerance) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Returns the point closest to a given point
	 * 
	 * @param point the point to check
	 * @return the distance between this point and the closest other point in the dataset
	 */
	public double getClosestDistance(double[] point) {
		double dist = Double.MAX_VALUE;
		for (int i = 0; i < numPoints; i++) {
			double temp = getDistance(point, getPoint(i));
			if (temp < dist) {
				dist = 	temp;
			}
		}
		return dist;
	}
	
	/**
	 * Returns the index of the closest point
	 * 
	 * @param point the point to check
	 * @return the index of the point closest to this one in the dataset
	 */
	public int getClosestIndex(double[] point) {
		double dist = Double.MAX_VALUE;
		int ret = 0;
		for (int i = 0; i < numPoints; i++) {
			double temp = getDistance(point, getPoint(i));
			if (temp < dist) {
				dist = 	temp;
				ret = i;
			}
		}
		return ret;
	}

	/**
	 * Returns the k'th nearest neighbor.
	 * 
	 * @param k which nearest neighbor (first, second, etc.) to find
	 * @param point the point whose neighbors are to be found
	 * @return index of nearest neighbor
	 */
	public int getKthNearestNeighbor(int k, double[] point) {
		
		//k-= 1;
		
		if (k > numPoints) {
			System.out.println("ERROR: Non-existent datapoint requested");
			return -1;
		}
		
		boolean past_closest[] = new boolean[numPoints];
		double distances[] = new double[numPoints];
		ArrayList ret = new ArrayList();
		
		//Make an array of neighbors and populate distances
		for (int i = 0; i < numPoints; i++) {
			distances[i] = getDistance(getPoint(i), point);
			past_closest[i] = false;
		}
		
		// Find k-th nearest neighbor
		for (int i = 0; i <= k; i++ ) {	
			
			double min = Double.MAX_VALUE;
			int closest = 0; 
			
			for(int j = 0; j < numPoints; j++) {
				if (past_closest[j] == true) {
					continue;
				}
				
				if (distances[j] < min) {
					min = distances[j];
					closest = j;
				}
			}
			
			past_closest[closest] = true;
			ret.add(new Integer(closest));
		}
		
		return ((Integer)ret.get(k)).intValue();
	}
	

	/**
	 * Get the distance between two points
	 * 
	 * @param index_1 index of point 1
	 * @param index_2 index of point 2
	 * @return distance between points 1 and 2
	 */
	public double getDistance(int index_1, int index_2) {
		if ((index_1 > numPoints) || (index_2 > numPoints)) {
			System.out.println("Dataset.getDistance(): index out of bounds");
			return 0;
		}
		return distances[index_1][index_2];
		
	}
	
	/**
	 * Returns tyhe euclidean distance between two points
	 * 
	 * @param point1
	 * @param point2
	 * @return the Euclidean distance between points 1 and 2
	 */
	public double getDistance(double[] point1, double[] point2) {
		
		if(point1.length != point2.length) {
			System.out.println("Points of different dimensions are being compared");
			return 0;
		}
		
		double sum = 0;
		
		for (int i = 0; i < point1.length; i++) {
			sum += Math.pow(point1[i] - point2[i], 2);
		}
		
		return Math.sqrt(sum);
		
	}
	
	/**
	 * @return the dimensionality of the points in the dataset
	 */
	public int getDimensions() {
		return dimensions;
	}

	/**
	 * Returns a matrix of interpoint distances, between the points in the dataset.
	 * Note that the lower triangular duplicates the upper triangular
	 * 
	 * @return a matrix of interpoint distances
	 */
	public double[][] getDistances() {
		return distances;
	}

	/**
	 * @return the number of points in the dataset
	 */
	public int getNumPoints() {
		return numPoints;
	}

	/**
	 * @return the sum of the distances between points in the dataset
	 */
	public double getSumDistances() {
		
		if (distances == null) {
			System.out.println("In getSumDistances() for " + dimensions + "-d");

			calculateDistances();
		}
		double sum = 0;
		for (int i = 0; i < numPoints; i++ ) {
			for (int j = i + 1; j < numPoints; j++) {
				sum += distances[i][j];	
			}
		}
		return sum;
	}

	/**
	 * Returns the mean of the dataset on a given dimension
	 * 
	 * @param d index of the dimension whose mean to get
	 * @return mean of dataset on dimension d
	 */
	public double getMean(int d) {
		
		double sum = 0;
		for (int i = 0; i < numPoints; i++ ) {
			sum += getComponent(i, d);
		}
				
		return sum / numPoints;
	}


	/**
	 * Returns the covariance of the ith component of the dataset with respect to the jth component
	 * 
	 * @param i first dimension
	 * @param j seconnd dimesion
	 * @return covariance of i with respect to j
	 */
	public double getCovariance(int i, int j) {
		
		double sum = 0;
		double mean_i, mean_j;
		for (int index = 0; index < numPoints; index++ ) {
			mean_i = getMean(i);
			mean_j = getMean(j);
			sum += ((getComponent(index, i) - mean_i) * (getComponent(index, j) - mean_j));
		}
			
		return sum / (numPoints);
	} 
	
	/**
	 * Returns a covariance matrix for the dataset
	 * 
	 * @return covariance matrix which describes how the data covary along each dimension
	 */
	public Matrix getCovarianceMatrix() {

		Matrix m = new Matrix(dimensions, dimensions);
				
		for (int i = 0; i < dimensions; i++) {
			for (int j = i; j < dimensions; j++) {
				m.set(i,j,getCovariance(i,j));								  
				if (i != j) {
					m.set(j,i, m.get(i,j)); // This is a symmetric matrix
				}	
			}
		}
		
		return m;
	}
	
	/**
	 * Returns the k'th most variant dimesion.  For example, the most variant dimension (k=1), or the
	 * least variant dimension (k=num_dimensions)
	 * @param k
	 * @return the k'th most variant dimension
	 */
	public int getKthVariantDimension(int k) {
		
		k-= 1;
		
		if (k > dimensions) {
			System.out.println("ERROR: Non-existent dimension requested");
			return -1;
		}
		
		boolean past_greatest[] = new boolean[dimensions];
		double variances[] = new double[dimensions];
		ArrayList ret = new ArrayList();
		
		//Make an array of variances and populate booles
		for (int i = 0; i < dimensions; i++) {
			Double var = new Double(getCovariance(i,i));
			//System.out.println("[" + i + "]=" + var);
			
			variances[i] = var.doubleValue();
			past_greatest[i] = false;
		}
		
		// Find k-th maximium variance
		for (int i = 0; i <= k; i++ ) {	
			
			double max = 0;
			int greatest = 0; 
			
			for(int j = 0; j < dimensions; j++) {
				if (past_greatest[j] == true) {
					continue;
				}
				
				if (variances[j] > max) {
					max = variances[j];
					greatest = j;
				}
			}
			past_greatest[greatest] = true;
			ret.add(new Integer(greatest));
		}
		
		return ((Integer)ret.get(k)).intValue();
	}
	
	/**
	 * @return a reference to the dataset
	 */
	public ArrayList getDataset() {
		return dataset;
	}

	/**
	 * @param list the dataset
	 */
	public void setDataset(ArrayList list) {
		dataset = list;
	}

	/**
	 * Print out all points in the dataset 
	 * Useful for debugging
	 */
	public void printDataset() {
		double[] tempPoint;
		for (int i = 0; i < dataset.size(); i++) {
			System.out.println("\n[" + i + "]");
			tempPoint = (double[]) getPoint(i);
			for (int j = 0; j < tempPoint.length; j++) {
				System.out.print(" " + tempPoint[j]);
			}
		}
		System.out.println(" "); // add a carriage return
	}

	/**
	 * Returns a matrix of strings, one row for each datapoint, representing the dataset.
	 * 
	 * @return a matrix of strings representing the dataset
	 */
	public String[][] getDoubleStrings() {
		String[][] ret = new String[numPoints][dimensions];
		double[] tempPoint = new double[dimensions];
		for (int i = 0; i < dataset.size(); i++) {
			tempPoint = (double[]) getPoint(i);
			for (int j = 0; j < tempPoint.length; j++) {
				ret[i][j] = Double.toString(tempPoint[j]);
			}
		}
		
		return ret;
	}
	
	/**
	 * Returns a matrix of double, one row for each datapoint, representing the dataset.
	 * 
	 * @return a matrix of double representing the dataset
	 */
	public double[][] getDoubles() {
		double[][] ret = new double[numPoints][dimensions];
		double[] tempPoint = new double[dimensions];
		for (int i = 0; i < dataset.size(); i++) {
			tempPoint = (double[]) getPoint(i);
			for (int j = 0; j < tempPoint.length; j++) {
				ret[i][j] = tempPoint[j];
			}
		}
		
		return ret;
	}
}



