package org.simbrain.network.gui;

import java.util.EventListener;

/**
 * Model listener.
 */
public interface ClipboardListener extends EventListener {

    /**
     * Notify of clipboard changed.
     */
    void clipboardChanged();
}
