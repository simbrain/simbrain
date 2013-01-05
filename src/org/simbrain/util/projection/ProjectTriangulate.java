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
 * <B>ProjectTriangulate</B> takes each new point and determines which two
 * points in the current data set are closest to it. Then, if possible, it will
 * place the projected image of the new point so that its distance from the
 * projected image of its two nearest neighbors is the same as it was in the
 * high dimensional space. When it is not possible to project the point such
 * that its distance to its two nearest neighbors is preserved, then the
 * projected image of the new point will be placed on a line connecting the
 * projected image of its two nearest neighbors. In this case the position of
 * the projected image of the new point on this line is determined by the
 * relative sizes of the distances between the new point and its two nearest
 * neighbors in the current data set.
 */
public class ProjectTriangulate extends ProjectionMethod {

    /**
     * Default PCA project.
     */
    public ProjectTriangulate(Projector projector) {
        super(projector);
    }

    @Override
    public void init() {
    }

    @Override
    public void project() {
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

        DataPoint lastAdded2D = projector.getDownstairs().getLastAddedPoint();
        DataPoint lastAddedUpstairs = projector.getUpstairs()
                .getLastAddedPoint();

        switch (numPoints) { // assumes numPoints is a positive integer
        case 0:
            lastAdded2D.setData(new double[] { 0, 0 });
            return;
        case 1:
            System.out.println("Only one point upstairs");
            point1Index = projector.getUpstairs().getKNearestNeighbors(1,
                    lastAddedUpstairs)[0];
            point1Up = projector.getUpstairs().getPoint(point1Index);
            point1Down = projector.getUpstairs().getPoint(point1Index);

            dist = projector.getUpstairs().getDistance(point1Up,
                    lastAddedUpstairs);
            x = point1Down.get(0) + dist;
            y = point1Down.get(1);

            lastAdded2D.setData(new double[] { x, y });
            return;
        case 2:
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
        }
    }

}
