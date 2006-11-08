/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world.visionworld;

import java.awt.Image;

/**
 * Pixel matrix.
 */
public interface PixelMatrix {

    /**
     * Return the height of this pixel matrix.
     *
     * @return the height of this pixel matrix
     */
    double getHeight();

    /**
     * Return the width of this pixel matrix.
     *
     * @return the width of this pixel matrix
     */
    double getWidth();

    /**
     * Return an image for this pixel matrix, scaled to dimensions
     * <code>getWidth() x getHeight()</code> if necessary.  The image will
     * not be null.
     *
     * @return an image for this pixel matrix
     */
    Image getImage();


    // todo:  what does a sensor need?

    /**
     * Sample this pixel matrix, returning an image for the specified receptive field.
     *
     * @param receptiveField receptive field at which to sample this pixel matrix, must not be null
     * @return an image for the specified receptive field
     */
    //Image sample(ReceptiveField receptiveField);
}
