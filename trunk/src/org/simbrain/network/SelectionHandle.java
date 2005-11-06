/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;

import org.simbrain.network.pnodes.PNodeWeight;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolox.handles.PHandle;
import edu.umd.cs.piccolox.util.PNodeLocator;


/**
 * <b>SelectionHandle</b> provides methods to draw a rectangle around a selected neuron (or other screen object) and to
 * remove that rectangle when the object is unselected.
 *
 * @author Mai Ngoc Thang
 */
public class SelectionHandle extends PHandle {
    private double xRatio = 0.2;

    /**
     * the ratio of the length of the bounding box to the length of the PNode
     */
    /**
     * Constructs a selection box based on the PNode associated with the given PNodeLocator.  The width and height of
     * the selection box will be computed from the PNode's width and height.
     *
     * @param aLocator PNodeLocator that determine the PNode
     */
    public SelectionHandle(final PNodeLocator aLocator, final NetworkPanel net) {
        super(aLocator);

        PNode node = aLocator.getNode();
        this.setPaint(null);
        this.setStrokePaint(net.getSelectionColor());
        this.reset();

        double xExt = getExtendAmount(node.getWidth());
        double yExt = getExtendAmount(node.getHeight());

        Rectangle2D rec = new Rectangle2D.Double(0d, 0d, node.getWidth() + (xExt * 2), node.getHeight() + (yExt * 2));

        this.append(rec, false);
    }

    /**
     * Adds a selection box to a PNode
     *
     * @param aNode node to add selection box to
     */
    public static void addSelectionHandleTo(PNode aNode, final NetworkPanel net) {
        if (aNode instanceof PNodeWeight) {
            aNode = ((PNodeWeight) aNode).getWeightBall();
        }

        aNode.addChild(new SelectionHandle(new PNodeLocator(aNode), net));
    }

    /**
     * Removes selection boxes from a PNode
     *
     * @param aNode node to remove selection box from
     */
    public static void removeSelectionHandleFrom(PNode aNode) {
        if (aNode instanceof PNodeWeight) {
            aNode = ((PNodeWeight) aNode).getWeightBall();
        }

        ArrayList handles = new ArrayList();

        Iterator i = aNode.getChildrenIterator();

        while (i.hasNext()) {
            PNode each = (PNode) i.next();

            if (each instanceof SelectionHandle) {
                handles.add(each);
            }
        }

        aNode.removeChildren(handles);
    }

    /**
     * Scales the length of a PNode based on xRatio
     *
     * @param length length to extend
     *
     * @return the extended length
     */
    private double getExtendAmount(final double length) {
        return length * xRatio;
    }
}
