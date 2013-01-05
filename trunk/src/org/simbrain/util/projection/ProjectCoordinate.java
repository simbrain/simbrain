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
 * <b>Project Coordinate</b> is perhaps the simplest possible projection
 * algorithm; It simply takes two specified dimensions in the high dimensional
 * space, and uses these as the basis for the low-dimensional space. In effect
 * it just takes a 2-dimensional subspace of the high-dimensional space.
 */
public class ProjectCoordinate extends ProjectionMethod {

  /** Coordinate Projection Settings. */
  private int hiD1 = ProjectorPreferences.getHiDim1();

  /** Coordinate Projection Settings. */
  private int hiD2 = ProjectorPreferences.getHiDim2();

  /** Automatically use most variant dimensions. */
  private boolean autoFind = ProjectorPreferences.getAutoFind();

    /**
     * Default projector coordinate constructor.
     */
    public ProjectCoordinate(Projector projector) {
        super(projector);
    }

    @Override
    public void init() {
        //super.init(up, down);

        if ((projector.getUpstairs().getNumPoints() > 1) && (autoFind)) {
            hiD1 = projector.getUpstairs().getKthVariantDimension(1);
            hiD2 = projector.getUpstairs().getKthVariantDimension(2);
        }

        checkCoordinates();
    }

    @Override
    public void project() {
        if (projector.getUpstairs().getNumPoints() < 1) {
            return;
        }

        checkCoordinates();

        for (int i = 0; i < projector.getUpstairs().getNumPoints(); i++) {
            double[] newLowDPoint = {
                    projector.getUpstairs().getComponent(i, hiD1),
                    projector.getUpstairs().getComponent(i, hiD2) };
            projector.getDownstairs().getPoint(i).setData(newLowDPoint);
        }

        // System.out.println("-->" + hi_d1);
        // System.out.println("-->" + hi_d2);
    }

    /**
     * If the current coordinate axes are outside acceptable bounds, set them to
     * acceptable values (currently 0 and 1).
     */
    public void checkCoordinates() {
        if (hiD1 >= projector.getUpstairs().getDimensions()) {
            hiD1 = 0;
        }

        if (hiD2 >= projector.getUpstairs().getDimensions()) {
            hiD2 = 1;
        }
    }


    /**
     * @return the hiD1
     */
    public int getHiD1() {
        return hiD1;
    }

    /**
     * @param hiD1 the hiD1 to set
     */
    public void setHiD1(int hiD1) {
        this.hiD1 = hiD1;
    }

    /**
     * @return the hiD2
     */
    public int getHiD2() {
        return hiD2;
    }

    /**
     * @param hiD2 the hiD2 to set
     */
    public void setHiD2(int hiD2) {
        this.hiD2 = hiD2;
    }

    /**
     * @return the autoFind
     */
    public boolean isAutoFind() {
        return autoFind;
    }

    /**
     * @param autoFind the autoFind to set
     */
    public void setAutoFind(boolean autoFind) {
        this.autoFind = autoFind;
    }
}
