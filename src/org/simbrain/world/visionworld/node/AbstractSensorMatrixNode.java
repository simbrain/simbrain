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
package org.simbrain.world.visionworld.node;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import edu.umd.cs.piccolo.PNode;

import edu.umd.cs.piccolo.util.PPaintContext;

import edu.umd.cs.piccolox.util.PFixedWidthStroke;

import org.apache.commons.lang.SystemUtils;

import org.simbrain.util.StrokeUtils;

import org.simbrain.world.visionworld.SensorMatrix;

/**
 * Abstract sensor matrix node.
 */
abstract class AbstractSensorMatrixNode
    extends PNode {

    /** Sensor matrix. */
    private final SensorMatrix sensorMatrix;

    /** Default grid paint. */
    private static final Paint DEFAULT_GRID_PAINT = Color.BLACK;

    /** Default grid stroke. */
    private static final Stroke DEFAULT_GRID_STROKE = SystemUtils.IS_OS_MAC_OSX ? new BasicStroke(0.5f) : new PFixedWidthStroke(0.5f);

    /** Default outline paint. */
    private static final Paint DEFAULT_OUTLINE_PAINT = Color.BLACK;

    /** Default outline stroke. */
    private static final Stroke DEFAULT_OUTLINE_STROKE = SystemUtils.IS_OS_MAC_OSX ? new BasicStroke(0.5f) : new PFixedWidthStroke(0.5f);

    /** Grid paint. */
    private Paint gridPaint = DEFAULT_GRID_PAINT;

    /** Grid stroke. */
    private Stroke gridStroke = DEFAULT_GRID_STROKE;

    /** Outline paint. */
    private Paint outlinePaint = DEFAULT_OUTLINE_PAINT;

    /** Outline stroke. */
    private Stroke outlineStroke = DEFAULT_OUTLINE_STROKE;


    /**
     * Create a new abstract sensor matrix node with the specified sensor matrix.
     *
     * @param sensorMatrix sensor matrix, must not be null
     */
    protected AbstractSensorMatrixNode(final SensorMatrix sensorMatrix) {
        super();
        if (sensorMatrix == null) {
            throw new IllegalArgumentException("sensorMatrix must not be null");
        }
        this.sensorMatrix = sensorMatrix;
        setWidth(sensorMatrix.columns() * sensorMatrix.getReceptiveFieldWidth());
        setHeight(sensorMatrix.rows() * sensorMatrix.getReceptiveFieldHeight());
    }


    /**
     * Return the sensor matrix for this sensor matrix node.
     * The sensor matrix will not be null.
     *
     * @return the sensor matrix for this sensor matrix node
     */
    public final SensorMatrix getSensorMatrix() {
        return sensorMatrix;
    }

    // todo:  add context menu, tooltip text
    // todo:  add action methods, add sensor, remove sensor, edit properties, etc.

    /**
     * Return the grid paint for this sensor matrix node.
     *
     * @return the grid paint for this sensor matrix node
     */
    public final Paint getGridPaint() {
        return gridPaint;
    }

    /**
     * Set the grid paint for this sensor matrix node to <code>gridPaint</code>.
     *
     * @param gridPaint grid paint for this sensor matrix node
     */
    public final void setGridPaint(final Paint gridPaint) {
        Paint oldGridPaint = this.gridPaint;
        this.gridPaint = gridPaint;
        firePropertyChange("gridPaint", oldGridPaint, this.gridPaint);
    }

    /**
     * Return the grid stroke for this sensor matrix node.
     * The grid stroke will not be null.
     *
     * @return the grid stroke for this sensor matrix node
     */
    public final Stroke getGridStroke() {
        return gridStroke;
    }

    /**
     * Set the grid stroke for this sensor matrix node to <code>gridStroke</code>.
     *
     * @param gridStroke grid stroke for this sensor matrix node, must not be null
     */
    public final void setGridStroke(final Stroke gridStroke) {
        if (gridStroke == null) {
            throw new IllegalArgumentException("gridStroke must not be null");
        }
        Stroke oldGridStroke = this.gridStroke;
        this.gridStroke = gridStroke;
        firePropertyChange("gridStroke", oldGridStroke, this.gridStroke);
    }

    /**
     * Return the outline paint for this sensor matrix node.
     *
     * @return the outline paint for this sensor matrix node
     */
    public final Paint getOutlinePaint() {
        return outlinePaint;
    }

    /**
     * Set the outline paint for this sensor matrix node to <code>outlinePaint</code>.
     *
     * @param outlinePaint outline paint for this sensor matrix node
     */
    public final void setOutlinePaint(final Paint outlinePaint) {
        Paint oldOutlinePaint = this.outlinePaint;
        this.outlinePaint = outlinePaint;
        firePropertyChange("outlinePaint", oldOutlinePaint, this.outlinePaint);
    }

    /**
     * Return the outline stroke for this sensor matrix node.
     * The outline stroke will not be null.
     *
     * @return the outline stroke for this sensor matrix node
     */
    public final Stroke getOutlineStroke() {
        return outlineStroke;
    }

    /**
     * Set the outline stroke for this sensor matrix node to <code>outlineStroke</code>.
     *
     * @param outlineStroke outline stroke for this sensor matrix node, must not be null
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

        if (getPaint() != null) {
            g.setPaint(getPaint());
            g.fill(rect);
        }

        if (gridPaint != null) {
            g.setPaint(gridPaint);
            g.setStroke(StrokeUtils.prepareStroke(gridStroke, paintContext));

            double h = rect.getHeight() / sensorMatrix.rows();
            double w = rect.getWidth() / sensorMatrix.columns();

            for (double x = rect.getX(); x < rect.getWidth(); x += w) {
                g.draw(new Line2D.Double(x, rect.getY(), x, rect.getY() + rect.getHeight()));
            }
            for (double y = rect.getY(); y < rect.getHeight(); y += h) {
                g.draw(new Line2D.Double(rect.getX(), y, rect.getX() + rect.getWidth(), y));
            }
        }

        if (outlinePaint != null) {
            g.setPaint(outlinePaint);
            g.setStroke(StrokeUtils.prepareStroke(outlineStroke, paintContext));
            g.draw(rect);
        }
    }
}
