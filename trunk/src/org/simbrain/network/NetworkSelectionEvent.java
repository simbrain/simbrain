
package org.simbrain.network;

import java.util.Set;
import java.util.Collections;

import java.util.EventObject;

/**
 * An event object representing a change in network selection.
 */
public final class NetworkSelectionEvent
    extends EventObject {

    /** Old selection. */
    private Set oldSelection;

    /** Selection. */
    private Set selection;


    /**
     * Create a new network selection event with the specified source.
     *
     * @param source source of the event
     * @param oldSelection old selection
     * @param selection selection
     */
    public NetworkSelectionEvent(final NetworkPanel source,
                                 final Set oldSelection,
                                 final Set selection) {
        super(source);
        this.oldSelection = Collections.unmodifiableSet(oldSelection);
        this.selection = Collections.unmodifiableSet(selection);
    }


    /**
     * Return the source of this event as a NetworkPanel.
     *
     * @return the source of this event as a NetworkPanel
     */
    public NetworkPanel getNetworkPanel() {
        return (NetworkPanel) getSource();
    }

    /**
     * Return the old selection.
     *
     * @return the old selection
     */
    public Set getOldSelection() {
        return oldSelection;
    }

    /**
     * Return the selection.
     *
     * @return the selection
     */
    public Set getSelection() {
        return selection;
    }
}