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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pixel matrix editors.
 */
public final class PixelMatrixEditors {

    /** Buffered image pixel matrix editor. */
    public static final PixelMatrixEditor BUFFERED_IMAGE = new BufferedImagePixelMatrixEditor();

    /** Private array of pixel matrix editors. */
    private static final PixelMatrixEditor[] values = new PixelMatrixEditor[] { BUFFERED_IMAGE };

    /** Public list of pixel matrix editors. */
    public static final List<PixelMatrixEditor> VALUES = Collections.unmodifiableList(Arrays.asList(values));
}
