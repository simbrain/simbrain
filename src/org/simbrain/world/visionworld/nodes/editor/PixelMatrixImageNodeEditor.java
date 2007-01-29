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
package org.simbrain.world.visionworld.nodes.editor;

import java.awt.Component;

import org.simbrain.world.visionworld.nodes.PixelMatrixImageNode;

/**
 * Pixel matrix image node editor.
 */
public interface PixelMatrixImageNodeEditor {

    /**
     * Return the editor component for this pixel matrix image node editor.
     * The editor component will not be null.
     *
     * @return the editor component for this pixel matrix image node editor
     */
    Component getEditorComponent();

    /**
     * Set the pixel matrix image node to edit to <code>pixelMatrixImageNode</code>.
     *
     * @param pixelMatrixImageNode pixel matrix image node to edit, must not be null
     */
    void setPixelMatrixImageNode(final PixelMatrixImageNode pixelMatrixImageNode);
}