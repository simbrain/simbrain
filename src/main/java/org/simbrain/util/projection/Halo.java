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

import org.simbrain.util.Utils;
import org.simbrain.util.math.SimbrainMath;

import java.awt.*;

/**
 * Utility for coloring points of a dataset using a "halo".Outside this radius
 * (in the high dimensional vector space) points are colored gray. Inside they
 * are colored red.
 */
public class Halo {

    // TODO: This should be coordinated with DataColorManager. But
    // this method overwrites all datapoints so I'm not sure the best approach.

    /**
     * Default "radius" of the halo.
     */
    static float DEFAULT_RADIUS = .2f;

    /**
     * Maximum saturation.
     */
    static float maxSaturation = 1f;

    /**
     * Minimum saturation. If set to 0 points saturate to white.
     */
    static float minSaturation = .2f;

    /**
     * Make halo with default halo radius.
     *
     * @param proj   the projector whose points should be colored
     * @param target target value around which the halo is created.
     */
    public static void makeHalo(Projector proj, double[] target) {
        makeHalo(proj, target, DEFAULT_RADIUS);
    }

    /**
     * Color the points in the plot according to how close they are to a
     * provided target value. A "halo" of red is created around the target
     * point.
     * <p>
     * The current point in the dataset is colored green.
     *
     * @param proj   the projector whose points should be colored
     * @param target target value around which the halo is created.
     * @param radius radius of the halo
     */
    public static void makeHalo(Projector proj, double[] target, float radius) {

        // TODO: Shouldn't this use proj.getUpstairs().getKNearestNeighbors()

        for (int i = 0; i < proj.getUpstairs().getNumPoints(); i++) {

            // Color the current point green
            double[] point = proj.getUpstairs().getPoint(i).getVector();
            if (java.util.Arrays.equals(point, proj.getCurrentPoint().getVector())) {
                ((DataPointColored) proj.getUpstairs().getPoint(i)).setColor(Color.green);
                continue;
            }

            // Color the target point red and a halo of saturated red around it,
            // scaled from max to min saturation. Outside of the radius color
            // points gray.
            double distance = SimbrainMath.distance(target, point);
            if (distance < radius) {
                float slope = -(maxSaturation - minSaturation) / radius;
                float saturation = (float) (distance * slope + maxSaturation);
                ((DataPointColored) proj.getUpstairs().getPoint(i)).setColor(Color.getHSBColor(Utils.colorToFloat(Color.red), saturation, 1));
            } else {
                ((DataPointColored) proj.getUpstairs().getPoint(i)).setColor(Color.gray);
            }

        }
    }
}