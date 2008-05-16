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

import java.awt.Rectangle;

/**
 * Receptive field.
 */
public final class ReceptiveField {

    private final Rectangle rect;
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
        rect = new Rectangle(x, y, width, height);
    }
    
    public int getY() {
        return rect.y;
    }
    
    public int getX() {
        return rect.x;
    }
    
    public int getWidth() {
        return rect.width;
    }
    
    public int getHeight() {
        return rect.height;
    }
    
    public double getCenterX() {
        return rect.getCenterX();
    }
    
    public double getCenterY() {
        return rect.getCenterY();
    }
}
