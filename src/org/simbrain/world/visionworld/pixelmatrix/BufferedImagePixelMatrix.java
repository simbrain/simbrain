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
import java.awt.Graphics2D;

import java.awt.geom.AffineTransform;

import java.awt.image.BufferedImage;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.simbrain.world.visionworld.PixelMatrix;
import org.simbrain.world.visionworld.ReceptiveField;

/**
 * BufferedImage pixel matrix.
 */
public final class BufferedImagePixelMatrix
    implements PixelMatrix {

    /** Image for this pixel matrix. */
    private BufferedImage image;

    /** Property change support. */
    private final PropertyChangeSupport propertyChangeSupport;

    /** Default height. */
    public static final int DEFAULT_HEIGHT = 100;

    /** Default width. */
    public static final int DEFAULT_WIDTH = 100;


    /**
     * Create a new BufferedImage pixel matrix with an empty image
     * the default dimensions.
     *
     * @see #DEFAULT_HEIGHT
     * @see #DEFAULT_WIDTH
     */
    public BufferedImagePixelMatrix() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Create a new BufferedImage pixel matrix with an empty image
     * the specified dimensions.
     *
     * @param width width in pixels, must be &gt; 0
     * @param height height in pixels, must be &gt; 0
     */
    public BufferedImagePixelMatrix(final int width, final int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("height must be greater than zero");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("width must be greater than zero");
        }
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.propertyChangeSupport = new PropertyChangeSupport(this);
    }

    /**
     * Create a new BufferedImage pixel matrix with the specified image.
     *
     * @param image image for this pixel matrix, must not be null
     */
    public BufferedImagePixelMatrix(final BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        this.image = makeIndexedRGBImage(image);
        this.propertyChangeSupport = new PropertyChangeSupport(this);
    }


    /**
     * Throw an ArrayIndexOutOfBoundsException if either of the specified
     * coordinates are outside the bounds of this pixel matrix.
     *
     * @param x x coordinate to check
     * @param y y coordinate to check
     * @throws ArrayIndexOutOfBoundsException if either of the specified coordinates
     *    are outside the bounds of this pixel matrix
     */
    private void checkCoordinates(final int x, final int y) {
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

    /**
     * Set the image for this pixel matrix to <code>image</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param image image for this pixel matrix, must not be null
     */
    public void setImage(final BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        BufferedImage oldImage = this.image;
        this.image = image;
        propertyChangeSupport.firePropertyChange("image", oldImage, this.image);
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

    /** {@inheritDoc} */
    public Image view(final ReceptiveField receptiveField) {
        if (receptiveField == null) {
            throw new IllegalArgumentException("receptiveField must not be null");
        }
        return image.getSubimage(receptiveField.x, receptiveField.y,
                                 receptiveField.width, receptiveField.height);
    }

    /** {@inheritDoc} */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /** {@inheritDoc} */
    public void addPropertyChangeListener(final String propertyName,
                                          final PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /** {@inheritDoc} */
    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /** {@inheritDoc} */
    public void removePropertyChangeListener(final String propertyName,
                                             final PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Create and return a new BufferedImage of type <code>BufferedImage.TYPE_INT_ARGB</code>
     * rendered from the specified base image.
     *
     * @param baseImage base image
     * @return a new BufferedImage of type <code>BufferedImage.TYPE_INT_ARGB</code> rendered
     *    from the specified base image
     */
    private static BufferedImage makeIndexedRGBImage(final BufferedImage baseImage) {
        BufferedImage image = new BufferedImage(baseImage.getWidth(),
                                                baseImage.getHeight(),
                                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.drawRenderedImage(baseImage, new AffineTransform());
        g.dispose();
        return image;
    }
}
