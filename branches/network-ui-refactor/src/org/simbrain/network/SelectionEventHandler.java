
package org.simbrain.network;

import java.awt.event.InputEvent;

import java.awt.geom.Point2D;

import java.util.Collection;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PCamera;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventFilter;
import edu.umd.cs.piccolo.event.PDragSequenceEventHandler;

import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PNodeFilter;

import org.simbrain.network.nodes.SelectionMarquee;

/**
 * Selection event handler.
 */
final class SelectionEventHandler
    extends PDragSequenceEventHandler {

    /** Selection marquee. */
    private SelectionMarquee marquee;

    /** Marquee selection start position. */
    private Point2D marqueeStartPosition;

    /** Bounds filter. */
    private final BoundsFilter boundsFilter;


    /**
     * Create a new selection event handler.
     */
    public SelectionEventHandler() {
        super();

        boundsFilter = new BoundsFilter();
        setEventFilter(new SelectionEventFilter());
    }

    /** @see PDragSequenceEventHandler */
    protected void startDrag(final PInputEvent event) {

        super.startDrag(event);

        marqueeStartPosition = event.getPosition();
        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();

        // if shift key is not down... {
        networkPanel.clearSelection();
        // }

        // create a new selection marquee at the mouse position
        marquee = new SelectionMarquee((float) marqueeStartPosition.getX(),
                                       (float) marqueeStartPosition.getY());

        // add marquee as child of the network panel's layer
        networkPanel.getLayer().addChild(marquee);
    }

    /** @see PDragSequenceEventHandler */
    protected void drag(final PInputEvent event) {

        super.drag(event);

        Point2D position = event.getPosition();
        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();

        PBounds rect = new PBounds();
        rect.add(marqueeStartPosition);
        rect.add(position);

        marquee.globalToLocal(rect);

        marquee.setPathToRectangle((float) rect.getX(), (float) rect.getY(),
                                   (float) rect.getWidth(), (float) rect.getHeight());

        boundsFilter.setBounds(rect);
        Collection highlightedNodes = networkPanel.getLayer().getRoot().getAllNodes(boundsFilter, null);

        networkPanel.setSelection(highlightedNodes);
    }

    /** @see PDragSequenceEventHandler */
    protected void endDrag(final PInputEvent event) {

        super.endDrag(event);

        marquee.removeFromParent();
        marquee = null;
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
            boolean boundsIntersects = node.intersects(bounds);
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
     * the network panel's build mode is <code>BuildMode.SELECTION</code>.
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
            BuildMode buildMode = networkPanel.getBuildMode();

            return (buildMode.isSelection() && super.acceptsEvent(event, type));
        }
    }
}