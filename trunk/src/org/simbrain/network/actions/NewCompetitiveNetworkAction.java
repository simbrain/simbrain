
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.CompetitiveDialog;
import org.simnet.networks.Competitive;

/**
 * Show input/output information.
 */
public final class NewCompetitiveNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new show input/output information action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public NewCompetitiveNetworkAction(final NetworkPanel networkPanel) {

        super("Competitive Network");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        CompetitiveDialog dialog = new CompetitiveDialog(networkPanel);
        dialog.pack();
        dialog.setVisible(true);

//        if (!dialog.hasUserCancelled()) {
//            Competitive compNet = new Competitive(dialog.getNumberOfNeurons());
//            compNet.setEpsilon(dialog.getEpsilon());
//            this.addNetwork(compNet, "Line");
//       }
//       renderObjects();
    }
}