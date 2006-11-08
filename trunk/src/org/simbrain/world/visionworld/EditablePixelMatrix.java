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

import java.awt.Color;

/**
 * Editable pixel matrix.
 */
public abstract class EditablePixelMatrix
    implements PixelMatrix {

    /**
     * Throw an ArrayIndexOutOfBoundsException if either of the specified
     * coordinates are outside the bounds of this pixel matrix.
     *
     * @param x x coordinate to check
     * @param y y coordinate to check
     * @throws ArrayIndexOutOfBoundsException if either of the specified coordinates
     *    are outside the bounds of this pixel matrix
     */
    protected final void checkCoordinates(final int x, final int y) {
        if (x < 0) {
            throw new ArrayIndexOutOfBoundsException("x must be greater than 0, was " + x);
        }
        if (y < 0) {
            throw new ArrayIndexOutOfBoundsException("y must be greater than 0, was " + y);
        }
        if (x > (int) (getWidth() - 1)) {
            throw new ArrayIndexOutOfBoundsException("x must be less than or equal to (getWidth() - 1), was " + x);
        }
        if (y > (int) (getHeight() - 1)) {
            throw new ArrayIndexOutOfBoundsException("y must be less than or equal to (getHeight() - 1), was " + y);
        }
    }

    /**
     * Return the color of the pixel at the specified (x, y) coordinates.
     *
     * @param x x coordinate of the pixel
     * @param y y coordinate of the pixel
     * @return the color of the pixel at the specified (x, y) coordinates
     * @throws ArrayIndexOutOfBoundsException if either of the specified coordinates
     *    are outside the bounds of this pixel matrix
     */
    public abstract Color getPixel(int x, int y);

    /**
     * Set the pixel at the specified (x, y) coordinates to the RGB and alpha values
     * in the specified color.
     *
     * @param x x coordinate of the pixel to set
     * @param y y coordinate of the pixel to set
     * @param color color value, must not be null
     * @throws ArrayIndexOutOfBoundsException if either of the specified coordinates
     *    are outside the bounds of this pixel matrix
     */
    public abstract void setPixel(int x, int y, Color color);
}
