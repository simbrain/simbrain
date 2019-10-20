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
package org.simbrain.network.gui;

import org.apache.commons.lang3.SystemUtils;
import org.piccolo2d.PCamera;
import org.piccolo2d.PLayer;
import org.piccolo2d.PNode;
import org.piccolo2d.event.PDragSequenceEventHandler;
import org.piccolo2d.event.PInputEvent;
import org.piccolo2d.event.PInputEventFilter;
import org.piccolo2d.extras.nodes.PStyledText;
import org.piccolo2d.util.PBounds;
import org.piccolo2d.util.PDimension;
import org.piccolo2d.util.PNodeFilter;
import org.simbrain.network.gui.nodes.*;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.Utils;
import org.simbrain.util.piccolo.SelectionMarquee;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Handle network mouse events, which  drag objects, select objects, toggle selections,
 * pan the canvas, create lassos for selection, etc.
 *
 * @author Michael Heuer
 * @author Jeff Yoshimi
 */
final class MouseEventHandler extends PDragSequenceEventHandler {

    /**
     * Selection marquee.
     */
    private SelectionMarquee marquee;

    /**
     * Picked node, if any, at the beginning of this drag sequence.
     */
    private PNode pickedNode;

    /**
     * Marquee selection start position.
     */
    private Point2D marqueeStartPosition;

    /**
     * Bounds filter.
     */
    private final BoundsFilter boundsFilter;

    /**
     * Prior selection, if any. Required for shift-lasso selection.
     */
    private Collection priorSelection = Collections.EMPTY_LIST;

    /**
     * Network Panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Create a new selection event handler.
     *
     * @param networkPanel parent panel
     */
    public MouseEventHandler(NetworkPanel networkPanel) {
        super();
        boundsFilter = new BoundsFilter();
        setEventFilter(new SelectionEventFilter());
        this.networkPanel = networkPanel;
    }

    @Override
    public void mousePressed(final PInputEvent event) {
        super.mousePressed(event);
    }

    @Override
    public void mouseClicked(final PInputEvent event) {

        // A click without a drag, e.g. making a lasso
        // Reset placement manager's anchor point
        networkPanel.getPlacementManager().setAnchorPoint(event.getPosition());

        // System.out.println("In net panel mouse clicked:" + event);
        super.mouseClicked(event);

        // Set picked node
        PNode node = event.getPath().getPickedNode();
        //System.out.println("Mouse clicked / Picked node: " + node);

        // Double click on text objects to edit them
        if (event.getClickCount() != 1) {
            if (node instanceof PStyledText) {
                networkPanel.clearSelection();
                networkPanel.getTextHandle().startEditing(event, ((PStyledText) node));
            }
            return;
        }

        // Clicking in empty parts of the canvas removes green selections.
        if (node instanceof PCamera) {
            if (!event.isShiftDown()) {
                networkPanel.clearSelection();
            }
        }
    }

    @Override
    protected void startDrag(final PInputEvent event) {

        super.startDrag(event);

        marqueeStartPosition = event.getPosition();

        // Set the initially picked node
        pickedNode = event.getPath().getPickedNode();

        // Cases where nothing was clicked on
        if (noObjectWasClickedOn()) {
            if (event.isShiftDown()) {
                priorSelection = new ArrayList(networkPanel.getSelectedNodes());
            } else {
                // Don't clear selection when panning screen
                if (!event.isMetaDown()) {
                    networkPanel.clearSelection();
                }
            }

            SwingUtilities.invokeLater(() -> {
                // Create a new selection marquee at the mouse position
                marquee = new SelectionMarquee((float) marqueeStartPosition.getX(), (float) marqueeStartPosition.getY());

                // Add marquee as child of the network panel's layer
                networkPanel.getCanvas().getLayer().addChild(marquee);
            });
            return;
        }

        // Single click selection of entities happens here.

        // This code transfers the "picked" object to the object that will be dragged
        if (pickedNode.getParent() instanceof ScreenElement) {
            pickedNode = pickedNode.getParent();
        }

        if (pickedNode instanceof NeuronNode) {
            networkPanel.setLastSelectedNeuron((NeuronNode) pickedNode);
            // NeuronNode's moving flag no longer used. See NeuronNode comments.
            //((NeuronNode) pickedNode).setMoving(true);
        }

        // Either start dragging selected node(s) or toggle selection (if shift
        // is pressed).
        if (networkPanel.isSelected(pickedNode)) {
            if (event.isShiftDown()) {
                networkPanel.toggleSelection(pickedNode);
            }
        } else {
            if (event.isShiftDown()) {
                networkPanel.toggleSelection(pickedNode);
            } else {
                networkPanel.setSelection(Collections.singleton(pickedNode));
            }
        }
    }

