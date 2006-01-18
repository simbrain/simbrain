
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.HopfieldDialog;
import org.simnet.networks.ContinuousHopfield;
import org.simnet.networks.DiscreteHopfield;

/**
 * Show input/output information.
 */
public final class NewHopfieldNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new show input/output information action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public NewHopfieldNetworkAction(final NetworkPanel networkPanel) {

        super("Hopfield Network");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        HopfieldDialog dialog = new HopfieldDialog();
        dialog.pack();
        dialog.setVisible(true);

//        if (!dialog.hasUserCancelled()) {
//            if (dialog.getType() == HopfieldDialog.DISCRETE) {
//                DiscreteHopfield hop = new DiscreteHopfield(dialog
//                        .getNumUnits());
//                this.addNetwork(hop, dialog.getCurrentLayout());
//            } else if (dialog.getType() == HopfieldDialog.CONTINUOUS) {
//                ContinuousHopfield hop = new ContinuousHopfield(dialog
//                        .getNumUnits());
//                this.addNetwork(hop, dialog.getCurrentLayout());
//            }
//        }
//
//        repaint();

    }
}