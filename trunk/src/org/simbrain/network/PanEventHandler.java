
package org.simbrain.network;

import java.awt.event.InputEvent;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventFilter;
import edu.umd.cs.piccolo.event.PPanEventHandler;

/**
 * Pan event handler.
 */
final class PanEventHandler
    extends PPanEventHandler {

    /**
     * Create a new pan event handler.
     */
    public PanEventHandler() {
        super();
        setEventFilter(new PanEventFilter());
    }


    /**
     * Pan event filter, accepts left mouse clicks, but only when the network
     * panel's edit mode is <code>EditMode.PAN</code>.
     */
    private class PanEventFilter
        extends PInputEventFilter {

        /**
         * Create a new pan event filter.
         */
        public PanEventFilter() {
            super(InputEvent.BUTTON1_MASK);
        }


        /** @see PInputEventFilter */
        public boolean acceptsEvent(final PInputEvent event, final int type) {

            NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
            EditMode editMode = networkPanel.getEditMode();

            return (editMode.isPan() && super.acceptsEvent(event, type));
        }
    }
}