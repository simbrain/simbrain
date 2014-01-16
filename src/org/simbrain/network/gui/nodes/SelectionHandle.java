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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.piccolo2d.PNode;
import org.piccolo2d.extras.handles.PHandle;
import org.piccolo2d.extras.util.PNodeLocator;

/**
 * The graphical handle drawn around selected PNodes.
 *
 * <p>
 * Usage:
 *
 * <pre>
 * PNode node = ...;
 * SelectionHandle.addSelectionHandleTo(node)
 * </pre>
 *
 * and
 *
 * <pre>
 * PNode node = ...;
 * SelectionHandle.removeSelectionHandleFrom(node)
 * </pre>
 *
 * </p>
 *
 * @see #addSelectionHandleTo(PNode)
 * @see #removeSelectionHandleFrom(PNode)
 */
public final class SelectionHandle extends PHandle {

    /**
     * Amount of space to add between the selected object and the selection
     * handle.
     */
    private static final float DEFAULT_EXTEND_FACTOR = 0.075f;

    /** Color of selection boxes. */
    private static Color selectionColor = Color.green;

    /**
     * Create a new selection handle.
     *
     * @param locator locator
     */
    private SelectionHandle(final PNodeLocator locator) {
        this(locator, DEFAULT_EXTEND_FACTOR);
    }

    /**
     * Create a selection handle with a specified extend factor (distance between selected
     * PNode and the selection handle).
     *
     * @param locator the locator
     * @param extendFactor extension factor (see above).
     */
    private SelectionHandle(final PNodeLocator locator, final float extendFactor) {
        super(locator);

        reset();
        setPickable(false);

        PNode parentNode = locator.getNode();
        parentNode.addChild(this);

        setPaint(null);
        setStrokePaint(selectionColor);

        // Force handle to check its location and size
        updateBounds(extendFactor);
        relocateHandle();
    }

    /** @see PHandle */
    public void parentBoundsChanged() {
        updateBounds(DEFAULT_EXTEND_FACTOR);
        super.parentBoundsChanged();
    }

    /**
     * Update the bounds of this selection handle based on the size of its
     * parent plus an extension factor.
     */
    private void updateBounds(float extendFactor) {
        PNode parentNode = ((PNodeLocator) getLocator()).getNode();

        double x = 0.0f - (parentNode.getBounds().getWidth() * extendFactor);
        double y = 0.0f - (parentNode.getBounds().getHeight() * extendFactor);
        double width = parentNode.getBounds().getWidth() + 2
                * (parentNode.getBounds().getWidth() * extendFactor);
        double height = parentNode.getBounds().getHeight() + 2
                * (parentNode.getBounds().getHeight() * extendFactor);

        this.reset(); // TODO: Check with Heuer
        append(new Rectangle2D.Float((float) x, (float) y, (float) width,
                (float) height), false);
    }

    /**
     * Return true if the specified node has a selection handle as a child.
     *
     * @param node node
     * @return true if the specified node has a selection handle as a child
     */
    private static boolean hasSelectionHandle(final PNode node) {
        for (Iterator i = node.getChildrenIterator(); i.hasNext();) {
            PNode n = (PNode) i.next();
            if (n instanceof SelectionHandle) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a selection handle to the specified node, if one does not exist
     * already.
     *
     * @param node node to add the selection handle to, must not be null
     */
    public static void addSelectionHandleTo(final PNode node) {

        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }

        if (hasSelectionHandle(node)) {
            return;
        }

        PNodeLocator nodeLocator = new PNodeLocator(node);

        // Special treatment for interaction boxes
        if (node instanceof InteractionBox) {
            SelectionHandle selectionHandle = new SelectionHandle(nodeLocator,
                    0);
            selectionHandle.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
        } else {
            SelectionHandle selectionHandle = new SelectionHandle(nodeLocator);
        }

    }

    /**
     * Remove the selection handle(s) from the specified node, if any exist.
     *
     * @param node node to remove the selection handle(s) from, must not be null
     */
    public static void removeSelectionHandleFrom(final PNode node) {

        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }

        Collection handlesToRemove = new ArrayList();

        for (Iterator i = node.getChildrenIterator(); i.hasNext();) {
            PNode n = (PNode) i.next();

            if (n instanceof SelectionHandle) {
                handlesToRemove.add(n);
            }
        }
        node.removeChildren(handlesToRemove);
    }

    /**
     * @return Returns the selectionColor.
     */
    public static Color getSelectionColor() {
        return selectionColor;
    }

    /**
     * @param selectionColor The selectionColor to set.
     */
    public static void setSelectionColor(final Color selectionColor) {
        SelectionHandle.selectionColor = selectionColor;
    }
}