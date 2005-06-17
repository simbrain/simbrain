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

import java.util.ArrayList;

/**
 * <B>ProjectSammon.java</B> Implements gradient descent to compute image of Sammon projection.
 */
public class ProjectSammon extends Projector {
	
	ArrayList Y;
	
	double[] X_i, X_j, Y_i, Y_j, Y_m, Y_n, Y_new;	// temporary variables	
							
	double[][] dstar;		// matrix of "upstairs" interpoint distances
	double[][] d;			// matrix of "downstairs" interpoint distances
	double dstarSum,PartSum, currentCloseness, E;		

	int lowDimension, numPoints, highDimension;


	public ProjectSammon() {
	}
	
	public ProjectSammon(Settings set) {
		theSettings = set;
	}
	
	/**
	 * Perform necessary initialization
	 */
	public void init(Dataset up, Dataset down) {
		
		super.init(up, down);
		
		lowDimension = downstairs.getDimensions();
		numPoints = upstairs.getNumPoints();
		highDimension = upstairs.getDimensions();
		upstairs.calculateDistances();
		dstar = upstairs.getDistances();
		dstarSum = upstairs.getSumDistances();
		downstairs.perturbOverlappingPoints(theSettings.getPerturbationAmount());
	}


	/**
	 * Iterate the Sammon algorithm and return currentCloseness
	 */
	public double iterate() {
		
		if (upstairs.getNumPoints() < 2) {
			return 0;
		}
		
		//Question: Why do I need the new below?  Why can't I use refs for Y_m and Y_i?
		Y = new ArrayList(downstairs.getDataset());
		downstairs.calculateDistances();
		d = downstairs.getDistances();
		// Computes partials
		for (int m = 0; m < numPoints; m++) {
			Y_m = new double[lowDimension];
			Y_m = (double[])Y.get(m);
			Y_new = new double[lowDimension];
			for (int n = 0; n < lowDimension; n++) {
				PartSum = 0.0;
				for (int i = 0; i < numPoints; i++) {
					if (i == m)
						continue;
					Y_i = new double[lowDimension];
					Y_i = (double[]) Y.get(i);
					PartSum += ((dstar[i][m] - d[i][m]) * (Y_i[n] - Y_m[n])	/ dstar[i][m] / d[i][m]);
				}
				Y_new[n] = Y_m[n] - theSettings.getEpsilon()* 2 * PartSum / dstarSum;
			}
			downstairs.setPoint(m, Y_new);
		
		}
		// Computes Closeness
		E = 0.0;
		for (int i = 0; i < numPoints; i++) {
			for (int j = i + 1; j < numPoints; j++) {
				E += (dstar[i][j] - d[i][j]) * (dstar[i][j] - d[i][j]) / dstar[i][j];
			}
		}

		currentCloseness = E / dstarSum;

		//System.out.println("currentCloseness = " + currentCloseness); 
		return currentCloseness;
	}
	
	public boolean isIterable() { return true;}
	
	public boolean isExtendible() {return false;}
	
	public void project() {}
	

	/**
	 * @return step size for Sammon map
	 */
	public double getEpsilon() {
		return theSettings.getEpsilon();
	}

	/**
	 * @param d step size for Sammon map
	 */
	public void setEpsilon(double d) {
		theSettings.setEpsilon(d);
	}

}
