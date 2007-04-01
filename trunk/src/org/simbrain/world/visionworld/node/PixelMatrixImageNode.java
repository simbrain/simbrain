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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

import java.awt.geom.Point2D;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PComponent;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.PRoot;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PDragSequenceEventHandler;

import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;

import org.simbrain.world.visionworld.PixelMatrix;

/**
 * Pixel matrix image node.
 */
public final class PixelMatrixImageNode
    extends AbstractPixelMatrixNode {

    /** Image node. */
    private final NonAntialiasingImageNode imageNode;

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
     * Create a new pixel matrix image node with the
     * specified pixel matrix.
     *
     * @param pixelMatrix pixel matrix, must not be null
     */
    public PixelMatrixImageNode(final PixelMatrix pixelMatrix) {
        super(pixelMatrix);
        imageNode = new NonAntialiasingImageNode(pixelMatrix.getImage());
        addChild(imageNode);
    }


    /**
     * Return the pen foreground color for this pixel matrix image node.
     *
     * @return the pen foreground color for this pixel matrix image node
     */
    public Color getPenForeground() {
        return penForeground;
    }

    /**
     * Set the pen foreground color for this pixel matrix image node to <code>penForeground</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param penForeground pen foreground color for this pixel matrix image node, must not be null
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
     * Return the pen background color for this pixel matrix image node.
     *
     * @return the pen background color for this pixel matrix image node
     */
    public Color getPenBackground() {
        return penBackground;
    }

    /**
     * Set the pen background color for this pixel matrix image node to <code>penBackground</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param penBackground pen background color for this pixel matrix image node, must not be null
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
     * Return true if this pixel matrix image node has focus.
     *
     * @return true if this pixel matrix image node has focus
     */
    public boolean hasFocus() {
        return hasFocus;
    }

    /**
     * Set to true to indicate this pixel matrix image node has focus
     * and should enabled drawing via mouse clicks.
     *
     * @param hasFocus true to indicate this pixel matrix image node has focus
     */
    public void setFocus(final boolean hasFocus) {
        boolean oldHasFocus = this.hasFocus;
        this.hasFocus = hasFocus;
        if (this.hasFocus != oldHasFocus) {
            if (this.hasFocus) {
                addInputEventListener(pen);
            } else {
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
     * Image node that paints images without antialiasing.
     */
    private class NonAntialiasingImageNode
        extends PNode {

        /** Image for this non-antialiasing image node. */
        private Image image;


        /**
         * Create a new non-antialiasing image node for the specifed image.
         *
         * @param image image
         */
        NonAntialiasingImageNode(final Image image) {
            super();
            setImage(image);
        }


        /**
         * Set the image for this non-antialiasing image node to <code>image</code>.
         *
         * @param image image
         */
        public void setImage(final Image image) {
            this.image = image;
            setBounds(0, 0, image.getWidth(null), image.getHeight(null));
            invalidatePaint();
        }

        /** {@inheritDoc} */
        protected void paint(final PPaintContext paintContext) {
            double iw = image.getWidth(null);
            double ih = image.getHeight(null);
            PBounds b = getBoundsReference();
            Graphics2D g = paintContext.getGraphics();

            // explicitly prevent antialiasing
            Object oldAntialiasingHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            Object oldInterpolationHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            if (b.x != 0 || b.y != 0 || b.width != iw || b.height != ih) {
                g.translate(b.x, b.y);
                g.scale(b.width / iw, b.height / ih);
                g.drawImage(image, 0, 0, null);
                g.scale(iw / b.width, ih / b.height);
                g.translate(-b.x, -b.y);
            }
            else {
                g.drawImage(image, 0, 0, null);
            }

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasingHint);
            if (oldInterpolationHint != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolationHint);
            }
            else {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            }
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
            Point2D position = event.getPositionRelativeTo(PixelMatrixImageNode.this);
            // border conditions are off-by-one
            int x = Math.min(getPixelMatrix().getWidth() - 1, (int) position.getX());
            int y = Math.min(getPixelMatrix().getHeight() - 1, (int) position.getY());
            Color oldColor = getPixelMatrix().getPixel(x, y);
            if (!oldColor.equals(dragColor)) {
                getPixelMatrix().setPixel(x, y, dragColor);
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
            Point2D position = event.getPositionRelativeTo(PixelMatrixImageNode.this);
            // border conditions are off-by-one
            int x = Math.min(getPixelMatrix().getWidth() - 1, (int) position.getX());
            int y = Math.min(getPixelMatrix().getHeight() - 1, (int) position.getY());
            Color oldColor = getPixelMatrix().getPixel(x, y);
            if (penForeground.equals(oldColor)) {
                getPixelMatrix().setPixel(x, y, penBackground);
            }
            else {
                getPixelMatrix().setPixel(x, y, penForeground);
            }
            refreshImage();
        }

        /** {@inheritDoc} */
        protected void startDrag(final PInputEvent event) {
            super.startDrag(event);
            Point2D position = event.getPositionRelativeTo(PixelMatrixImageNode.this);
            // border conditions are off-by-one
            int x = Math.min(getPixelMatrix().getWidth() - 1, (int) position.getX());
            int y = Math.min(getPixelMatrix().getHeight() - 1, (int) position.getY());
            Color oldColor = getPixelMatrix().getPixel(x, y);
            if (penForeground.equals(oldColor)) {
                dragColor = penBackground;
            }
            else {
                dragColor = penForeground;
            }
        }
    }
}
