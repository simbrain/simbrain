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

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PComponent;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.PRoot;

import edu.umd.cs.piccolo.nodes.PImage;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PDragSequenceEventHandler;

import edu.umd.cs.piccolo.util.PPaintContext;

import org.simbrain.world.visionworld.EditablePixelMatrix;

/**
 * Editable pixel matrix image node.
 */
public final class EditablePixelMatrixImageNode
    extends AbstractPixelMatrixNode {

    /** Image node. */
    private final PImage imageNode;

    /** Default pen foreground color. */
    private static final Color DEFAULT_PEN_FOREGROUND = Color.BLACK;

    /** Default pen background color. */
    private static final Color DEFAULT_PEN_BACKGROUND = new Color(0, 0, 0, 0);

    /** Pen foreground color. */
    private Color penForeground = DEFAULT_PEN_FOREGROUND;

    /** Pen background color. */
    private Color penBackground = DEFAULT_PEN_BACKGROUND;

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
     * Return the pen foregroundcolor for this editable pixel matrix image node.
     *
     * @return the pen foreground color for this editable pixel matrix image node
     */
    public Color getPenForeground() {
        return penForeground;
    }

    /**
     * Set the pen foreground color for this editable pixel matrix image node to <code>penForeground</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param penForeground pen foreground color for this editable pixel matrix image node, must not be null
     */
    public void setPenForeground(final Color penForeground) {
        if (penForeground == null) {
            throw new IllegalArgumentException("penForeground must not be null");
        }
        Color oldPenForeground = this.penForeground;
        this.penForeground = penForeground;
        firePropertyChange("penForeground", oldPenForeground, this.penForeground);
    }

    /**
     * Return the pen backgroundcolor for this editable pixel matrix image node.
     *
     * @return the pen background color for this editable pixel matrix image node
     */
    public Color getPenBackground() {
        return penBackground;
    }

    /**
     * Set the pen background color for this editable pixel matrix image node to <code>penBackground</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param penBackground pen background color for this editable pixel matrix image node, must not be null
     */
    public void setPenBackground(final Color penBackground) {
        if (penBackground == null) {
            throw new IllegalArgumentException("penBackground must not be null");
        }
        Color oldPenBackground = this.penBackground;
        this.penBackground = penBackground;
        firePropertyChange("penBackground", oldPenBackground, this.penBackground);
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
                // todo:  temporary workaround until animate to center works properly
                PCanvas canvas = findCanvas();
                if (canvas != null) {
                    canvas.removeInputEventListener(canvas.getPanEventHandler());
                }
            } else {
                removeInputEventListener(pen);
                // todo:  temporary workaround until animate to center works properly
                PCanvas canvas = findCanvas();
                if (canvas != null) {
                    canvas.addInputEventListener(canvas.getPanEventHandler());
                }
            }
            firePropertyChange("hasFocus", oldHasFocus, this.hasFocus);
        }
    }

    /**
     * Find the canvas for this node if possible.
     *
     * @return the canvas for this node if possible
     */
    private PCanvas findCanvas() {
        // todo:  temporary workaround until animate to center works properly
        if (isDescendentOfRoot()) {
            // traverse up to the root
            PNode parent = this;
            while (!(parent instanceof PRoot)) {
                parent = parent.getParent();
            }
            // find the camera node
            for (int i = 0, size = parent.getChildrenCount(); i < size; i++) {
                PNode child = parent.getChild(i);
                if (child instanceof PCamera) {
                    PCamera camera = (PCamera) child;
                    PComponent component = camera.getComponent();
                    if (component instanceof PCanvas) {
                        PCanvas canvas = (PCanvas) component;
                        return canvas;
                    }
                    return null;
                }
            }
        }
        return null;
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
        extends PDragSequenceEventHandler {

        /** Temporary drag color, may be null. */
        private Color dragColor;


        /** {@inheritDoc} */
        protected void drag(final PInputEvent event) {
            super.drag(event);
            Point2D position = event.getPositionRelativeTo(EditablePixelMatrixImageNode.this);
            int x = (int) position.getX();
            int y = (int) position.getY();
            EditablePixelMatrix editablePixelMatrix = (EditablePixelMatrix) getPixelMatrix();
            Color oldColor = editablePixelMatrix.getPixel(x, y);            
            if (!oldColor.equals(dragColor)) {
                editablePixelMatrix.setPixel(x, y, dragColor);
                refreshImage();
            }
        }

        /** {@inheritDoc} */
        protected void endDrag(final PInputEvent event) {
            super.endDrag(event);
            dragColor = null;
        }

        /** {@inheritDoc} */
        public void mouseClicked(final PInputEvent event) {
            Point2D position = event.getPositionRelativeTo(EditablePixelMatrixImageNode.this);
            int x = (int) position.getX();
            int y = (int) position.getY();
            EditablePixelMatrix editablePixelMatrix = (EditablePixelMatrix) getPixelMatrix();
            Color oldColor = editablePixelMatrix.getPixel(x, y);
            if (penForeground.equals(oldColor)) {
                editablePixelMatrix.setPixel(x, y, penBackground);
            }
            else {
                editablePixelMatrix.setPixel(x, y, penForeground);
            }
            refreshImage();
        }

        /** {@inheritDoc} */
        protected void startDrag(final PInputEvent event) {
            super.startDrag(event);
            Point2D position = event.getPositionRelativeTo(EditablePixelMatrixImageNode.this);
            int x = (int) position.getX();
            int y = (int) position.getY();
            EditablePixelMatrix editablePixelMatrix = (EditablePixelMatrix) getPixelMatrix();
            Color oldColor = editablePixelMatrix.getPixel(x, y);
            if (penForeground.equals(oldColor)) {
                dragColor = penBackground;
            }
            else {
                dragColor = penForeground;
            }
        }
    }
}
