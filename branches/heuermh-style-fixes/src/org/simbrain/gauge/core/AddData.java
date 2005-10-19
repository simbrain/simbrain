/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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

/**
 * <b>AddData</b> is a set of functions for adding new datapoints to existing datasets, when a projection is being used in real-time.
 * These methods will generally be fast as compared with, for example, re-running the Sammon map each time a new point is added.
 */
public class AddData {

	/**
	 * Coordinate project new points
	 * 
	 * @param i first low-d coordinate for projection
	 * @param j second low-d coordinate for projection
	 * @param hi_point new point upstairs
	 * @return projected point downstairs
	 */
	public static double[] coordinate(int i, int j, double[] hi_point) {
		double[] low_d = { hi_point[i], hi_point[j] };
		return low_d;
	}

	/**
	 * Adds a new datapoint which preserves distances to nearest neighbors 
	 * 
	 * @param upstairs reference to upstairs dataset
	 * @param downstairs reference to downstairs dataset
	 * @param hi_point new hi-d point to be added
	 * @return low-d point
	 */
	public static double[] triangulate(Dataset upstairs, Dataset downstairs, double[] hi_point) {

		int point1_index, point2_index, point3_index;
		double[] point1_up, point2_up, point3_up;
		double[] point1_down, point2_down, point3_down;
		double x, y, dist, d1, d2;

		int numPoints = upstairs.getNumPoints() - 1;

		switch (numPoints) { // assumes numPoints is a positive integer
			
			
			case 0:
				return new double[] {0, 0};
			case 1 :
				System.out.println("Only one point upstairs");
				point1_index = upstairs.getKthNearestNeighbor(1, hi_point);
				point1_up = upstairs.getPoint(point1_index);
				point1_down = downstairs.getPoint(point1_index);

				dist = upstairs.getDistance(point1_up, hi_point);
				x = point1_down[0] + dist;
				y = point1_down[1];
				return new double[] { x, y };

			case 2 :
				System.out.println("Only two points upstairs");
				point1_index = upstairs.getKthNearestNeighbor(1, hi_point);
				point2_index = upstairs.getKthNearestNeighbor(2, hi_point);
				point1_up = upstairs.getPoint(point1_index);
				point2_up = upstairs.getPoint(point2_index);
				point1_down = downstairs.getPoint(point1_index);
				point2_down = downstairs.getPoint(point2_index);

				d1 = upstairs.getDistance(point1_up, hi_point);
				d2 = upstairs.getDistance(point2_up, hi_point);

				x = (d1 * point1_down[0] + d2 * point2_down[0]) / (d1 + d2);
				y = (d1 * point1_down[1] + d2 * point2_down[1]) / (d1 + d2);

				return new double[] { x, y };

			// The standard case where are there are at least three upstairs points
			default :
				point1_index = upstairs.getKthNearestNeighbor(1, hi_point);
				point2_index = upstairs.getKthNearestNeighbor(2, hi_point);
				point3_index = upstairs.getKthNearestNeighbor(3, hi_point);
				point1_up = upstairs.getPoint(point1_index);
				point2_up = upstairs.getPoint(point2_index);
				point3_up = upstairs.getPoint(point3_index);
				point1_down = downstairs.getPoint(point1_index);
				point2_down = downstairs.getPoint(point2_index);
				point3_down = downstairs.getPoint(point3_index);

				dist = (point1_down[0] - point2_down[0])* (point1_down[0] - point2_down[0])
						+ (point1_down[1] - point2_down[1]) * (point1_down[1] - point2_down[1]);
				d1 = upstairs.getDistance(point1_up, hi_point);
				d2 = upstairs.getDistance(point2_up, hi_point);
				double disc = (dist - (d2 - d1) * (d2 - d1)) * ((d2 + d1) * (d2 + d1) - dist);

				if (disc < 0) {
					x = (d1 * point1_down[0] + d2 * point2_down[0]) / (d1 + d2);
					y = (d1 * point1_down[1] + d2 * point2_down[1]) / (d1 + d2);

					return new double[] { x, y };
					
				} else {
					// Find candidates for intersection points of circles
					double discx = (point1_down[1] - point2_down[1]) * (point1_down[1] - point2_down[1]) * disc;
					double discy = (point1_down[0] - point2_down[0]) * (point1_down[0] - point2_down[0]) * disc;
					double xfront = (point1_down[0] + point2_down[0]) * dist + (d1 * d1 - d2 * d2) * (point2_down[0] - point1_down[0]);
					double yfront = (point1_down[1] + point2_down[1]) * dist + (d1 * d1 - d2 * d2) * (point2_down[1] - point1_down[1]);					
					double xplus = (xfront + Math.sqrt(discx)) / dist / 2;
					double xminus = (xfront - Math.sqrt(discx)) / dist / 2;
					double yplus = (yfront + Math.sqrt(discy)) / dist / 2;
					double yminus = (yfront - Math.sqrt(discy)) / dist / 2;
					
					//Find out which of the candidates are the intersection points
					dist = upstairs.getDistance(hi_point, point3_up);
					if ((point1_down[0] - point2_down[0]) * (point1_down[1] - point2_down[1]) > 0) { 
						// mindful of the sign of the square root
						d1 = (xplus - point3_down[0]) * (xplus - point3_down[0]) + (yminus - point3_down[1])
									* (yminus - point3_down[1]);
						d2 = (xminus - point3_down[0]) * (xminus - point3_down[0]) + (yplus - point3_down[1])
									* (yplus - point3_down[1]);
						// check which intersection maintain proper distance from third point
						if (Math.abs(dist - d1) < Math.abs(dist - d2)) {
							x = xplus;
							y = yminus;
							return new double[] { x, y };
						} else {
							x = xminus;
							y = yplus;
							return new double[] { x, y };
						}
					} else {

						d1 = (xplus - point3_down[0]) * (xplus - point3_down[0]) + (yplus - point3_down[1])
									* (yplus - point3_down[1]);
						d2 = (xminus - point3_down[0]) * (xminus - point3_down[0]) + (yminus - point3_down[1])
									* (yminus - point3_down[1]);
						//check which intersection maintain proper distance from third point
						if (Math.abs(dist - d1) < Math.abs(dist - d2)) {
							x = xplus;
							y = yplus;
							return new double[] { x, y };
						} else {
							x = xminus;
							y = yminus;
							return new double[] { x, y };
						}
					}
				}
		}
	}

