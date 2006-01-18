
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.WTADialog;
import org.simnet.networks.WinnerTakeAll;

/**
 * Show input/output information.
 */
public final class NewWTANetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new show input/output information action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public NewWTANetworkAction(final NetworkPanel networkPanel) {

        super("Winner Take All Network");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        WTADialog dialog = new WTADialog(networkPanel);
        dialog.pack();
        dialog.setVisible(true);

//        if (!dialog.hasUserCancelled()) {
//            WinnerTakeAll wta = new WinnerTakeAll(dialog.getNumUnits());
//            this.addNetwork(wta, dialog.getCurrentLayout());
//        }
//
//        renderObjects();

    }
}