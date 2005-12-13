
package org.simbrain.network.actions;

import javax.swing.KeyStroke;

import org.simbrain.network.EditMode;
import org.simbrain.network.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Pan edit mode action.
 */
public final class PanEditModeAction
    extends EditModeAction {

    /**
     * Create a new pan edit mode action.
     *
     * @param networkPanel network panel, must not be null
     */
    public PanEditModeAction(final NetworkPanel networkPanel) {
        super("Pan", networkPanel, EditMode.PAN);
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Pan.gif"));
        putValue(SHORT_DESCRIPTION, "Pan Mode (k)");

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke('k'), this);
        networkPanel.getActionMap().put(this, this);

    }
}