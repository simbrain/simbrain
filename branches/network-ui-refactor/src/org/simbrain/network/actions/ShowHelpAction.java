
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;

/**
 * Show help page for network component.
 */
public final class ShowHelpAction
    extends AbstractAction {

    /**
     * Create a new show help action.
     *
     * @param networkPanel network panel, must not be null
     */
    public ShowHelpAction() {
        super("Help");
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        org.simbrain.util.Utils.showQuickRef("Network.html");
    }
}