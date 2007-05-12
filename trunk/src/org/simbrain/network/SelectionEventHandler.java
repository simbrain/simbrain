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
package org.simbrain.network;

import java.awt.event.InputEvent;

import java.awt.geom.Point2D;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PCamera;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventFilter;
import edu.umd.cs.piccolo.event.PDragSequenceEventHandler;

import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDimension;
import edu.umd.cs.piccolo.util.PNodeFilter;
import edu.umd.cs.piccolox.nodes.P3DRect;
import edu.umd.cs.piccolox.nodes.PStyledText;

import org.apache.commons.collections.CollectionUtils;

import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.network.nodes.ScreenElement;
import org.simbrain.network.nodes.SelectionMarquee;
import org.simbrain.network.nodes.SubnetworkNode;
import org.simbrain.util.Utils;
import org.simnet.util.SimnetUtils;

/**
 * Selection event handler.
 */
final class SelectionEventHandler
    extends PDragSequenceEventHandler {

    /** Selection marquee. */
    private SelectionMarquee marquee;

    /** Picked node, if any. */
    private PNode pickedNode;

    /** Marquee selection start position. */
    private Point2D marqueeStartPosition;

    /** Bounds filter. */
    private final BoundsFilter boundsFilter;

    /** Prior selection, if any.  Required for shift-lasso selection. */
    private Collection priorSelection = Collections.EMPTY_LIST;


    /**
     * Create a new selection event handler.
     */
    public SelectionEventHandler() {
        super();

        boundsFilter = new BoundsFilter();
        setEventFilter(new SelectionEventFilter());
    }


    /** @see PDragSequenceEventHandler */
    public void mousePressed(final PInputEvent event) {
        super.mousePressed(event);
        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
        networkPanel.setLastClickedPosition(event.getPosition());
        if (event.getPath().getPickedNode() instanceof PCamera) {
            networkPanel.setBeginPosition(event.getPosition());
        }
    }

    /** @see PDragSequenceEventHandler */
    public void mouseClicked(final PInputEvent event) {

        super.mouseClicked(event);

        if (event.getClickCount() != 1) {
            return;
        }

        PNode node = event.getPath().getPickedNode();
        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();

        if (node instanceof PCamera) {
            if (!event.isShiftDown()) {
                networkPanel.clearSelection();
            }
        }
    }

    /** @see PDragSequenceEventHandler */
    protected void startDrag(final PInputEvent event) {

        super.startDrag(event);

        marqueeStartPosition = event.getPosition();
        pickedNode = event.getPath().getPickedNode();
        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();

        if (pickedNode instanceof PCamera) {
            pickedNode = null;
        }

        // Clicking on text objects picks the underlying text object
        if (pickedNode instanceof PStyledText) {
            pickedNode = pickedNode.getParent();
        }


        if (pickedNode == null) {

            if (event.isShiftDown()) {
                priorSelection = new ArrayList(networkPanel.getSelection());
            } else {
                networkPanel.clearSelection();
            }

            // create a new selection marquee at the mouse position
            marquee = new SelectionMarquee((float) marqueeStartPosition.getX(),
                                           (float) marqueeStartPosition.getY());

            // add marquee as child of the network panel's layer
            networkPanel.getLayer().addChild(marquee);
        } else {
            if (pickedNode instanceof NeuronNode) {
                networkPanel.setLastSelectedNeuron((NeuronNode) pickedNode);
                // To ensure fire neuron moving events don't affect these neurons
                ((NeuronNode)pickedNode).setMoving(true);
            }

            // start dragging selected node(s)
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
    }

    /** @see PDragSequenceEventHandler */
    protected void drag(final PInputEvent event) {

        super.drag(event);

        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();

        if (pickedNode == null) {

            // continue marquee selection
            Point2D position = event.getPosition();
            PBounds rect = new PBounds();
            rect.add(marqueeStartPosition);
            rect.add(position);

            marquee.globalToLocal(rect);

            marquee.setPathToRectangle((float) rect.getX(), (float) rect.getY(),
                                       (float) rect.getWidth(), (float) rect.getHeight());

            boundsFilter.setBounds(rect);

            Collection highlightedNodes = networkPanel.getLayer().getRoot().getAllNodes(boundsFilter, null);

            if (event.isShiftDown()) {
                Collection selection = CollectionUtils.union(priorSelection, highlightedNodes);
                selection.removeAll(CollectionUtils.intersection(priorSelection, highlightedNodes));
                networkPanel.setSelection(selection);
            } else {
                networkPanel.setSelection(highlightedNodes);
            }

        } else {
            
            // continue to drag selected node(s)
            PDimension delta = event.getDeltaRelativeTo(pickedNode);

            for (Iterator i = networkPanel.getSelection().iterator(); i.hasNext(); ) {
                PNode node = (PNode) i.next();
                if (node instanceof ScreenElement) {

                    if (pickedNode instanceof NeuronNode) {
                        ((NeuronNode)pickedNode).pushViewPositionToModel();
                    }

                    ScreenElement screenElement = (ScreenElement) node;
                    if (screenElement.isDraggable()) {
                        screenElement.localToParent(delta);
                        screenElement.offset(delta.getWidth(), delta.getHeight());
                        networkPanel.setChangedSinceLastSave(true);
                    }
                }
            }
        }
    }

    /** @see PDragSequenceEventHandler */
    protected void endDrag(final PInputEvent event) {

        super.endDrag(event);
        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();

        // Nothing was being dragged
        if (pickedNode == null) {
            // end marquee selection
            marquee.removeFromParent();
            marquee = null;
            marqueeStartPosition = null;
        } else {
        // Something was being dragged

            // To ensure fire neuron moving events don't affect these neurons
            for (Iterator i = networkPanel.getSelection().iterator(); i.hasNext(); ) {
                PNode node = (PNode) i.next();
                if (node instanceof NeuronNode) {
                    ((NeuronNode)node).setMoving(false);
                    ((NeuronNode)node).pushViewPositionToModel();
                } else if (node instanceof SubnetworkNode) {
                    for (Object object : (ArrayList) node.getAllNodes()) {
                      if (object instanceof NeuronNode) {
                          ((NeuronNode)object).setMoving(false);
                          ((NeuronNode)object).pushViewPositionToModel();
                      }
                  }
              }
            }

            // end drag selected node(s)
            pickedNode = null;

            // Reset the beginning of a sequence of pastes, but keep the old paste-offset
            // This occurs when pasting a sequence, and moving one set of objects to a new location
            if (networkPanel.getNumberOfPastes() != 1) {
                networkPanel.setBeginPosition(SimnetUtils.getUpperLeft((ArrayList) networkPanel.getSelectedModelElements()));
            }
            networkPanel.setEndPosition(SimnetUtils.getUpperLeft((ArrayList) networkPanel.getSelectedModelElements()));
        }
        priorSelection = Collections.EMPTY_LIST;
        networkPanel.repaint();
    }


    /**
     * Bounds filter.
     */
    private class BoundsFilter
        implements PNodeFilter {

        /** Bounds. */
        private PBounds bounds;


        /**
         * Set the bounds for this bounds filter to <code>bounds</code>.
         *
         * @param bounds bounds for this bounds filter
         */
        public void setBounds(final PBounds bounds) {
            this.bounds = bounds;
        }

        /** @see PNodeFilter */
        public boolean accept(final PNode node) {
            boolean isPickable = node.getPickable();
            boolean boundsIntersects = node.getGlobalBounds().intersects(bounds);
            boolean isLayer = (node instanceof PLayer);
            boolean isCamera = (node instanceof PCamera);
            boolean isMarquee = (marquee == node);

            return (isPickable && boundsIntersects && !isLayer && !isCamera && !isMarquee);
        }

        /** @see PNodeFilter */
        public boolean acceptChildrenOf(final PNode node) {
            boolean areChildrenPickable = node.getChildrenPickable();
            boolean isCamera = (node instanceof PCamera);
            boolean isLayer = (node instanceof PLayer);
            boolean isMarquee = (marquee == node);

            return ((areChildrenPickable || isCamera || isLayer) && !isMarquee);
        }
    }

    /**
     * Selection event filter, accepts various mouse events, but only when
     * the network panel's edit mode is <code>EditMode.SELECTION</code>.
     */
    private class SelectionEventFilter
        extends PInputEventFilter {

        /**
         * Create a new selection event filter.
         */
        public SelectionEventFilter() {
            super(InputEvent.BUTTON1_MASK);
        }


        /** @see PInputEventFilter */
        public boolean acceptsEvent(final PInputEvent event, final int type) {

            NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
            EditMode editMode = networkPanel.getEditMode();

            return (editMode.isSelection() && super.acceptsEvent(event, type));
        }
    }
}