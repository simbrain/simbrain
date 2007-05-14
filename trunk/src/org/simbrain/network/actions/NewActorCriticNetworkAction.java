package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.ActorCriticDialog;

public class NewActorCriticNetworkAction extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /**
     * Create a new new backprop network action with the specified
     * network panel.
     *
     * @param networkPanel
     *            networkPanel, must not be null
     */
    public NewActorCriticNetworkAction(final NetworkPanel networkPanel) {

        super("Actor Critic Network");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        ActorCriticDialog dialog = new ActorCriticDialog(networkPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
