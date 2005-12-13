
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

        // if shift key is not down...
        networkPanel.clearSelection();

        // create a new selection marquee at the mouse position
        marquee = new SelectionMarquee((float) marqueeStartPosition.getX(),
                                       (float) marqueeStartPosition.getY());

        // add marquee as child of the network panel's layer
        //    NOTE:  this makes the marquee scale with zoom. . .
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
            boolean isLayer = (node instanceof PLayer);
            boolean isCamera = (node instanceof PCamera);
            boolean isMarquee = (marquee == node);

            boolean rv = (isPickable && boundsIntersects && !isLayer && !isCamera && !isMarquee);

            if (rv) {
                System.out.println("   accepting " + node.getClass());
            }
            else {
                System.out.println("   rejecting " + node.getClass());
            }

            return rv;
        }

        /** @see PNodeFilter */
        public boolean acceptChildrenOf(final PNode node) {

            //return true;
            boolean areChildrenPickable = node.getChildrenPickable();
            boolean isCamera = (node instanceof PCamera);
            boolean isLayer = (node instanceof PLayer);
            boolean isMarquee = (marquee == node);

            boolean rv = ((areChildrenPickable || isCamera || isLayer) && !isMarquee);

            if (rv) {
                System.out.println("accepting children of " + node.getClass());
            }
            else {
                System.out.println("rejecting children of " + node.getClass());
            }

            return rv;
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