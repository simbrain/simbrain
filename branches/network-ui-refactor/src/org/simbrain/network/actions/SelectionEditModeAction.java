
package org.simbrain.network.actions;

import javax.swing.KeyStroke;

import org.simbrain.network.EditMode;
import org.simbrain.network.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Selection edit mode action.
 */
public final class SelectionEditModeAction
    extends EditModeAction {

    /**
     * Create a new selection build mode action.
     *
     * @param networkPanel network panel, must not be null
     */
    public SelectionEditModeAction(final NetworkPanel networkPanel) {
        super("Selection", networkPanel, EditMode.SELECTION);

        putValue(SMALL_ICON, ResourceManager.getImageIcon("Arrow.gif"));
        putValue(SHORT_DESCRIPTION, "Selection mode (s)");

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke('s'), this);
        networkPanel.getActionMap().put(this, this);

    }
}