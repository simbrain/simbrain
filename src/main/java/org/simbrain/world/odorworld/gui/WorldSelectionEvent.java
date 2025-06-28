package org.simbrain.world.odorworld.gui;

import org.piccolo2d.PNode;
import org.simbrain.world.odorworld.OdorWorldPanel;

import java.util.Collections;
import java.util.EventObject;
import java.util.Set;

/**
 * An event object representing a change in network selection.
 */
public class WorldSelectionEvent extends EventObject {

    /**
     * Old selection.
     */
    private final Set<PNode> oldSelection;

    /**
     * Selection.
     */
    private final Set<PNode> selection;

    /**
     * Create a new network selection event with the specified source.
     *
     * @param source       source of the event
     * @param oldSelection old selection
     * @param selection    selection
     */
    public WorldSelectionEvent(final OdorWorldPanel source, final Set<PNode> oldSelection, final Set<PNode> selection) {
        super(source);
        this.oldSelection = Collections.unmodifiableSet(oldSelection);
        this.selection = Collections.unmodifiableSet(selection);
    }

    /**
     * Return the source of this event as a OdorWorldPanel.
     *
     * @return the source of this event as a OdorWorldPanel
     */
    public OdorWorldPanel getWorldPanel() {
        return (OdorWorldPanel) getSource();
    }

    /**
     * Return the old selection.
     *
     * @return the old selection
     */
    public Set<PNode> getOldSelection() {
        return oldSelection;
    }

    /**
     * Return the selection.
     *
     * @return the selection
     */
    public Set<PNode> getSelection() {
        return selection;
    }
}