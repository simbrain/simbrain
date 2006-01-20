
package org.simbrain.network;

import java.util.EventListener;

/**
 * A listener that receives notification of changes in
 * the selection for network panel.
 */
public interface NetworkSelectionListener
    extends EventListener {

    /**
     * Notify this listener that the selection has changed.
     *
     * @param e network selection event
     */
    void selectionChanged(NetworkSelectionEvent e);
}