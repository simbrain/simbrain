
package org.simbrain.network.actions;

import org.simbrain.network.EditMode;
import org.simbrain.network.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Zoom out edit mode action.
 */
public final class ZoomOutEditModeAction
    extends EditModeAction {

    /**
     * Create a new zoom out edit mode action.
     *
     * @param networkPanel network panel, must not be null
     */
    public ZoomOutEditModeAction(final NetworkPanel networkPanel) {
        super("Zoom out", networkPanel, EditMode.ZOOM_OUT);
        putValue(SMALL_ICON, ResourceManager.getImageIcon("ZoomOut.gif"));

    }
}