
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.BackpropDialog;
import org.simnet.networks.Backprop;

/**
 * Show input/output information.
 */
public final class NewBackpropNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new show input/output information action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public NewBackpropNetworkAction(final NetworkPanel networkPanel) {

        super("Backprop Network");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        BackpropDialog dialog = new BackpropDialog(networkPanel);
        dialog.pack();
        dialog.setVisible(true);

//        if (!dialog.hasUserCancelled()) {
//            Backprop bp = new Backprop();
//            bp.setNInputs(dialog.getNumInputs());
//            bp.setNHidden(dialog.getNumHidden());
//            bp.setNOutputs(dialog.getNumOutputs());
//            bp.defaultInit();
//            this.addNetwork(bp, "Layers");
//        }
//
//        renderObjects();
    }
}