
package org.simbrain.network.actions;

import javax.swing.KeyStroke;

import org.simbrain.network.EditMode;
import org.simbrain.network.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Build build mode action.
 */
public final class BuildEditModeAction
    extends EditModeAction {

    /**
     * Create a new build edit mode action.
     *
     * @param networkPanel network panel, must not be null
     */
    public BuildEditModeAction(final NetworkPanel networkPanel) {
        super("Build", networkPanel, EditMode.BUILD);

        putValue(SMALL_ICON, ResourceManager.getImageIcon("Build.gif"));
        putValue(SHORT_DESCRIPTION, "Build mode (b)");

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke('b'), this);
        networkPanel.getActionMap().put(this, this);

    }
}