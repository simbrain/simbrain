
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.BuildMode;
import org.simbrain.network.NetworkPanel;

/**
 * Build mode action.
 */
class BuildModeAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /** Build mode. */
    private final BuildMode buildMode;


    /**
     * Create a new build mode action with the specified name,
     * network panel, and build mode.
     *
     * @param name name
     * @param networkPanel network panel, must not be null
     * @param buildMode build mode, must not be null
     */
    BuildModeAction(final String name,
                    final NetworkPanel networkPanel,
                    final BuildMode buildMode) {

        super(name);

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }
        if (buildMode == null) {
            throw new IllegalArgumentException("buildMode must not be null");
        }

        this.networkPanel = networkPanel;
        this.buildMode = buildMode;
    }


    /** @see AbstractAction */
    public final void actionPerformed(final ActionEvent event) {
        networkPanel.setBuildMode(buildMode);
    }
}