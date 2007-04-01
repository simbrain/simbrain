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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;

import java.awt.geom.Rectangle2D;

import edu.umd.cs.piccolo.PNode;

import edu.umd.cs.piccolo.util.PPaintContext;

import edu.umd.cs.piccolox.util.PFixedWidthStroke;

import org.apache.commons.lang.SystemUtils;

import org.simbrain.world.visionworld.PixelMatrix;

/**
 * Abstract pixel matrix node.
 */
abstract class AbstractPixelMatrixNode
    extends PNode {

    /** Pixel matrix. */
    private final PixelMatrix pixelMatrix;

    /** Default outline paint. */
    private static final Paint DEFAULT_OUTLINE_PAINT = Color.BLACK;

    /** Default outline stroke. */
    private static final Stroke DEFAULT_OUTLINE_STROKE = SystemUtils.IS_OS_MAC_OSX ? new BasicStroke(0.5f) : new PFixedWidthStroke(0.5f);

    /** Outline paint. */
    private Paint outlinePaint = DEFAULT_OUTLINE_PAINT;

    /** Outline stroke. */
    private Stroke outlineStroke = DEFAULT_OUTLINE_STROKE;


    /**
     * Create a new abstract pixel matrix node with the specified pixel matrix.
     *
     * @param pixelMatrix pixel matrix, must not be null
     */
    protected AbstractPixelMatrixNode(final PixelMatrix pixelMatrix) {
        super();
        if (pixelMatrix == null) {
            throw new IllegalArgumentException("pixelMatrix must not be null");
        }
        this.pixelMatrix = pixelMatrix;
        setHeight(this.pixelMatrix.getHeight());
        setWidth(this.pixelMatrix.getWidth());
    }


    /**
     * Return the pixel matrix for this pixel matrix node.
     * The pixel matrix will not be null.
     *
     * @return the pixel matrix for this pixel matrix node
     */
    public final PixelMatrix getPixelMatrix() {
        return pixelMatrix;
    }


    /**
     * Return the outline paint for this pixel matrix node.
     *
     * @return the outline paint for this pixel matrix node
     */
    public final Paint getOutlinePaint() {
        return outlinePaint;
    }

    /**
     * Set the outline paint for this pixel matrix node to <code>outlinePaint</code>.
     *
     * @param outlinePaint outline paint for this pixel matrix node
     */
    public final void setOutlinePaint(final Paint outlinePaint) {
        Paint oldOutlinePaint = this.outlinePaint;
        this.outlinePaint = outlinePaint;
        firePropertyChange("outlinePaint", oldOutlinePaint, this.outlinePaint);
    }

    /**
     * Return the outline stroke for this pixel matrix node.
     * The outline stroke will not be null.
     *
     * @return the outline stroke for this pixel matrix node
     */
    public final Stroke getOutlineStroke() {
        return outlineStroke;
    }

    /**
     * Set the outline stroke for this pixel matrix node to <code>outlineStroke</code>.
     *
     * @param outlineStroke outline stroke for this pixel matrix node, must not be null
     */
    public final void setOutlineStroke(final Stroke outlineStroke) {
        if (outlineStroke == null) {
            throw new IllegalArgumentException("outlineStroke must not be null");
        }
        Stroke oldOutlineStroke = this.outlineStroke;
        this.outlineStroke = outlineStroke;
        firePropertyChange("outlineStroke", oldOutlineStroke, this.outlineStroke);
    }

    /** {@inheritDoc} */
    protected final void paint(final PPaintContext paintContext) {

        Graphics2D g = paintContext.getGraphics();
        Rectangle2D rect = getBounds();

        if (outlinePaint != null) {
            g.setPaint(outlinePaint);
            g.setStroke(StrokeUtils.prepareStroke(outlineStroke, paintContext));
            g.draw(rect);
        }
    }
}
