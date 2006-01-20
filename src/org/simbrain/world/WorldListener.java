
package org.simbrain.world;

import java.util.EventListener;

/**
 * Classes implementing this interface are notified of world events.
 */
public interface WorldListener extends EventListener {

    /**
     * Notify this listener of a worldChanged event.
     */
    void worldChanged();
}