/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.util.projection;

/**
 * <b>AddData</b> is a set of functions for adding new datapoints to existing
 * datasets, when a projection is being used in real-time. These methods will
 * generally be fast as compared with, for example, re-running the Sammon map
 * each time a new point is added.
 */
public class AddData {
    /**
     * Coordinate project new points.
     *
     * @param i first low-d coordinate for projection
     * @param j second low-d coordinate for projection
     * @param hiPoint new point upstairs
     * @return projected point downstairs
     */
    public static double[] coordinate(final int i, final int j,
            final double[] hiPoint) {
        double[] lowD = { hiPoint[i], hiPoint[j] };

        return lowD;
    }

    /**
     * Adds a new datapoint which preserves distances to nearest neighbors.
     *
     * @param upstairs reference to upstairs dataset
     * @param downstairs reference to downstairs dataset
     * @param hiPoint new hi-d point to be added
     * @return low-d point
     */
    public static double[] triangulate(final Dataset upstairs,
            final Dataset downstairs, final double[] hiPoint) {
        int point1Index;
        int point2Index;
        int point3Index;
        double[] point1Up;
        double[] point2Up;
        double[] point3Up;
        double[] point1Down;
        double[] point2Down;
        double[] point3Down;
        double x;
        double y;
        double dist;
        double d1;
        double d2;

        int numPoints = upstairs.getNumPoints() - 1;

        switch (numPoints) { // assumes numPoints is a positive integer
        case 0: {
            return new double[] { 0, 0 };

        }
        case 1: {
            System.out.println("Only one point upstairs");
            point1Index = upstairs.getKNearestNeighbors(1, hiPoint)[0];
            point1Up = upstairs.getPoint(point1Index);
            point1Down = downstairs.getPoint(point1Index);

            dist = upstairs.getDistance(point1Up, hiPoint);
            x = point1Down[0] + dist;
            y = point1Down[1];

            return new double[] { x, y };

        }
        case 2: {
            System.out.println("Only two points upstairs");
            int[] neighbors = upstairs.getKNearestNeighbors(2, hiPoint);
            point1Index = neighbors[0];
            point2Index = neighbors[1];
            point1Up = upstairs.getPoint(point1Index);
            point2Up = upstairs.getPoint(point2Index);
            point1Down = downstairs.getPoint(point1Index);
            point2Down = downstairs.getPoint(point2Index);

            d1 = upstairs.getDistance(point1Up, hiPoint);
            d2 = upstairs.getDistance(point2Up, hiPoint);

            x = ((d1 * point1Down[0]) + (d2 * point2Down[0])) / (d1 + d2);
            y = ((d1 * point1Down[1]) + (d2 * point2Down[1])) / (d1 + d2);

            return new double[] { x, y };

            // The standard case where are there are at least three upstairs
            // points
        }
        default: {
            int[] neighbors = upstairs.getKNearestNeighbors(2, hiPoint);
            point1Index = neighbors[0];
            point2Index = neighbors[1];
            point3Index = neighbors[2];
            point1Up = upstairs.getPoint(point1Index);
            point2Up = upstairs.getPoint(point2Index);
            point3Up = upstairs.getPoint(point3Index);
            point1Down = downstairs.getPoint(point1Index);
            point2Down = downstairs.getPoint(point2Index);
            point3Down = downstairs.getPoint(point3Index);

            dist = ((point1Down[0] - point2Down[0]) * (point1Down[0] - point2Down[0]))
                    + ((point1Down[1] - point2Down[1]) * (point1Down[1] - point2Down[1]));
            d1 = upstairs.getDistance(point1Up, hiPoint);
            d2 = upstairs.getDistance(point2Up, hiPoint);

            double disc = (dist - ((d2 - d1) * (d2 - d1)))
                    * (((d2 + d1) * (d2 + d1)) - dist);

            if (disc < 0) {
                x = ((d1 * point1Down[0]) + (d2 * point2Down[0])) / (d1 + d2);
                y = ((d1 * point1Down[1]) + (d2 * point2Down[1])) / (d1 + d2);

                return new double[] { x, y };
            } else {
                // Find candidates for intersection points of circles
                double discx = (point1Down[1] - point2Down[1])
                        * (point1Down[1] - point2Down[1]) * disc;
                double discy = (point1Down[0] - point2Down[0])
                        * (point1Down[0] - point2Down[0]) * disc;
                double xfront = ((point1Down[0] + point2Down[0]) * dist)
                        + (((d1 * d1) - (d2 * d2)) * (point2Down[0] - point1Down[0]));
                double yfront = ((point1Down[1] + point2Down[1]) * dist)
                        + (((d1 * d1) - (d2 * d2)) * (point2Down[1] - point1Down[1]));
                double xplus = (xfront + Math.sqrt(discx)) / dist / 2;
                double xminus = (xfront - Math.sqrt(discx)) / dist / 2;
                double yplus = (yfront + Math.sqrt(discy)) / dist / 2;
                double yminus = (yfront - Math.sqrt(discy)) / dist / 2;

                // Find out which of the candidates are the intersection points
                dist = upstairs.getDistance(hiPoint, point3Up);

                if (((point1Down[0] - point2Down[0]) * (point1Down[1] - point2Down[1])) > 0) {
                    // mindful of the sign of the square root
                    d1 = ((xplus - point3Down[0]) * (xplus - point3Down[0]))
                            + ((yminus - point3Down[1]) * (yminus - point3Down[1]));
                    d2 = ((xminus - point3Down[0]) * (xminus - point3Down[0]))
                            + ((yplus - point3Down[1]) * (yplus - point3Down[1]));

                    // check which intersection maintain proper distance from
                    // third point
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
                    d1 = ((xplus - point3Down[0]) * (xplus - point3Down[0]))
                            + ((yplus - point3Down[1]) * (yplus - point3Down[1]));
                    d2 = ((xminus - point3Down[0]) * (xminus - point3Down[0]))
                            + ((yminus - point3Down[1]) * (yminus - point3Down[1]));

                    // check which intersection maintain proper distance from
                    // third point
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
    }

    /**
     * Adds new datapoint to subspace spanned by nearest-neighbors.
     *
     * @param upstairs reference to upstairs dataset
     * @param downstairs reference to downstairs dataset
     * @param hiPoint new hi-d point to be added
     * @return low-d point
     */
    public static double[] nnSubspace(final Dataset upstairs,
            final Dataset downstairs, final double[] hiPoint) {
        int point1Index;
        int point2Index;
        int point3Index;
        double[] point1Up;
        double[] point2Up;
        double[] point3Up;
        double[] point1Down;
        double[] point2Down;
        double[] point3Down;
        double x;
        double y;
        double dist;
        double d1;
        double d2;

        int numPoints = upstairs.getNumPoints() - 1;
        downstairs.perturbOverlappingPoints(.0000001);

        switch (numPoints) { // assumes numPoints is a positive integer
        case 0: {
            return new double[] { 0, 0 };

        }
        case 1: {
            System.out.println("Only one point upstairs");
            point1Index = upstairs.getKNearestNeighbors(1, hiPoint)[0];
            point1Up = upstairs.getPoint(point1Index);
            point1Down = downstairs.getPoint(point1Index);

            dist = upstairs.getDistance(point1Up, hiPoint);
            x = point1Down[0] + dist;
            y = point1Down[1];

            return new double[] { x, y };
        }
        case 2: {
            System.out.println("Only two points upstairs");
            int[] neighbors = upstairs.getKNearestNeighbors(2, hiPoint);
            point1Index = neighbors[0];
            point2Index = neighbors[1];
            point1Up = upstairs.getPoint(point1Index);
            point2Up = upstairs.getPoint(point2Index);
            point1Down = downstairs.getPoint(point1Index);
            point2Down = downstairs.getPoint(point2Index);

            d1 = upstairs.getDistance(point1Up, hiPoint);
            d2 = upstairs.getDistance(point2Up, hiPoint);

            x = ((d1 * point1Down[0]) + (d2 * point2Down[0])) / (d1 + d2);
            y = ((d1 * point1Down[1]) + (d2 * point2Down[1])) / (d1 + d2);

            return new double[] { x, y };

            // The standard case where are there are at least three upstairs
            // points
        }
        default: {
            int[] neighbors = upstairs.getKNearestNeighbors(2, hiPoint);
            point1Index = neighbors[0];
            point2Index = neighbors[1];
            point3Index = neighbors[2];
            point1Up = upstairs.getPoint(point1Index);
            point2Up = upstairs.getPoint(point2Index);
            point3Up = upstairs.getPoint(point3Index);
            point1Down = downstairs.getPoint(point1Index);
            point2Down = downstairs.getPoint(point2Index);
            point3Down = downstairs.getPoint(point3Index);

            // Create an ortho-normal basis for nearest-neighbor subspace
            // upstairs
            double norm1 = 0.0;

            // Create an ortho-normal basis for nearest-neighbor subspace
            // upstairs
            double norm2 = 0.0;

            for (int k = 0; k < point1Up.length; ++k) {
                norm1 += ((point2Up[k] - point1Up[k]) * (point2Up[k] - point1Up[k]));
            }

            norm1 = Math.sqrt(norm1);

            double[] base1Up = new double[point1Up.length];
            double[] base2Up = new double[point1Up.length];
            double[] temp = new double[point1Up.length];
            double tempVal = 0;

            for (int k = 0; k < point1Up.length; ++k) {
                base1Up[k] = (point2Up[k] - point1Up[k]) / norm1;
                temp[k] = point3Up[k] - point1Up[k];
                tempVal += (base1Up[k] * temp[k]);
            }

            for (int k = 0; k < point1Up.length; ++k) {
                base2Up[k] = temp[k] - (tempVal * base1Up[k]);
                norm2 += (base2Up[k] * base2Up[k]);
            }

            norm2 = Math.sqrt(norm2);

            for (int k = 0; k < point1Up.length; ++k) {
                base2Up[k] /= norm2;
            }

            /**
             * Write the projection of the new point onto the nn-subspace in
             * terms of our orthonormal basis for that space.
             */
            double n1 = 0.0;

            double n2 = 0.0;

            for (int k = 0; k < point1Up.length; ++k) {
                n1 += ((hiPoint[k] - point1Up[k]) * base1Up[k]);
                n2 += ((hiPoint[k] - point1Up[k]) * base2Up[k]);
            }

            // Create an ortho-normal basis for nearest-neighbor subspace
            // downstairs
            norm1 = 0.0;
            norm2 = 0.0;

            for (int k = 0; k < point1Down.length; ++k) {
                norm1 += ((point2Down[k] - point1Down[k]) * (point2Down[k] - point1Down[k]));
            }

            norm1 = Math.sqrt(norm1);

            double[] base1Down = new double[point1Down.length];
            double[] base2Down = new double[point1Down.length];
            temp = new double[point1Down.length];
            tempVal = 0;

            for (int k = 0; k < point1Down.length; ++k) {
                base1Down[k] = (point2Down[k] - point1Down[k]) / norm1;
                temp[k] = point3Down[k] - point1Down[k];
                tempVal += (base1Down[k] * temp[k]);
            }

            for (int k = 0; k < point1Down.length; ++k) {
                base2Down[k] = temp[k] - (tempVal * base1Down[k]);
                norm2 += (base2Down[k] * base2Down[k]);
            }

            /**
             * Case where three nearest neighbors are collinear, in which case
             * they don't define a two-dimensional subspace, so we project to a
             * 1-dimensional subspace, the horizontal-axis.
             */
            if (norm2 == 0) {
                x = n1 * base1Down[0];
                y = n1 * base1Down[1];

                return new double[] { x, y };
            }

            norm2 = Math.sqrt(norm2);

            for (int k = 0; k < point1Down.length; ++k) {
                base2Down[k] /= norm2;
            }

            // Create the new point downstairs in terms of the downstairs basis
            x = (n1 * base1Down[0]) + (n2 * base2Down[0]) + point1Down[0];
            y = (n1 * base1Down[1]) + (n2 * base2Down[1]) + point1Down[1];

            return new double[] { x, y };
        }
        }
    }
}
