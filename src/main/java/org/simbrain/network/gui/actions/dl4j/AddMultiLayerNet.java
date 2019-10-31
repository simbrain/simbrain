package org.simbrain.network.gui.actions.dl4j;

import org.simbrain.network.gui.NetworkPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class AddMultiLayerNet extends AbstractAction {


    /**
     * Network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Create a new DL4J Multilayer net to the specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public AddMultiLayerNet(final NetworkPanel networkPanel) {
        super("Add Multilayer Network...");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        putValue(SHORT_DESCRIPTION, "Add a Dl4J Multi Layer Network to the network");

    }

    @Override
    public void actionPerformed(final ActionEvent event) {
        networkPanel.showMultiLayerNetworkCreationDialog();
    }

}
