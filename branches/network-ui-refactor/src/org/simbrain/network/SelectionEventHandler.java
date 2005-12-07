
package org.simbrain.network;

import java.awt.geom.Point2D;

import java.util.Collection;

import edu.umd.cs.piccolo.PNode;

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


    /**
     * Create a new selection event handler.
     */
    public SelectionEventHandler() {
        super();
        setEventFilter(new SelectionEventFilter());
    }

    /** @see PDragSequenceEventHandler */
    protected void startDrag(final PInputEvent event) {

        super.startDrag(event);

        marqueeStartPosition = event.getPosition();
        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();

        // create a new selection marquee at the mouse position
        marquee = new SelectionMarquee((float) marqueeStartPosition.getX(),
                                       (float) marqueeStartPosition.getY());

        // add marquee as child of the network panel's layer
        // TODO:
        // I think rather this needs to be added directly to the camera
        //    so that it doesn't scale when zoomed in or out?
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

        BoundsFilter filter = new BoundsFilter(rect);
        // TODO:
        // this filter is not working correctly yet
        Collection highlightedNodes = networkPanel.getLayer().getRoot().getAllNodes(filter, null);

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

        /** Local bounds. */
        private PBounds localBounds;


        /**
         * Create a new bounds filter with the specified bounds.
         *
         * @param bounds bounds
         */
        public BoundsFilter(final PBounds bounds) {
            this.bounds = bounds;
            localBounds = new PBounds();
        }


        /** @see PNodeFilter */
        public boolean accept(final PNode node) {
            localBounds.setRect(bounds);
            node.globalToLocal(localBounds);

            boolean isPickable = node.getPickable();
            boolean boundsIntersects = node.intersects(localBounds);
            boolean isMarquee = (marquee == node);

            return (isPickable && boundsIntersects && !isMarquee);
        }

        /** @see PNodeFilter */
        public boolean acceptChildrenOf(final PNode node) {
            return true;
        }
    }

    /**
     * Selection event filter, accepts various mouse events, but only when
     * the network panel's build mode is <code>BuildMode.SELECTION</code>.
     */
    private class SelectionEventFilter
        extends PInputEventFilter {

        /** @see PInputEventFilter */
        public boolean acceptsEvent(final PInputEvent event, final int type) {

            // TODO:  still accepts context menu events as valid!
            NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
            BuildMode buildMode = networkPanel.getBuildMode();

            return (buildMode.isSelection() && super.acceptsEvent(event, type));
        }
    }
}