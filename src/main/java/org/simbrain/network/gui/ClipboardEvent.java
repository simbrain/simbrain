package org.simbrain.network.gui;

import java.util.EventObject;

/**
 * An event object representing a change in clipboard content.
 */
public class ClipboardEvent extends EventObject {

    /**
     * Create a new clipboard event with the specified source.
     */
    public ClipboardEvent() {
        // super(source);
        super(new Object());
    }
}
