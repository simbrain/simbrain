package org.simbrain.network.gui.actions.neuronarray;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.NeuronArray;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class AddNeuronArrayAction extends AbstractAction {


    /**
     * Network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Create a new neuron action with the specified network panel.
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

    /**
     * @param event
     * @see AbstractAction
     */
    public void actionPerformed(final ActionEvent event) {
        networkPanel.showNeuronArrayCreationDialog();
    }

}
