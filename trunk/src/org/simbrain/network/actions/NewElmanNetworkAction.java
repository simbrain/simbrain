
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.ElmanDialog;
import org.simnet.networks.Elman;

/**
 * Show input/output information.
 */
public final class NewElmanNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new show input/output information action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public NewElmanNetworkAction(final NetworkPanel networkPanel) {

        super("Elman Network");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        ElmanDialog dialog = new ElmanDialog(networkPanel);
        dialog.pack();
        dialog.setVisible(true);

//        if (!dialog.hasUserCancelled()) {
//            Elman net = new Elman();
//            net.setNInputs(dialog.getNumInputs());
//            net.setNHidden(dialog.getNumHidden());
//            net.setNOutputs(dialog.getNumInputs());
//            net.defaultInit();
//            this.addNetwork(net, "Elman");
//        }
//
//        renderObjects();

    }
}