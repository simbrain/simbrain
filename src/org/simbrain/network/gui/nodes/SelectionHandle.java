/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.simbrain.network.gui.NetworkPreferences;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolox.handles.PHandle;
import edu.umd.cs.piccolox.util.PNodeLocator;

/**
 * Selection handle.
 *
 * <p>Usage:
 * <pre>
 * PNode node = ...;
 * SelectionHandle.addSelectionHandleTo(node)
 * </pre>
 * and
 * <pre>
 * PNode node = ...;
 * SelectionHandle.removeSelectionHandleFrom(node)
 * </pre>
 * </p>
 *
 * @see #addSelectionHandleTo(PNode)
 * @see #removeSelectionHandleFrom(PNode)
 */
public final class SelectionHandle
    extends PHandle {

    /** Extend factor. */
    private static final double EXTEND_FACTOR = 0.075d;

    /** Color of selection boxes. */
    private static Color selectionColor = new Color(NetworkPreferences.getSelectionColor());


    /**
     * Create a new selection handle.
     *
     * @param locator locator
     */
    private SelectionHandle(final PNodeLocator locator) {

        super(locator);

        reset();
        setPickable(false);

        PNode parentNode = locator.getNode();
        parentNode.addChild(this);

        setPaint(null);
        setStrokePaint(selectionColor);

        // force handle to check its location and size
        updateBounds();
        relocateHandle();
    }

    /** @see PHandle */
    public void parentBoundsChanged() {
        updateBounds();
        super.parentBoundsChanged();
    }

    /**
     * Update the bounds of this selection handle based on the
     * size of its parent plus an extension factor.
     */
    private void updateBounds() {
        PNode parentNode = ((PNodeLocator) getLocator()).getNode();

        double x = 0.0d - (parentNode.getBounds().getWidth() * EXTEND_FACTOR);
        double y = 0.0d - (parentNode.getBounds().getHeight() * EXTEND_FACTOR);
        double width = parentNode.getBounds().getWidth() + 2 * (parentNode.getBounds().getWidth() * EXTEND_FACTOR);
        double height = parentNode.getBounds().getHeight() + 2 * (parentNode.getBounds().getHeight() * EXTEND_FACTOR);

        setPathToRectangle((float) x, (float) y, (float) width, (float) height);
    }


    /**
     * Return true if the specified node has a selection handle
     * as a child.
     *
     * @param node node
     * @return true if the specified node has a selection handle
     *    as a child
     */
    private static boolean hasSelectionHandle(final PNode node) {

        for (Iterator i = node.getChildrenIterator(); i.hasNext(); ) {
            PNode n = (PNode) i.next();

            if (n instanceof SelectionHandle) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a selection handle to the specified node, if one does not
     * exist already.
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
        SelectionHandle selectionHandle = new SelectionHandle(nodeLocator);
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

        for (Iterator i = node.getChildrenIterator(); i.hasNext(); ) {
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