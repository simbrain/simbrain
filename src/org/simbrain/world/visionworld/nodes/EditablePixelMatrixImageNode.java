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

import java.awt.Color;
import java.awt.Image;

import java.awt.geom.Point2D;

import edu.umd.cs.piccolo.nodes.PImage;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;

import edu.umd.cs.piccolo.util.PPaintContext;

import org.simbrain.world.visionworld.EditablePixelMatrix;

/**
 * Editable pixel matrix image node.
 */
public final class EditablePixelMatrixImageNode
    extends AbstractPixelMatrixNode {

    /** Image node. */
    private final PImage imageNode;

    /** Default pen color. */
    private static final Color DEFAULT_PEN_COLOR = Color.BLACK;

    /** Pen color. */
    private Color penColor = DEFAULT_PEN_COLOR;

    /** True if this node "has focus". */
    private boolean hasFocus = false;

    /** Pen. */
    private final Pen pen = new Pen();


    /**
     * Create a new editable pixel matrix image node with the
     * specified editable pixel matrix.
     *
     * @param pixelMatrix editable pixel matrix, must not be null
     */
    public EditablePixelMatrixImageNode(final EditablePixelMatrix pixelMatrix) {
        super(pixelMatrix);
        imageNode = new PImageWrapper(pixelMatrix.getImage());
        addChild(imageNode);
    }


    /**
     * Return the pen color for this editable pixel matrix image node.
     *
     * @return the pen color for this editable pixel matrix image node
     */
    public Color getPenColor() {
        return penColor;
    }

    /**
     * Set the pen color for this editable pixel matrix image node to <code>penColor</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param penColor pen color for this editable pixel matrix image node, must not be null
     */
    public void setPenColor(final Color penColor) {
        if (penColor == null) {
            throw new IllegalArgumentException("penColor must not be null");
        }
        Color oldPenColor = this.penColor;
        this.penColor = penColor;
        firePropertyChange("penColor", oldPenColor, this.penColor);
    }

    /**
     * Return true if this editable pixel matrix image node has focus.
     *
     * @return true if this editable pixel matrix image node has focus
     */
    public boolean hasFocus() {
        return hasFocus;
    }

    /**
     * Set to true to indicate this editiable pixel matrix image node has focus
     * and should enabled drawing via mouse clicks.
     *
     * @param hasFocus true to indicate this editable pixel matrix image node has focus
     */
    public void setFocus(final boolean hasFocus) {
        boolean oldHasFocus = this.hasFocus;
        this.hasFocus = hasFocus;
        if (this.hasFocus != oldHasFocus) {
            if (this.hasFocus) {
                addInputEventListener(pen);
            }
            else {
                removeInputEventListener(pen);
            }
            firePropertyChange("hasFocus", oldHasFocus, this.hasFocus);
        }
    }

    /**
     * Refresh the image node.
     */
    private void refreshImage() {
        imageNode.setImage(getPixelMatrix().getImage());
    }

    /**
     * Wrapper for PImage.
     */
    private class PImageWrapper
        extends PImage {

        /**
         * Create a new PImage wrapper for the specifed image.
         *
         * @param image image
         */
        PImageWrapper(final Image image) {
            super(image);
        }


        /** {@inheritDoc} */
        protected void paint(final PPaintContext paintContext) {
            int oldRenderQuality = paintContext.getRenderQuality();
            // always paint this "blocky" so that individual pixels are not antialiased
            paintContext.setRenderQuality(PPaintContext.LOW_QUALITY_RENDERING);
            super.paint(paintContext);
            paintContext.setRenderQuality(oldRenderQuality);
        }
    }

    /**
     * Pen.
     */
    private class Pen
        extends PBasicInputEventHandler {

        /** {@inheritDoc} */
        public void mouseClicked(final PInputEvent event) {
            Point2D position = event.getPositionRelativeTo(EditablePixelMatrixImageNode.this);
            int x = (int) position.getX();
            int y = (int) position.getY();
            Color oldColor = ((EditablePixelMatrix) getPixelMatrix()).getPixel(x, y);
            if (!penColor.equals(oldColor)) {
                ((EditablePixelMatrix) getPixelMatrix()).setPixel(x, y, penColor);
                refreshImage();
            }
        }
    }
}