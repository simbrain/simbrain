
package org.simbrain.network.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Save network action.
 */
public final class SaveNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new save network action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public SaveNetworkAction(final NetworkPanel networkPanel) {

        super("Save");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        putValue(SMALL_ICON, ResourceManager.getImageIcon("Save.gif"));

        this.putValue(this.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        networkPanel.saveCurrentNetwork();
    }
}