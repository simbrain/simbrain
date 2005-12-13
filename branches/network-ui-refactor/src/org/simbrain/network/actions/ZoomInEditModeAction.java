
package org.simbrain.network.actions;

import javax.swing.KeyStroke;

import org.simbrain.network.EditMode;
import org.simbrain.network.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Zoom in edit mode action.
 */
public final class ZoomInEditModeAction
    extends EditModeAction {

    /**
     * Create a new zoom in edit mode action.
     *
     * @param networkPanel network panel, must not be null
     */
    public ZoomInEditModeAction(final NetworkPanel networkPanel) {
        super("Zoom in", networkPanel, EditMode.ZOOM_IN);
        putValue(SMALL_ICON, ResourceManager.getImageIcon("ZoomIn.gif"));
        putValue(SHORT_DESCRIPTION, "Zoom mode (z)");

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke('z'), this);
        networkPanel.getActionMap().put(this, this);

    }
}