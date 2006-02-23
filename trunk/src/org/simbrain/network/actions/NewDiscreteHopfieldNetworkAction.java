
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.DiscreteHopfieldDialog;

/**
 * Show input/output information.
 */
public final class NewDiscreteHopfieldNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new show input/output information action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public NewDiscreteHopfieldNetworkAction(final NetworkPanel networkPanel) {

        super("Discrete Hopfield");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }



    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        DiscreteHopfieldDialog dialog = new DiscreteHopfieldDialog(networkPanel);
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