
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.ContinuousHopfieldDialog;

/**
 * Show input/output information.
 */
public final class NewContinuousHopfieldNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new show input/output information action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public NewContinuousHopfieldNetworkAction(final NetworkPanel networkPanel) {

        super("Continuous Hopfield");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }



    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        ContinuousHopfieldDialog dialog = new ContinuousHopfieldDialog(networkPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
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