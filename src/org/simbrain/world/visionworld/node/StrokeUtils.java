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

import java.awt.BasicStroke;
import java.awt.Stroke;

import edu.umd.cs.piccolo.util.PPaintContext;

import edu.umd.cs.piccolox.util.PFixedWidthStroke;

import org.apache.commons.lang.SystemUtils;

/**
 * Fixed width stroke.
 */
final class StrokeUtils {

    /**
     * Prepare the specified stroke for the specified paint context, returning
     * a new instance of <code>Stroke</code> with a scaled line width if necessary.
     *
     * @param stroke stroke to prepare, must not be null
     * @param paintContext paint context, must not be null
     * @return the specified stroke or a new instance of <code>Stroke</code> with
     *    a scaled line width if necessary
     */
    public static Stroke prepareStroke(final Stroke stroke, final PPaintContext paintContext) {
        if (stroke == null) {
            throw new IllegalArgumentException("stroke must not be null");
        }
        if (paintContext == null) {
            throw new IllegalArgumentException("paintContext must not be null");
        }

        // use the existing stroke on platforms other than MacOSX
        if (!SystemUtils.IS_OS_MAC_OSX) {
            return stroke;
        }

        double scale = paintContext.getScale();

        // create a scaled BasicStroke for PFixedWidthStrokes on MacOSX
        if (stroke instanceof PFixedWidthStroke) {
            PFixedWidthStroke fixedWidthStroke = (PFixedWidthStroke) stroke;
            float lineWidth = (float) (fixedWidthStroke.getLineWidth() / scale);
            int endCap = fixedWidthStroke.getEndCap();
            int lineJoin = fixedWidthStroke.getLineJoin();
            float miterLimit = fixedWidthStroke.getMiterLimit();
            float[] dashArray = fixedWidthStroke.getDashArray();
            float dashPhase = fixedWidthStroke.getDashPhase();
            Stroke scaledStroke = new BasicStroke(lineWidth, endCap, lineJoin, miterLimit, dashArray, dashPhase);
            return scaledStroke;
        }

        if (stroke instanceof BasicStroke) {
            // use the existing stroke on MacOSX at scales <= 1.0d
            if (scale <= 1.0) {
                return stroke;
            }

            // return a new instance of the specified stroke after scaling line width
            BasicStroke basicStroke = (BasicStroke) stroke;
            float lineWidth = (float) (basicStroke.getLineWidth() / scale);
            int endCap = basicStroke.getEndCap();
            int lineJoin = basicStroke.getLineJoin();
            float miterLimit = basicStroke.getMiterLimit();
            float[] dashArray = basicStroke.getDashArray();
            float dashPhase = basicStroke.getDashPhase();
            Stroke scaledStroke = new BasicStroke(lineWidth, endCap, lineJoin, miterLimit, dashArray, dashPhase);
            return scaledStroke;
        }

        // give up, custom strokes aren't supported on Mac OSX
        return stroke;
    }
}
