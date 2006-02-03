
package org.simbrain.network;

import java.awt.event.InputEvent;

import edu.umd.cs.piccolo.PCamera;

import edu.umd.cs.piccolo.util.PBounds;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventFilter;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;

/**
 * Zoom event handler.
 */
final class ZoomEventHandler
    extends PBasicInputEventHandler {

    /** Zoom in factor. */
    private static final double ZOOM_IN_FACTOR = 0.5d;

    /** Zoom out factor. */
    private static final double ZOOM_OUT_FACTOR = 1.5d;

    /** Duration in microseconds of the zoom animation. */
    private static final int ZOOM_ANIMATION_DURATION = 1000;


    /**
     * Create a new zoom event handler.
     */
    public ZoomEventHandler() {
        super();
        setEventFilter(new ZoomEventFilter());
    }


    /** @see PBasicInputEventHandler */
    public void mouseClicked(final PInputEvent event) {

        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
        PCamera camera = networkPanel.getCamera();

        EditMode editMode = networkPanel.getEditMode();
        double zoom = editMode.isZoomIn() ? ZOOM_IN_FACTOR : ZOOM_OUT_FACTOR;

        double x = event.getPosition().getX();
        double y = event.getPosition().getY();
        double w = camera.getViewBounds().getWidth() * zoom;

        PBounds rect = new PBounds(x - (w / 2), y - (w / 2), w, w);
        camera.animateViewToCenterBounds(rect, true, ZOOM_ANIMATION_DURATION);

        rect = null;
    }


    /**
     * Zoom event filter, accepts left mouse clicks, but only when the network
     * panel's edit mode is either <code>EditMode.ZOOM_IN</code> or <code>EditMode.ZOOM_OUT</code>.
     */
    private class ZoomEventFilter
        extends PInputEventFilter {

        /**
         * Create a new zoom event filter.
         */
        public ZoomEventFilter() {
            super(InputEvent.BUTTON1_MASK);
        }


        /** @see PInputEventFilter */
        public boolean acceptsEvent(final PInputEvent event, final int type) {

            NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
            EditMode editMode = networkPanel.getEditMode();

            return (editMode.isZoom() && super.acceptsEvent(event, type));
        }
    }
}