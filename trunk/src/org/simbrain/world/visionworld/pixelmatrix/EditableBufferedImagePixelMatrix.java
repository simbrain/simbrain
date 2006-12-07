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

import java.awt.Color;
import java.awt.Image;

import java.awt.image.BufferedImage;

import org.simbrain.world.visionworld.EditablePixelMatrix;
import org.simbrain.world.visionworld.ReceptiveField;

/**
 * Editable BufferedImage pixel matrix.
 */
public final class EditableBufferedImagePixelMatrix
    extends EditablePixelMatrix {

    /** Image for this pixel matrix. */
    private BufferedImage image;


    /**
     * Create a new editable BufferedImage pixel matrix with the specified image.
     *
     * @param width width in pixels
     * @param height height in pixels
     */
    public EditableBufferedImagePixelMatrix(final int width, final int height) {
        super();
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
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

    /** {@inheritDoc} */
    public Color getPixel(final int x, final int y) {
        checkCoordinates(x, y);
        int rgb = image.getRGB(x, y);
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        int[] a = image.getAlphaRaster().getPixel(x, y, new int[1]);
        return new Color(r, g, b, a[0]);
    }

    /** {@inheritDoc} */
    public void setPixel(final int x, final int y, final Color color) {
        checkCoordinates(x, y);
        if (color == null) {
            throw new IllegalArgumentException("color must not be null");
        }
        int rgb = (color.getRed() << 16) | (color.getGreen() << 8) | (color.getBlue());
        int[] a = new int[1];
        a[0] = color.getAlpha();
        image.setRGB(x, y, rgb);
        image.getAlphaRaster().setPixel(x, y, a);
    }
}
