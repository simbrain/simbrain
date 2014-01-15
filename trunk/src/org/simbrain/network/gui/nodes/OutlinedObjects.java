/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
package org.simbrain.network.gui.nodes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;

import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.util.PBounds;
import org.piccolo2d.util.PPaintContext;

/**
 * A node that draws an outline around its children nodes. To use create an
 * instance and then add children directly to it using addChild().
 *
 * Adapted from an example in the piccolo 1.3 source by Lance Good.
 *
 * @author Lance Good
 * @author Jeff Yoshimi
 */
public class OutlinedObjects extends PPath.Float {

    /**
     * Whether to draw an outline around entire set of grouped objects or not.
     * In some cases a fill is enough.
     */
    private boolean drawOutline = true;

    /** What outline color to use. */
    private Color lineColor = Color.gray;

    /** Whether to fill the background around grouped objects or not. */
    private boolean fillBackground = true;

    /** What background color to use. */
    private Color backgroundColor = Color.white;

    /** Indentation amount around drawn outline. */
    private int outlinePadding = 10;

    /** Cache of children bounds. For use in validating bounds, */
    private PBounds cachedChildBounds = new PBounds();

    /**
     * Cache of bound to compare with cached bounds. For use in validating
     * bounds.
     */
    private PBounds comparisonBounds = new PBounds();

    /**
     * Construct the outlined objects group.
     */
    public OutlinedObjects() {
        super();
        // This seems to be needed to initialize the paint system properly
        this.setPaint(Color.gray);
    }

    /**
     * Change the default paint to fill an expanded bounding box based on its
     * children's bounds.
     */
    public void paint(final PPaintContext ppc) {
        final Paint paint = getPaint();
        if (paint != null) {
            final Graphics2D g2 = ppc.getGraphics();
            final PBounds bounds = getUnionOfChildrenBounds(null);

            if (fillBackground) {
                g2.setPaint(backgroundColor);
                g2.fillRect((int) bounds.getX() - outlinePadding,
                        (int) bounds.getY() - outlinePadding,
                        (int) bounds.getWidth() + 2 * outlinePadding,
                        (int) bounds.getHeight() + 2 * outlinePadding);
            }

            if (drawOutline) {
                g2.setPaint(lineColor);
                g2.drawRect((int) bounds.getX() - outlinePadding,
                        (int) bounds.getY() - outlinePadding,
                        (int) bounds.getWidth() + 2 * outlinePadding,
                        (int) bounds.getHeight() + 2 * outlinePadding);
            }
            // if (backgroundColor != null) {
            // g2.setPaint(backgroundColor);
            // g2.fillRect((int) bounds.getX() - updatePadding,
            // (int) bounds.getY() - updatePadding,
            // (int) bounds.getWidth() + 2 * updatePadding,
            // (int) bounds.getHeight() + 2 * updatePadding);
            // }
        }
    }

    /**
     * Change the full bounds computation to take into account that we are
     * expanding the children's bounds Do this instead of overriding
     * getBoundsReference() since the node is not volatile.
     */
    @Override
    public PBounds computeFullBounds(final PBounds dstBounds) {
        final PBounds result = getUnionOfChildrenBounds(dstBounds);

        cachedChildBounds.setRect(result);
        result.setRect(result.getX() - outlinePadding, result.getY()
                - outlinePadding, result.getWidth() + 2 * outlinePadding,
                result.getHeight() + 2 * outlinePadding);
        localToParent(result);
        return result;
    }

    /**
     * This is a crucial step. We have to override this method to invalidate the
     * paint each time the bounds are changed so we repaint the correct region
     */
    @Override
    public boolean validateFullBounds() {
        comparisonBounds = getUnionOfChildrenBounds(comparisonBounds);

        if (!cachedChildBounds.equals(comparisonBounds)) {
            setPaintInvalid(true);
        }
        return super.validateFullBounds();
    }

    /**
     * @return the drawOutline
     */
    public boolean isDrawOutline() {
        return drawOutline;
    }

    /**
     * @param drawOutline the drawOutline to set
     */
    public void setDrawOutline(boolean drawOutline) {
        this.drawOutline = drawOutline;
    }

    /**
     * @return the backgroundColor
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * @param backgroundColor the backgroundColor to set
     */
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /**
     * @return the outlinePadding
     */
    public int getOutlinePadding() {
        return outlinePadding;
    }

    /**
     * @param outlinePadding the outlinePadding to set
     */
    public void setOutlinePadding(int outlinePadding) {
        this.outlinePadding = outlinePadding;
    }

    /**
     * @return the fillBackground
     */
    public boolean isFillBackground() {
        return fillBackground;
    }

    /**
     * @param fillBackground the fillBackground to set
     */
    public void setFillBackground(boolean fillBackground) {
        this.fillBackground = fillBackground;
    }

    /**
     * @return the lineColor
     */
    public Color getLineColor() {
        return lineColor;
    }

    /**
     * @param lineColor the lineColor to set
     */
    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
    }

}
