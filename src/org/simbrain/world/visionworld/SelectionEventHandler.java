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

    /** Selection marquee. */
    private SelectionMarquee marquee;

    /** Start position for the selection marquee. */
    private Point2D marqueeStartPosition;

    /** Vision world. */
    private VisionWorld visionWorld;

    /** Sensor selection model. */
    private SensorSelectionModel selectionModel;

    /** Bounds filter. */
    private BoundsFilter boundsFilter = new BoundsFilter();

    /** Prior selection, if any. */
    private Collection<Sensor> priorSelection = Collections.<Sensor>emptyList();


    /**
     * Create a new selection event handler for the specified vision world.
     *
     * @param visionWorld vision world, must not be null
     */
    SelectionEventHandler(final VisionWorld visionWorld) {
        super();
        if (visionWorld == null) {
            throw new IllegalArgumentException("visionWorld must not be null");
        }
        this.visionWorld = visionWorld;
        this.selectionModel = visionWorld.getSensorSelectionModel();
    }


    /** {@inheritDoc} */
    public void mouseClicked(final PInputEvent event) {
        if (!event.isLeftMouseButton()) {
            return;
        }
        super.mouseClicked(event);
        if (event.getClickCount() != 1) {
            return;
        }
    }

    /** {@inheritDoc} */
    protected void startDrag(final PInputEvent event) {
        if (!event.isLeftMouseButton()) {
            return;
        }
        super.startDrag(event);

        marqueeStartPosition = event.getPosition();
        PCanvas canvas = (PCanvas) event.getComponent();

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

    /** {@inheritDoc} */
    protected void drag(final PInputEvent event) {
        if (!event.isLeftMouseButton()) {
            return;
        }
        super.drag(event);

        PCanvas canvas = (PCanvas) event.getComponent();

        Point2D position = event.getPosition();
        PBounds rect = new PBounds();
        rect.add(marqueeStartPosition);
        rect.add(position);

        marquee.globalToLocal(rect);

        marquee.setPathToRectangle((float) rect.getX(), (float) rect.getY(),
                                   (float) rect.getWidth(), (float) rect.getHeight());
    }

    /** {@inheritDoc} */
    protected void endDrag(final PInputEvent event) {
        if (!event.isLeftMouseButton()) {
            return;
        }
        super.endDrag(event);
        PCanvas canvas = (PCanvas) event.getComponent();

        PBounds rect = marquee.getBounds();
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
            if (highlightedSensors.size() == 1) {
                selectionModel.toggleSelection(highlightedSensors.iterator().next());
            }
            else {
                Collection<Sensor> selection = union(priorSelection, highlightedSensors);
                selectionModel.setSelection(selection);
            }
        } else {
            selectionModel.setSelection(highlightedSensors);
        }

        marquee.removeFromParent();
        marquee = null;
        marqueeStartPosition = null;

        priorSelection = Collections.<Sensor>emptyList();
        canvas.repaint();
    }

    /**
     * Return the union of the specified collections <code>coll1</code> and
     * <code>coll2</code>.
     *
     * @param <T> collection element type
     * @param coll1 first collection
     * @param coll2 second collection
     * @return the union of the specified collections
     */
    private <T> Collection<T> union(final Collection<T> coll1, final Collection<T> coll2) {
        Collection<T> union = new HashSet<T>();
        union.addAll(coll1);
        union.addAll(coll2);
        return union;
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
            boolean isSensorNode = (node instanceof SensorNode);
            boolean boundsIntersects = node.getGlobalBounds().intersects(bounds);
            return (isSensorNode && boundsIntersects);
        }

        /** {@inheritDoc} */
        public boolean acceptChildrenOf(final PNode node) {
            boolean areChildrenPickable = node.getChildrenPickable();
            boolean isCamera = (node instanceof PCamera);
            boolean isLayer = (node instanceof PLayer);
            boolean isMarquee = (marquee == node);
            return ((areChildrenPickable || isCamera || isLayer) && !isMarquee);
        }
    }
}
