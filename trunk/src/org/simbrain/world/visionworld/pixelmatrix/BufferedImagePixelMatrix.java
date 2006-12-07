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
package org.simbrain.world.visionworld.pixelmatrix;

import java.awt.Image;

import java.awt.image.BufferedImage;

import org.simbrain.world.visionworld.PixelMatrix;
import org.simbrain.world.visionworld.ReceptiveField;

/**
 * BufferedImage pixel matrix.
 */
public final class BufferedImagePixelMatrix
    implements PixelMatrix {

    /** Image for this pixel matrix. */
    private BufferedImage image;


    /**
     * Create a new BufferedImage pixel matrix with the specified image.
     *
     * @param image image for this pixel matrix, must not be null
     */
    public BufferedImagePixelMatrix(final BufferedImage image) {
        super();
        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        this.image = image;
    }


    /** {@inheritDoc} */
    public int getHeight() {
        return image.getHeight();
    }

    /** {@inheritDoc} */
    public int getWidth() {
        return image.getWidth();
    }

    /** {@inheritDoc} */
    public Image getImage() {
        return image;
    }

    /** {@inheridDoc} */
    public Image view(final ReceptiveField receptiveField) {
        if (receptiveField == null) {
            throw new IllegalArgumentException("receptiveField must not be null");
        }
        return image.getSubimage(receptiveField.x, receptiveField.y,
                                 receptiveField.width, receptiveField.height);
    }
}
