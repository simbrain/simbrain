package org.simbrain.network.gui.actions.dl4j;

import org.simbrain.network.gui.NetworkPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class AddNeuronArrayAction extends AbstractAction {


    /**
     * Network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Create a new neuron array with the specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public AddNeuronArrayAction(final NetworkPanel networkPanel) {
        super("Add Neuron Array...");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        putValue(SHORT_DESCRIPTION, "Add a neuron array to the network");

    }

    @Override
    public void actionPerformed(final ActionEvent event) {
        networkPanel.showNeuronArrayCreationDialog();
    }

}
