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

/**
 * Receptive field.
 */
public final class ReceptiveField {

    /** X offset. */
    private final int x;

    /** Y offset. */
    private final int y;

    /** Width. */
    private final int width;

    /** Height. */
    private final int height;


    /**
     * Create a new receptive field.
     *
     * @param x x offset for this receptive field, relative to a pixel matrix
     * @param y y offset for this receptive field, relative to a pixel matrix
     * @param width width of this receptive field
     * @param height height of this receptive field
     */
    // todo:  can this ctr be kept package private?
    public ReceptiveField(final int x, final int y, final int width, final int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }


    /**
     * Return the x offset for this receptive field.
     *
     * @return the x offset for this receptive field
     */
    public int getX() {
        return x;
    }

    /**
     * Return the y offset for this receptive field.
     *
     * @return the y offset for this receptive field
     */
    public int getY() {
        return y;
    }

    /**
     * Return the width of this receptive field.
     *
     * @return the width of this receptive field
     */
    public int getWidth() {
        return width;
    }

    /**
     * Return the height of this receptive field.
     *
     * @return the height of this receptive field
     */
    public int getHeight() {
        return height;
    }

    /**
     * Return the center x coordinate for this receptive field.
     *
     * @return the center x coordinate for this receptive field
     */
    public double getCenterX() {
        return x + (width / 2.0d);
    }

    /**
     * Return the center y coordinate for this receptive field.
     *
     * @return the center y coordinate for this receptive field
     */
    public double getCenterY() {
        return y + (height / 2.0d);
    }
}