	/**
	 * Adds new datapoint to subspace spanned by nearest-neighbors
	 * 
	 * @param upstairs reference to upstairs dataset
	 * @param downstairs reference to downstairs dataset
	 * @param hi_point new hi-d point to be added
	 * @return low-d point
	 */
	public static double[] nn_subspace(Dataset upstairs, Dataset downstairs, double[] hi_point) {

		int point1_index, point2_index, point3_index;
		double[] point1_up, point2_up, point3_up;
		double[] point1_down, point2_down, point3_down;
		double x, y, dist, d1, d2;

		int numPoints = upstairs.getNumPoints() - 1;
		downstairs.perturbOverlappingPoints(.0000001);

		switch (numPoints) { // assumes numPoints is a positive integer
			
			case 0:
				return new double[] {0, 0};
			case 1 :
				System.out.println("Only one point upstairs");
				point1_index = upstairs.getKthNearestNeighbor(1, hi_point);
				point1_up = upstairs.getPoint(point1_index);
				point1_down = downstairs.getPoint(point1_index);

				dist = upstairs.getDistance(point1_up, hi_point);
				x = point1_down[0] + dist;
				y = point1_down[1];
				return new double[] { x, y };

			case 2 :
				System.out.println("Only two points upstairs");
				point1_index = upstairs.getKthNearestNeighbor(1, hi_point);
				point2_index = upstairs.getKthNearestNeighbor(2, hi_point);
				point1_up = upstairs.getPoint(point1_index);
				point2_up = upstairs.getPoint(point2_index);
				point1_down = downstairs.getPoint(point1_index);
				point2_down = downstairs.getPoint(point2_index);

				d1 = upstairs.getDistance(point1_up, hi_point);
				d2 = upstairs.getDistance(point2_up, hi_point);

				x = (d1 * point1_down[0] + d2 * point2_down[0]) / (d1 + d2);
				y = (d1 * point1_down[1] + d2 * point2_down[1]) / (d1 + d2);

				return new double[] { x, y };

				// The standard case where are there are at least three upstairs points
			default :
				point1_index = upstairs.getKthNearestNeighbor(1, hi_point);
				point2_index = upstairs.getKthNearestNeighbor(2, hi_point);
				point3_index = upstairs.getKthNearestNeighbor(3, hi_point);
				point1_up = upstairs.getPoint(point1_index);
				point2_up = upstairs.getPoint(point2_index);
				point3_up = upstairs.getPoint(point3_index);
				point1_down = downstairs.getPoint(point1_index);
				point2_down = downstairs.getPoint(point2_index);
				point3_down = downstairs.getPoint(point3_index);

				// Create an ortho-normal basis for nearest-neighbor subspace upstairs
				double norm1 = 0.0, norm2 = 0.0;
				for (int k = 0; k < point1_up.length; ++k) {
					norm1 += (point2_up[k] - point1_up[k]) * (point2_up[k] - point1_up[k]);
				}
				norm1 = Math.sqrt(norm1);
				double[] base1_up = new double[point1_up.length];
				double[] base2_up = new double[point1_up.length];
				double[] temp = new double[point1_up.length];
				double temp_val = 0;
				for (int k = 0; k < point1_up.length; ++k) {
					base1_up[k] = (point2_up[k] - point1_up[k]) / norm1;
					temp[k] = point3_up[k] - point1_up[k];
					temp_val += base1_up[k] * temp[k];
				}
				for (int k = 0; k < point1_up.length; ++k) {
					base2_up[k] = temp[k] - temp_val * base1_up[k];
					norm2 += base2_up[k] * base2_up[k];
				}
				norm2 = Math.sqrt(norm2);

				for (int k = 0; k < point1_up.length; ++k) {
					base2_up[k] /= norm2;
				}

			
				//Write the projection of the new point onto the nn-subspace in terms of our orthonormal basis for that space
				double n1 = 0.0, n2 = 0.0;
				for (int k = 0; k < point1_up.length; ++k) {
					n1 += (hi_point[k] - point1_up[k]) * base1_up[k];
					n2 += (hi_point[k] - point1_up[k]) * base2_up[k];
				}
				
				// Create an ortho-normal basis for nearest-neighbor subspace downstairs
				norm1 = 0.0; norm2 = 0.0;
				for (int k = 0; k < point1_down.length; ++k) {
					norm1 += (point2_down[k] - point1_down[k]) * (point2_down[k] - point1_down[k]);
				}
				norm1 = Math.sqrt(norm1);
	
				double[] base1_down = new double[point1_down.length];
				double[] base2_down = new double[point1_down.length];
				temp = new double[point1_down.length];
				temp_val = 0;
				for (int k = 0; k < point1_down.length; ++k) {
					base1_down[k] = (point2_down[k] - point1_down[k]) / norm1;
					temp[k] = point3_down[k] - point1_down[k];
					temp_val += base1_down[k] * temp[k];
				}
				for (int k = 0; k < point1_down.length; ++k) {
					base2_down[k] = temp[k] - temp_val * base1_down[k];
					norm2 += base2_down[k] * base2_down[k];
				}
				
				//Case where three nearest neighbors are collinear,
				// in which case they don't define a two-dimensional subspace, so we 
				// project to a 1-dimensional subspace, the horizontal-axis.
				if(norm2 == 0) {
					x = n1 * base1_down[0];
					y = n1 * base1_down[1];
					return new double[] { x, y };
				}
				
				norm2 = Math.sqrt(norm2);
	
				for (int k = 0; k < point1_down.length; ++k) {
					base2_down[k] /= norm2;
				}

				//Create the new point downstairs in terms of the downstairs basis
				x = n1 * base1_down[0] + n2 * base2_down[0] + point1_down[0];
				y = n1 * base1_down[1] + n2 * base2_down[1] + point1_down[1];

				return new double[] { x, y };

		}
	}
}

