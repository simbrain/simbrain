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
package org.simbrain.world.visionworld.pixelmatrix.editor;

import java.awt.Component;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import javax.swing.JPanel;

import org.simbrain.world.visionworld.PixelMatrix;

import org.simbrain.world.visionworld.pixelmatrix.BufferedImagePixelMatrix;

/**
 * Buffered image pixel matrix editor.
 */
public final class BufferedImagePixelMatrixEditor
    extends JPanel
    implements PixelMatrixEditor {

    /** Image file. */
    private File imageFile;


    /** {@inheritDoc} */
    public Component getEditorComponent() {
        return this;
    }

    /** {@inheritDoc} */
    public PixelMatrix createPixelMatrix() throws PixelMatrixEditorException {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            return new BufferedImagePixelMatrix(image);
        }
        catch (IOException e) {
            throw new PixelMatrixEditorException(e);
        }
    }
}