    @Override
    protected void drag(final PInputEvent event) {

        super.drag(event);

        // Pan the canvas for command-click on Mac and control-click on other systems
        boolean panMode = false;
        if (SystemUtils.IS_OS_MAC) {
            if (event.isMetaDown()) {
                panMode = true;
            }
        } else {
            if (event.isControlDown()) {
                panMode = true;
            }
        }
        if (panMode) {
            pan(event);
            return;
        }

        // The case where nothing was clicked on initially. So draw the lasso
        // and select things.
        if (noObjectWasClickedOn()) {
            // Select lassoed nodes
            Point2D position = event.getPosition();
            PBounds rect = new PBounds();
            rect.add(marqueeStartPosition);
            rect.add(position);
            if (marquee == null) {
                return;
            }
            marquee.globalToLocal(rect);
            marquee.reset(); //todo: better way?
            marquee.append(new Rectangle2D.Float((float) rect.getX(), (float) rect.getY(), (float) rect.getWidth(), (float) rect.getHeight()), false);
            boundsFilter.setBounds(rect);
            Collection highlightedNodes = networkPanel.getCanvas().getLayer().getRoot().getAllNodes(boundsFilter, null);
            // Toggle things if shift is being pressed
            if (event.isShiftDown()) {
                Collection selection = Utils.union(priorSelection, highlightedNodes);
                selection.removeAll(Utils.intersection(priorSelection, highlightedNodes));
                networkPanel.setSelection(selection);
            } else {
                networkPanel.setSelection(highlightedNodes);
            }
            return;

        }

        // Where is the drag in relation to the initially clicked on object
        PDimension delta = event.getDeltaRelativeTo(pickedNode);

        //System.out.println("Drag:" + pickedNode);

        // Handle interaction box dragging
        if (pickedNode instanceof InteractionBox) {
            delta = event.getDeltaRelativeTo(pickedNode.getParent());
            if (pickedNode.getParent() instanceof GroupNode ) {
                pickedNode.getParent().offset(delta.getWidth(), delta.getHeight());
            }
        }

        // Continue to drag nodes that have already been selected
        for (PNode node : networkPanel.getSelectedNodes()) {
            if (node instanceof ScreenElement) {
                ScreenElement screenElement = (ScreenElement) node;
                if (screenElement.isDraggable()) {
                    screenElement.localToParent(delta);
                    screenElement.offset(delta.getWidth(), delta.getHeight());
                }
            }
        }

    }

    @Override
    protected void endDrag(final PInputEvent event) {

        super.endDrag(event);

        // Nothing was being dragged
        if (noObjectWasClickedOn()) {
            // End lasso selection
            if (marquee != null) {
                marquee.removeFromParent();
            }
            marquee = null;
            marqueeStartPosition = null;
            return;
        }

        // End drag selected node(s)
        pickedNode = null;

        // Set the paste delta
        Point2D upperLeft = SimnetUtils.getUpperLeft(networkPanel.getSelectedModels());
        networkPanel.getPlacementManager().setPasteDelta(upperLeft);
        // Also reset the anchor point, so that new points emerge from here
        // with the delta that was just set
        networkPanel.getPlacementManager().setAnchorPoint(upperLeft);

        priorSelection = Collections.EMPTY_LIST;
        networkPanel.repaint();
    }

