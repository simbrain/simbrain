/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2007 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world.visionworld;

import java.awt.geom.Point2D;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;

import edu.umd.cs.piccolo.event.PDragSequenceEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventFilter;

import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDimension;
import edu.umd.cs.piccolo.util.PNodeFilter;

import org.simbrain.network.nodes.SelectionMarquee;

import org.simbrain.world.visionworld.node.PixelMatrixImageNode;
import org.simbrain.world.visionworld.node.SensorMatrixNode;
import org.simbrain.world.visionworld.node.SensorNode;

/**
 * Sensor selection event handler.
 */
final class SelectionEventHandler
    extends PDragSequenceEventHandler {

    private SelectionMarquee marquee;
    private PNode pickedNode;
    private Point2D marqueeStartPosition;
    private VisionWorld visionWorld;
    private SensorSelectionModel selectionModel;
    private BoundsFilter boundsFilter = new BoundsFilter();
    private Collection<Sensor> priorSelection = Collections.<Sensor>emptyList();

    SelectionEventHandler(final VisionWorld visionWorld) {
        super();
        this.visionWorld = visionWorld;
        this.selectionModel = visionWorld.getSensorSelectionModel();
    }

    public void mouseClicked(final PInputEvent event) {
        super.mouseClicked(event);
        if (event.getClickCount() != 1) {
            return;
        }
        PNode node = event.getPath().getPickedNode();

        if ((node instanceof PCamera) && (!event.isShiftDown())) {
            selectionModel.clear();
        }
    }

    protected void startDrag(final PInputEvent event) {
        super.startDrag(event);

        marqueeStartPosition = event.getPosition();
        pickedNode = event.getPath().getPickedNode();
        PCanvas canvas = (PCanvas) event.getComponent();

        if (pickedNode instanceof PCamera) {
            pickedNode = null;
        }

        if (pickedNode == null) {
            if (event.isShiftDown()) {
                priorSelection = new HashSet<Sensor>(selectionModel.getSelection());
            }
            else {
                selectionModel.clear();
            }
            marquee = new SelectionMarquee((float) marqueeStartPosition.getX(),
                                           (float) marqueeStartPosition.getY());
            canvas.getLayer().addChild(marquee);
        }
        else {
            if (pickedNode instanceof SensorNode) {
                SensorNode sensorNode = (SensorNode) pickedNode;
                Sensor sensor = sensorNode.getSensor();
                if (isSensorNodeAChildOfTheFocusOwner(sensorNode)) {
                    if (selectionModel.isSelected(sensor)) {
                        if (event.isShiftDown()) {
                            selectionModel.toggleSelection(sensor);
                        }
                    }
                    else {
                        if (event.isShiftDown()) {
                            selectionModel.toggleSelection(sensor);
                        }
                        else {
                            selectionModel.setSelection(Collections.singleton(sensor));
                        }
                    }
                }
            }
        }
    }

    private boolean isSensorNodeAChildOfTheFocusOwner(final SensorNode sensorNode) {
        PNode parent = sensorNode;
        while (!(parent instanceof SensorMatrixNode)) {
            parent = parent.getParent();
        }
        return visionWorld.getFocusOwner().equals(parent);
    }

    protected void drag(final PInputEvent event) {
        super.drag(event);

        PCanvas canvas = (PCanvas) event.getComponent();

        if (pickedNode == null) {
            Point2D position = event.getPosition();
            PBounds rect = new PBounds();
            rect.add(marqueeStartPosition);
            rect.add(position);

            marquee.globalToLocal(rect);

            marquee.setPathToRectangle((float) rect.getX(), (float) rect.getY(),
                                       (float) rect.getWidth(), (float) rect.getHeight());

            boundsFilter.setBounds(rect);

            Collection highlightedNodes = canvas.getLayer().getRoot().getAllNodes(boundsFilter, null);

            Collection<Sensor> highlightedSensors = new HashSet<Sensor>();
            for (Iterator i = highlightedNodes.iterator(); i.hasNext(); ) {
                Object node = i.next();
                if (node instanceof SensorNode) {
                    SensorNode sensorNode = (SensorNode) node;
                    highlightedSensors.add(sensorNode.getSensor());
                }
            }

            if (event.isShiftDown()) {
                Collection<Sensor> selection = union(priorSelection, highlightedSensors);
                selection.removeAll(intersection(priorSelection, highlightedSensors));
                selectionModel.setSelection(selection);
            } else {
                selectionModel.setSelection(highlightedSensors);
            }            
        }
    }

    protected void endDrag(final PInputEvent event) {
        super.endDrag(event);
        PCanvas canvas = (PCanvas) event.getComponent();

        if (pickedNode == null) {
            marquee.removeFromParent();
            marquee = null;
            marqueeStartPosition = null;
        }

        priorSelection = Collections.<Sensor>emptyList();
        canvas.repaint();
    }

    private <T> Collection<T> union(final Collection<T> coll1, final Collection<T> coll2) {
        Collection<T> union = new HashSet<T>();
        union.addAll(coll1);
        union.addAll(coll2);
        return union;
    }

    private <T> Collection<T> intersection(final Collection<T> coll1, final Collection<T> coll2) {
        Collection<T> intersection = new HashSet<T>();
        intersection.addAll(coll1);
        intersection.retainAll(coll2);
        return intersection;
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

        /** {@inheritDoc} */
        public boolean accept(final PNode node) {
            boolean isPickable = node.getPickable();
            boolean boundsIntersects = node.getGlobalBounds().intersects(bounds);
            boolean isLayer = (node instanceof PLayer);
            boolean isCamera = (node instanceof PCamera);
            boolean isMarquee = (marquee == node);

            return (isPickable && boundsIntersects && !isLayer && !isCamera && !isMarquee);
        }

        /** {@inheritDoc} */
        public boolean acceptChildrenOf(final PNode node) {
            boolean areChildrenPickable = node.getChildrenPickable();
            boolean isCamera = (node instanceof PCamera);
            boolean isLayer = (node instanceof PLayer);
            boolean isMarquee = (marquee == node);
            boolean isNotFocusOwner = (node instanceof PixelMatrixImageNode) ? true : ((node instanceof SensorMatrixNode) && (!visionWorld.getFocusOwner().equals(node)));
            return ((areChildrenPickable || isCamera || isLayer) && !isNotFocusOwner && !isMarquee);
        }
    }
}
