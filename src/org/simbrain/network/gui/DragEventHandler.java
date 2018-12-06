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
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.util.PBounds;
import org.piccolo2d.util.PDimension;
import org.piccolo2d.util.PNodeFilter;
import org.simbrain.network.core.NeuronArray;
import org.simbrain.network.gui.nodes.*;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.Utils;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Handle simbrain drag events, which pan the canvas, create lassos for
 * selection. handles selection an, toggle selection, drags objects as
 * appropriate, updates relevant graphics parameters like
 * "last clicked position".
 * <p>
 * Coding this properly requires tracking picked nodes and their parents fairly
 * closely. To see the scene graph hierarchy for debugging this use ctrl-c while
 * a network panel is open.
 *
 * @author Michael Heuer
 * @author Jeff Yoshimi
 */
final class DragEventHandler extends PDragSequenceEventHandler {

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
    public DragEventHandler(NetworkPanel networkPanel) {
        super();
        boundsFilter = new BoundsFilter();
        setEventFilter(new SelectionEventFilter());
        this.networkPanel = networkPanel;
    }

    @Override
    public void mousePressed(final PInputEvent event) {
        super.mousePressed(event);

        // Set last clicked position, used in many areas for "placement" of
        // objects in the last clicked position on screen.
        networkPanel.setLastClickedPosition(event.getPosition());

        networkPanel.getWhereToAdd().setLocation(event.getPosition());


        // Set pressed position for use in double clicking
        if (event.getPath().getPickedNode() instanceof PCamera) {
            networkPanel.setBeginPosition(event.getPosition());
        }
    }

    @Override
    public void mouseClicked(final PInputEvent event) {

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
                priorSelection = new ArrayList(networkPanel.getSelection());
            } else {
                // Don't clear selection when panning screen
                if (!event.isMetaDown()) {
                    networkPanel.clearSelection();
                }
            }

            // Create a new selection marquee at the mouse position
            marquee = new SelectionMarquee((float) marqueeStartPosition.getX(), (float) marqueeStartPosition.getY());

            // Add marquee as child of the network panel's layer
            networkPanel.getCanvas().getLayer().addChild(marquee);
            return;
        }

        // System.out.println("start:" + pickedNode);
        // System.out.println("start-parent:" + pickedNode.getParent());

        // Must be careful no to add too much hierarchy in pnodes because then it's hard to pick them
        // Currently most pnodes have just a single layer of children.
        if (pickedNode.getParent() instanceof TextNode) {
            pickedNode = pickedNode.getParent();
        } else if (pickedNode.getParent() instanceof NeuronNode) {
            pickedNode = pickedNode.getParent();
        } else if (pickedNode.getParent() instanceof NeuronArrayNode) {
            pickedNode = pickedNode.getParent();
        } else if (pickedNode.getParent() instanceof SynapseNode) {
            pickedNode = pickedNode.getParent();
        } else if (pickedNode.getParent() instanceof InteractionBox) {
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
        if(SystemUtils.IS_OS_MAC) {
            if(event.isMetaDown()) {
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
            if (pickedNode.getParent() instanceof NeuronGroupNode) {
                pickedNode.getParent().offset(delta.getWidth(), delta.getHeight());
            } else if (pickedNode.getParent() instanceof SubnetworkNode) {
                pickedNode.getParent().offset(delta.getWidth(), delta.getHeight());
            }
        }

        // Continue to drag nodes that have already been selected
        for (PNode node : networkPanel.getSelection() ) {
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
            marquee.removeFromParent();
            marquee = null;
            marqueeStartPosition = null;
            return;
        }

        // Reset the beginning of a sequence of pastes, but keep the old
        // paste-offset. This occurs when pasting a sequence, and moving one set
        // of objects to a new location
        if (networkPanel.getNumberOfPastes() != 1) {
            networkPanel.setBeginPosition(SimnetUtils.getUpperLeft((ArrayList) networkPanel.getSelectedModelElements()));
        }

        // End drag selected node(s)
        pickedNode = null;
        networkPanel.setEndPosition(SimnetUtils.getUpperLeft((ArrayList) networkPanel.getSelectedModelElements()));

        // Reset the place new neurons and groups should be added
        networkPanel.getWhereToAdd().setLocation(event.getPosition().getX() + NetworkPanel.DEFAULT_SPACING, event.getPosition().getY());

        priorSelection = Collections.EMPTY_LIST;
        networkPanel.repaint();
    }

    /**
     * Encapsulate logic for determining the case where no object (neuron node,
     * synpase node, etc) was clicked on at the beginning of this drag sequence.
     *
     * @return true if no object was clicked on, false otherwise.
     */
    private boolean noObjectWasClickedOn() {
        boolean pickedNodeNull = (pickedNode == null);
        boolean cameraPicked = (pickedNode instanceof PCamera);
        return (pickedNodeNull || cameraPicked);
    }

    /**
     * A filter that determines whether a given pnode is selectable or not.
     * Bounds are updated as the lasso tool is dragged.
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
         * @param node
         * @return
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
         * @param node
         * @return
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
     * Selection event filter, accepts various mouse events, but only when the
     * network panel's edit mode is <code>EditMode.SELECTION</code>.
     */
    private class SelectionEventFilter extends PInputEventFilter {

        /**
         * Create a new selection event filter.
         */
        public SelectionEventFilter() {
            super(InputEvent.BUTTON1_MASK);
        }

        /**
         * @param event
         * @param type
         * @return
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
     * Pans the camera in response to the pan event provided. (From the source
     * code for PanEventHandler. Note that "autopan"--from that class--is not
     * being used. Not sure what is being lost by not using it.)
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