    /**
     * Encapsulate logic for determining the case where no object (neuron node, synpase node, etc) was clicked on at the
     * beginning of this drag sequence.
     *
     * @return true if no object was clicked on, false otherwise.
     */
    private boolean noObjectWasClickedOn() {
        boolean pickedNodeNull = (pickedNode == null);
        boolean cameraPicked = (pickedNode instanceof PCamera);
        return (pickedNodeNull || cameraPicked);
    }

    /**
     * A filter that determines whether a given pnode is selectable or not. Bounds are updated as the lasso tool is
     * dragged.
     */
    private class BoundsFilter implements PNodeFilter {

        /**
         * Bounds.
         */
        private PBounds bounds;

        /**
         * Set the bounds for this bounds filter to <code>bounds</code>.
         *
         * @param bounds bounds for this bounds filter
         */
        public void setBounds(final PBounds bounds) {
            this.bounds = bounds;
        }

        /**
         * Return true if the PNode should be dragged
         *
         * @param node the node to check
         * @return true if it should be dragged
         * @see PNodeFilter
         */
        public boolean accept(final PNode node) {
            boolean isPickable = node.getPickable();
            boolean boundsIntersects = node.getGlobalBounds().intersects(bounds);
            // Allow selection of synapses via the line associated with it
            if (node instanceof SynapseNode) {
                SynapseNode synapseNode = (SynapseNode) node;
                if (synapseNode.isSelfConnection()) {
                    Arc2D.Float arc = synapseNode.getArcBound();
                    Area boundArea = new Area(bounds);
                    Area lineArea = new Area(arc);
                    boundArea.intersect(lineArea);
                    if (!boundArea.isEmpty()) {
                        boundsIntersects = true;
                    }
                } else {
                    Line2D.Float line = synapseNode.getLineBound();
                    if (bounds.intersectsLine(line)) {
                        boundsIntersects = true;
                    }
                }

            }
            boolean isLayer = (node instanceof PLayer);
            boolean isCamera = (node instanceof PCamera);
            boolean isMarquee = (marquee == node);

            return (isPickable && boundsIntersects && !isLayer && !isCamera && !isMarquee);
        }

        /**
         * Return true if the the children of the given node should be dragged
         *
         * @param  node the node to test
         * @return true if the children should be dragged
         * @see PNodeFilter
         */
        public boolean acceptChildrenOf(final PNode node) {
            boolean areChildrenPickable = node.getChildrenPickable();
            boolean isCamera = (node instanceof PCamera);
            boolean isLayer = (node instanceof PLayer);
            boolean isMarquee = (marquee == node);

            return ((areChildrenPickable || isCamera || isLayer) && !isMarquee);
        }
    }

    /**
     * Selection event filter, accepts various mouse events, but only when the network panel's edit mode is
     * <code>EditMode.SELECTION</code>.
     */
    private class SelectionEventFilter extends PInputEventFilter {

        /**
         * Create a new selection event filter.
         */
        public SelectionEventFilter() {
            super(InputEvent.BUTTON1_MASK);
        }

        /**
         * @see PInputEventFilter
         */
        public boolean acceptsEvent(final PInputEvent event, final int type) {

            EditMode editMode = networkPanel.getEditMode();

            if (editMode.isSelection() && super.acceptsEvent(event, type)) {
                networkPanel.getTextHandle().stopEditing();
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Pans the camera in response to the pan event provided. (From the source code for PanEventHandler. Note that
     * "autopan"--from that class--is not being used. Not sure what is being lost by not using it.)
     *
     * @param event contains details about the drag used to translate the view
     * @author Jesse Grosjean
     */
    protected void pan(final PInputEvent event) {
        final PCamera c = event.getCamera();
        final Point2D l = event.getPosition();

        if (c.getViewBounds().contains(l)) {
            final PDimension d = event.getDelta();
            c.translateView(d.getWidth(), d.getHeight());
        }
    }

}
