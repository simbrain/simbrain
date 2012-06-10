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
package org.simbrain.world.visionworld.filter.editor;

import java.awt.Component;

import javax.swing.JPanel;

import org.simbrain.world.visionworld.Filter;
import org.simbrain.world.visionworld.filter.PixelAccumulator;

/**
 * Pixel accumulator editor.
 */
public final class PixelAccumulatorEditor extends JPanel implements
        FilterEditor {

    /** Display name. */
    private static final String DISPLAY_NAME = "Pixel accumulator";

    /** Description. */
    private static final String DESCRIPTION = "Returns the number of black pixels";

    /**
     * Create a new pixel accumulator editor.
     */
    public PixelAccumulatorEditor() {
        super();
        setToolTipText(DESCRIPTION);
    }

    /** {@inheritDoc} */
    public Component getEditorComponent() {
        return this;
    }

    /** {@inheritDoc} */
    public Filter createFilter() throws FilterEditorException {
        return new PixelAccumulator();
    }

    /** {@inheritDoc} */
    public String toString() {
        return DISPLAY_NAME;
    }
}
