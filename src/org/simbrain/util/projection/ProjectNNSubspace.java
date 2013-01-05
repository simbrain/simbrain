/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
package org.simbrain.util.projection;

/**
 * <B>ProjectNNSubspace</B> is a Nearest Neighbor Subspace Method.
 *
 * (1) Takes each new point and determines the three points in the current data
 * set that are closest to it.
 *
 * (2) Finds the projection of the new point into the two-dimensional subspace
 * that contains the three nearest neighbors in the high-dimensional space.
 *
 * (3) Uses the three nearest neighbors and their corresponding points in the
 * low dimensional dataset to find an affine map that approximates the full
 * projection method (whichever one is currently being used).
 *
 * (4) Applies the affine map to the new datapoint.
 */
public class ProjectNNSubspace extends ProjectionMethod {

    /**
     * Default PCA project.
     */
    public ProjectNNSubspace(Projector projector) {
        super(projector);
    }

    @Override
    public void init() {
    }

    @Override
    public void project() {

        DataPoint lastAdded2D = projector.getDownstairs().getLastAddedPoint();
        DataPoint lastAddedUpstairs = projector.getUpstairs()
                .getLastAddedPoint();

        int point1Index;
        int point2Index;
        int point3Index;
        DataPoint point1Up;
        DataPoint point2Up;
        DataPoint point3Up;
        DataPoint point1Down;
        DataPoint point2Down;
        DataPoint point3Down;
        double x;
        double y;
        double dist;
        double d1;
        double d2;

        int numPoints = projector.getUpstairs().getNumPoints() - 1;
        projector.getDownstairs().perturbOverlappingPoints(.0000001);

        switch (numPoints) { // assumes numPoints is a positive integer
        case 0: {
            lastAdded2D.setData(new double[] { 0, 0 });
        }
        case 1: {
            System.out.println("Only one point upstairs");
            point1Index = projector.getUpstairs().getKNearestNeighbors(1,
                    lastAddedUpstairs)[0];
            point1Up = projector.getUpstairs().getPoint(point1Index);
            point1Down = projector.getDownstairs().getPoint(point1Index);

            dist = projector.getUpstairs().getDistance(point1Up,
                    lastAddedUpstairs);
            x = point1Down.get(0) + dist;
            y = point1Down.get(1);

            lastAdded2D.setData(new double[] { x, y });
            return;
        }
        case 2: {
            System.out.println("Only two points upstairs");
            int[] neighbors = projector.getUpstairs().getKNearestNeighbors(2,
                    lastAddedUpstairs);
            point1Index = neighbors[0];
            point2Index = neighbors[1];
            point1Up = projector.getUpstairs().getPoint(point1Index);
            point2Up = projector.getUpstairs().getPoint(point2Index);
            point1Down = projector.getDownstairs().getPoint(point1Index);
            point2Down = projector.getDownstairs().getPoint(point2Index);

            d1 = projector.getUpstairs().getDistance(point1Up,
                    lastAddedUpstairs);
            d2 = projector.getUpstairs().getDistance(point2Up,
                    lastAddedUpstairs);

            x = ((d1 * point1Down.get(0)) + (d2 * point2Down.get(0)))
                    / (d1 + d2);
            y = ((d1 * point1Down.get(1)) + (d2 * point2Down.get(1)))
                    / (d1 + d2);

            lastAdded2D.setData(new double[] { x, y });
            return;

            // The standard case where are there are at least three upstairs
            // points
        }
        default: {
            int[] neighbors = projector.getUpstairs().getKNearestNeighbors(3,
                    lastAddedUpstairs); // KNN returning identical points
            point1Index = neighbors[0];
            point2Index = neighbors[1];
            point3Index = neighbors[2];
            point1Up = projector.getUpstairs().getPoint(point1Index);
            point2Up = projector.getUpstairs().getPoint(point2Index);
            point3Up = projector.getUpstairs().getPoint(point3Index);
            point1Down = projector.getDownstairs().getPoint(point1Index);
            point2Down = projector.getDownstairs().getPoint(point2Index);
            point3Down = projector.getDownstairs().getPoint(point3Index);

            // Create an ortho-normal basis for nearest-neighbor subspace
            // upstairs
            double norm1 = 0.0;

            // Create an ortho-normal basis for nearest-neighbor subspace
            // upstairs
            double norm2 = 0.0;

            for (int k = 0; k < point1Up.getDimension(); ++k) {
                norm1 += ((point2Up.get(k) - point1Up.get(k)) * (point2Up
                        .get(k) - point1Up.get(k)));
            }

            norm1 = Math.sqrt(norm1);

            double[] base1Up = new double[point1Up.getDimension()];
            double[] base2Up = new double[point1Up.getDimension()];
            double[] temp = new double[point1Up.getDimension()];
            double tempVal = 0;

            for (int k = 0; k < point1Up.getDimension(); ++k) {
                base1Up[k] = (point2Up.get(k) - point1Up.get(k)) / norm1;
                temp[k] = point3Up.get(k) - point1Up.get(k);
                tempVal += (base1Up[k] * temp[k]);
            }

            for (int k = 0; k < point1Up.getDimension(); ++k) {
                base2Up[k] = temp[k] - (tempVal * base1Up[k]);
                norm2 += (base2Up[k] * base2Up[k]);
            }

            norm2 = Math.sqrt(norm2);

            for (int k = 0; k < point1Up.getDimension(); ++k) {
                base2Up[k] /= norm2;
            }

            /**
             * Write the projection of the new point onto the nn-subspace in
             * terms of our orthonormal basis for that space.
             */
            double n1 = 0.0;

            double n2 = 0.0;

            for (int k = 0; k < point1Up.getDimension(); ++k) {
                n1 += ((lastAddedUpstairs.get(k) - point1Up.get(k)) * base1Up[k]);
                n2 += ((lastAddedUpstairs.get(k) - point1Up.get(k)) * base2Up[k]);
            }

            // Create an ortho-normal basis for nearest-neighbor subspace
            // downstairs
            norm1 = 0.0;
            norm2 = 0.0;

            for (int k = 0; k < point1Down.getDimension(); ++k) {
                norm1 += ((point2Down.get(k) - point1Down.get(k)) * (point2Down
                        .get(k) - point1Down.get(k)));
            }

            norm1 = Math.sqrt(norm1);

            double[] base1Down = new double[point1Down.getDimension()];
            double[] base2Down = new double[point1Down.getDimension()];
            temp = new double[point1Down.getDimension()];
            tempVal = 0;

            for (int k = 0; k < point1Down.getDimension(); ++k) {
                base1Down[k] = (point2Down.get(k) - point1Down.get(k)) / norm1;
                temp[k] = point3Down.get(k) - point1Down.get(k);
                tempVal += (base1Down[k] * temp[k]);
            }

            for (int k = 0; k < point1Down.getDimension(); ++k) {
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

                lastAdded2D.setData(new double[] { x, y });
                return;
            }

            norm2 = Math.sqrt(norm2);

            for (int k = 0; k < point1Down.getDimension(); ++k) {
                base2Down[k] /= norm2;
            }

            // Create the new point downstairs in terms of the downstairs basis
            x = (n1 * base1Down[0]) + (n2 * base2Down[0]) + point1Down.get(0);
            y = (n1 * base1Down[1]) + (n2 * base2Down[1]) + point1Down.get(1);


            lastAdded2D.setData(new double[] { x, y });
        }
        }

    }
}
