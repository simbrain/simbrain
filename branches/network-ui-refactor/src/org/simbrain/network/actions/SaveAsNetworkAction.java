
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Save as network action.
 */
public final class SaveAsNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new save as network action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public SaveAsNetworkAction(final NetworkPanel networkPanel) {

        super("Save As...");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        putValue(SMALL_ICON, ResourceManager.getImageIcon("SaveAs.gif"));

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        networkPanel.showSaveFileDialog();
    }
}