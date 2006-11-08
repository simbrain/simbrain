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
package org.simbrain.world.visionworld.nodes;

import org.simbrain.world.visionworld.PixelMatrix;

/**
 * Editable pixel matrix node.
 */
public final class EditablePixelMatrixNode
    extends AbstractPixelMatrixNode {

    /**
     * Create a new editable pixel matrix node with the specified pixel matrix.
     *
     * @param pixelMatrix pixel matrix for this editable pixel matrix node, must not be null
     */
    public EditablePixelMatrixNode(final PixelMatrix pixelMatrix) {
        super(pixelMatrix);
    }
}
