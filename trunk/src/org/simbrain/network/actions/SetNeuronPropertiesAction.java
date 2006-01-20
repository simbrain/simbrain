
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;

/**
 * Set neuron properties.
 */
public final class SetNeuronPropertiesAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new set neuron properties action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public SetNeuronPropertiesAction(final NetworkPanel networkPanel) {

        super("Neuron Properties...");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        updateAction();

        // add a selection listener to update state based on selection
        networkPanel.addSelectionListener(new NetworkSelectionListener() {

                /** @see NetworkSelectionListener */
                public void selectionChanged(final NetworkSelectionEvent event) {
                    updateAction();
                }
            });
    }

    /**
     * Set action text based on number of selected neurons.
     */
    private void updateAction() {
        int numNeurons = networkPanel.getSelectedNeurons().size();

        if (numNeurons > 0) {
            String text = new String(("Set " + numNeurons + ((numNeurons > 1) ? " Selected Neurons" : " Selected Neuron")));
            putValue(NAME, text);
            setEnabled(true);
        } else {
            putValue(NAME, "Set Selected Neuron(s)");
            setEnabled(false);
        }
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        networkPanel.showSelectedNeuronProperties();

    }
}