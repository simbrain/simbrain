
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.StandardNetworkDialog;

/**
 * Show input/output information.
 */
public final class NewStandardNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new show input/output information action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public NewStandardNetworkAction(final NetworkPanel networkPanel) {

        super("Standard Network");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        StandardNetworkDialog dialog = new StandardNetworkDialog(networkPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

//        if (!dialog.hasUserCancelled()) {
//            Competitive compNet = new Competitive(dialog.getNumberOfNeurons());
//            compNet.setEpsilon(dialog.getEpsilon());
//            this.addNetwork(compNet, "Line");
//       }
//       renderObjects();
    }
}