
package org.simbrain.network.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;

/**
 * Select all action.
 */
public final class SelectAllAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new select all action.
     *
     * @param networkPanel network panel, must not be null
     */
    public SelectAllAction(final NetworkPanel networkPanel) {
        super("Select all");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        this.putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        networkPanel.getInputMap().put(KeyStroke.getKeyStroke('a'), this);
        networkPanel.getActionMap().put(this, this);
   }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        networkPanel.selectAll();
    }
}