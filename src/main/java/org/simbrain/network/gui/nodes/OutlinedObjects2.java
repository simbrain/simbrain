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

import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PShape;
import org.piccolo2d.util.PBounds;
import org.piccolo2d.util.PPaintContext;

import java.awt.*;
import java.util.ArrayList;

/**
 * A version of {@link OutlinedObjects} that does not have {@link NeuronNode}s as children
 * but uses them to determine bounds.
 *
 * TODO: See if this can be merged to OutlinedObjects as a special case or if code can be shared.
 *
 * @author Lance Good
 * @author Jeff Yoshimi
 */
public class OutlinedObjects2 extends PPath.Float {

    /**
     * The width and height of the arc in the rounded rectangle that surrounds the outlined objects.
     */
    public static final int ROUNDING_WIDTH_HEIGHT = 20;

    /**
     * Whether to draw an outline around entire set of grouped objects or not. In some cases a fill is enough.
     */
    private boolean drawOutline = true;

    /**
     * What outline color to use.
     */
    private Color lineColor = Color.gray;

    /**
     * Whether to fill the background around grouped objects or not.
     */
    private boolean fillBackground = true;

    /**
     * What background color to use.
     */
    private Color backgroundColor = Color.white;

    /**
     * Indentation amount around drawn outline.
     */
    private int outlinePadding = 10;

    /**
     * Cache of children bounds. For use in validating bounds,
     */
    private PBounds cachedChildBounds = new PBounds();

    /**
     * Cache of bound to compare with cached bounds. For use in validating bounds.
     */
    private PBounds comparisonBounds = new PBounds();

    ArrayList<NeuronNode> neuronNodeRefs = new ArrayList<>();

    /**
     * Construct the outlined objects group.
     */
    public OutlinedObjects2() {
        super();
        // This seems to be needed to initialize the paint system properly
        this.setVisible(true);
        this.setPaint(Color.gray);
    }

    public void addChildRef(NeuronNode node) {
        neuronNodeRefs.add(node);
    }

    public ArrayList<NeuronNode> getNeuronNodeRefs() {
        return neuronNodeRefs;
    }

    @Override
    public PBounds getFullBounds() {
        return getUnionOfBounds();
    }

    /**
     * Change the default paint to fill an expanded bounding box based on its children's bounds.
     */
    @Override
    public void paint(final PPaintContext ppc) {
        final Paint paint = getPaint();
        if (paint != null) {
            final Graphics2D g2 = ppc.getGraphics();
            final PBounds bounds = getUnionOfBounds();

            if (fillBackground) {
                g2.setPaint(backgroundColor);
                g2.fillRect((int) bounds.getX() - outlinePadding, (int) bounds.getY() - outlinePadding, (int) bounds.getWidth() + 2 * outlinePadding, (int) bounds.getHeight() + 2 * outlinePadding);
            }

            if (drawOutline) {
                g2.setPaint(lineColor);
                g2.drawRoundRect((int) bounds.getX() - outlinePadding, (int) bounds.getY() - outlinePadding, (int) bounds.getWidth() + 2 * outlinePadding, (int) bounds.getHeight() + 2 * outlinePadding, ROUNDING_WIDTH_HEIGHT, ROUNDING_WIDTH_HEIGHT);
            }
        }
    }


    private PBounds getUnionOfBounds() {
        PBounds resultBounds = new PBounds();
        for(PNode node : neuronNodeRefs) {
            resultBounds.add(node.getFullBoundsReference());
        }
        return resultBounds;
    }


    //TODO: Below works with validatefull bounds
    /**
     * Change the full bounds computation to take into account that we are expanding the children's bounds Do this
     * instead of overriding getBoundsReference() since the node is not volatile.
     */
    @Override
    public PBounds computeFullBounds(final PBounds dstBounds) {
        final PBounds result = getUnionOfBounds();

        cachedChildBounds.setRect(result);
        //result.setRect(result.getX() - outlinePadding, result.getY() - outlinePadding,
        //        result.getWidth() + 2 * outlinePadding, result.getHeight() + 2 * outlinePadding);
        //localToParent(result);
        return result;
    }

    /**
     * This is a crucial step. We have to override this method to invalidate the paint each time the bounds are changed
     * so we repaint the correct region // True must be returned for smooth update of bounds (TODO: Move to NG)
     */
    @Override
    public boolean validateFullBounds() {
        try {
            if (comparisonBounds == null) {
                System.out.println("Im null before.");
            }
            comparisonBounds = getUnionOfBounds();
            if (comparisonBounds == null) {
                System.out.println("Im null after.");
            }
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }

        if (!cachedChildBounds.equals(comparisonBounds)) {
            setPaintInvalid(true);
        }

        // TODO: We get smooth update when this returns true
        return super.validateFullBounds();
    }

    public boolean isDrawOutline() {
        return drawOutline;
    }

    public void setDrawOutline(boolean drawOutline) {
        this.drawOutline = drawOutline;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getOutlinePadding() {
        return outlinePadding;
    }

    public void setOutlinePadding(int outlinePadding) {
        this.outlinePadding = outlinePadding;
    }

    public boolean isFillBackground() {
        return fillBackground;
    }

    public void setFillBackground(boolean fillBackground) {
        this.fillBackground = fillBackground;
    }

    public Color getLineColor() {
        return lineColor;
    }

    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
    }


}
