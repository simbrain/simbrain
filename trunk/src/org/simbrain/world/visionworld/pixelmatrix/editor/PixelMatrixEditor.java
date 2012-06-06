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

import org.simbrain.world.visionworld.PixelMatrix;

/**
 * Pixel matrix editor.
 */
public interface PixelMatrixEditor {

    /**
     * Return the editor component for this pixel matrix editor. The editor
     * component will not be null.
     *
     * @return the editor component for this pixel matrix editor
     */
    Component getEditorComponent();

    /**
     * Create a new instance of PixelMatrix from the properties of this pixel
     * matrix editor. The pixel matrix will not be null.
     *
     * @return a new instance of PixelMatrix created from the properties of this
     *         pixel matrix editor
     * @throws PixelMatrixEditorException if a PixelMatrix cannot properly be
     *             created from the properties of this pixel matrix editor
     */
    PixelMatrix createPixelMatrix() throws PixelMatrixEditorException;
}
