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
import java.awt.Image;

import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable implementation of VisionWorldModel.
 */
public final class MutableVisionWorldModel
    extends AbstractVisionWorldModel {

    /** Pixel matrix. */
    private PixelMatrix pixelMatrix;

    /** List of sensor matrices. */
    private final List<SensorMatrix> sensorMatrices;

    /** Empty pixel matrix. */
    private static final PixelMatrix EMPTY_PIXEL_MATRIX = new EmptyPixelMatrix();


    /**
     * Create a new mutable vision world model.
     */
    public MutableVisionWorldModel() {
        super();
        this.pixelMatrix = EMPTY_PIXEL_MATRIX;
        this.sensorMatrices = new ArrayList<SensorMatrix>();
    }


    /** {@inheritDoc} */
    public PixelMatrix getPixelMatrix() {
        return pixelMatrix;
    }

    /** {@inheritDoc} */
    public void setPixelMatrix(final PixelMatrix pixelMatrix) {
        if (pixelMatrix == null) {
            throw new IllegalArgumentException("pixelMatrix must not be null");
        }
        PixelMatrix oldPixelMatrix = this.pixelMatrix;
        this.pixelMatrix = pixelMatrix;
        if (!oldPixelMatrix.equals(this.pixelMatrix)) {
            firePixelMatrixChanged(oldPixelMatrix, this.pixelMatrix);
        }
    }

    /** {@inheritDoc} */
    public int getSensorMatrixCount() {
        return sensorMatrices.size();
    }

    /** {@inheritDoc} */
    public void addSensorMatrix(final SensorMatrix sensorMatrix) {
        if (sensorMatrix == null) {
            throw new IllegalArgumentException("sensorMatrix must not be null");
        }
        if (sensorMatrices.add(sensorMatrix)) {
            fireSensorMatrixAdded(sensorMatrix);
        }
    }

    /** {@inheritDoc} */
    public void removeSensorMatrix(final SensorMatrix sensorMatrix) {
        if (sensorMatrix == null) {
            throw new IllegalArgumentException("sensorMatrix must not be null");
        }
        if (sensorMatrices.remove(sensorMatrix)) {
            fireSensorMatrixRemoved(sensorMatrix);
        }
    }

    /** {@inheritDoc} */
    public List<SensorMatrix> getSensorMatrices() {
        return Collections.unmodifiableList(sensorMatrices);
    }

    /**
     * Empty pixel matrix.
     */
    private static class EmptyPixelMatrix
        implements PixelMatrix {

        /** Empty image. */
        private final Image emptyImage = new BufferedImage(0, 0, BufferedImage.TYPE_INT_ARGB);


        /** {@inheritDoc} */
        public int getWidth() {
            return 0;
        }

        /** {@inheritDoc} */
        public int getHeight() {
            return 0;
        }

        /** {@inheritDoc} */
        public Image getImage() {
            return emptyImage;
        }

        /** {@inheritDoc} */
        public Color getPixel(final int x, final int y) {
            return null;
        }

        /** {@inheritDoc} */
        public void setPixel(final int x, final int y, final Color color) {
            throw new UnsupportedOperationException("setPixel operation not supported by this pixel matrix");
        }

        /** {@inheritDoc} */
        public Image view(final ReceptiveField receptiveField) {
            if (receptiveField == null) {
                throw new IllegalArgumentException("receptiveField must not be null");
            }
            return emptyImage;
        }
    }
}
