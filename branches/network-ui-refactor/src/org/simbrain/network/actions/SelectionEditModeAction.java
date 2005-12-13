
package org.simbrain.network.actions;

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
    }
}