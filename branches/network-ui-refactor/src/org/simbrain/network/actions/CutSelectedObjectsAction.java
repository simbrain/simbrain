
package org.simbrain.network.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.simbrain.network.NetworkFrame;
import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.Utils;

/**
 * Show help action, opens help file <code>Network.html</code>
 * in an external web browser.
 */
public final class CutSelectedObjectsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new show network preferences action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public CutSelectedObjectsAction(final NetworkPanel networkPanel) {

        super("Cut");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        putValue(SMALL_ICON, ResourceManager.getImageIcon("Cut.gif"));

        this.putValue(this.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        networkPanel.cutSelectedObjects();

    }
}