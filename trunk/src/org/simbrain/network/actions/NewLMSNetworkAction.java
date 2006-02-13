
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.LMSDialog;

/**
 * Show input/output information.
 */
public final class NewLMSNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new show input/output information action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public NewLMSNetworkAction(final NetworkPanel networkPanel) {

        super("LMS Network");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        LMSDialog dialog = new LMSDialog(networkPanel);
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