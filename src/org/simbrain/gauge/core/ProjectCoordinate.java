/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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
 * <b>Project Coordinate</b> is perhaps the simplest possible projection algorithm; It  simply takes two specificed
 * dimensions in the high dimensional space, and uses these as the basis for the low-dimensional space.  In effect it
 * just takes a 2-dimensional  subspace of the high-dimensional space.
 */
public class ProjectCoordinate extends Projector {
    /**
     * Default projector coordinate constructor.
     */
    public ProjectCoordinate() {
    }

    /**
     * Projector coordinate constructor.
     * @param set Projector setting
     */
    public ProjectCoordinate(final Settings set) {
        theSettings = set;
    }

    /**
     * Initializes the coordinage dataset.
     * @param up Upper data set
     * @param down Lower data set
     */
    public void init(final Dataset up, final Dataset down) {
        super.init(up, down);

        if ((upstairs.getNumPoints() > 1) && (theSettings.isAutoFind())) {
            theSettings.setHiD1(upstairs.getKthVariantDimension(1));
            theSettings.setHiD2(upstairs.getKthVariantDimension(2));
        }

        checkCoordinates();
    }

    /**
     * Project data points.
     */
    public void project() {
        if (upstairs.getNumPoints() < 1) {
            return;
        }

        checkCoordinates();

        for (int i = 0; i < upstairs.getNumPoints(); i++) {
            double[] newLowDPoint = {
                                        upstairs.getComponent(i, theSettings.getHiD1()),
                                        upstairs.getComponent(i, theSettings.getHiD2())
                                    };
            downstairs.setPoint(i, newLowDPoint);
        }

        //System.out.println("-->" + hi_d1);
        //System.out.println("-->" + hi_d2);
    }

    /**
     * If the current coordinate axes are outside acceptable bounds, set them to  acceptable values (currently 0 and
     * 1).
     */
    public void checkCoordinates() {
        if (theSettings.getHiD1() >= upstairs.getDimensions()) {
            theSettings.setHiD1(0);
        }

        if (theSettings.getHiD2() >= upstairs.getDimensions()) {
            theSettings.setHiD2(1);
        }
    }

    /**
     * @return Return project is extendable.
     */
    public boolean isExtendable() {
        return true;
    }

    /**
     * @return Returns project is iterable
     */
    public boolean isIterable() {
        return false;
    }

    /**
     * @return default times to iterate.
     */
    public double iterate() {
        return 0;
    }

    /**
     * @return the first coordinate projected onto
     */
    public int getHiD1() {
        return theSettings.getHiD1();
    }

    /**
     * @return the second coordinate projected onto
     */
    public int getHiD2() {
        return theSettings.getHiD2();
    }

    /**
     * @param i the first coordinate to project onto
     */
    public void setHiD1(final int i) {
        checkCoordinates();
        theSettings.setHiD1(i);
    }

    /**
     * @param i the second coordinate to project onto
     */
    public void setHiD2(final int i) {
        checkCoordinates();
        theSettings.setHiD2(i);
    }

    /**
     * In auto-find the projection automatically uses the most variant dimensions.
     *
     * @return true if in auto-find mode, false otherwise
     */
    public boolean isAutoFind() {
        return theSettings.isAutoFind();
    }

    /**
     * In auto-find the projection automatically uses the most variant dimensions.
     *
     * @param b whether to use auto-find mode
     */
    public void setAutoFind(final boolean b) {
        theSettings.setAutoFind(b);
    }

    /**
     * @see Projector
     */
    public boolean hasDialog() {
        return true;
    }
}
