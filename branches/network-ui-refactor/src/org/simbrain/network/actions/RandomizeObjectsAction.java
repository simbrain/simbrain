
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;

import org.simbrain.resource.ResourceManager;

/**
 * Randomize screen elements action.
 *
 * TODO: rename to RandomizeScreenElementsAction?
 */
public final class RandomizeObjectsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new randomize screen elements action with the
     * specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public RandomizeObjectsAction(final NetworkPanel networkPanel) {
        super("Randomize selection");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Rand.gif"));

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke('r'), this);
        networkPanel.getActionMap().put(this, this);

    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        System.out.println("Ranndomize neurons and/ or weights");
    }
}