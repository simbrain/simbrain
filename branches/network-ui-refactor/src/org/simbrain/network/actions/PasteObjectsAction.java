
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.Clipboard;
import org.simbrain.network.NetworkPanel;

import edu.umd.cs.piccolo.PNode;

/**
 * Paste objects action.
 */
public final class PasteObjectsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new paste objects action with the
     * specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public PasteObjectsAction(final NetworkPanel networkPanel) {
        super("Paste");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke("Control V"), this);
        networkPanel.getActionMap().put(this, this);
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        Clipboard.paste(networkPanel);
        networkPanel.setNumberOfPastes(networkPanel.getNumberOfPastes() + 1);
    }

}