
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;
import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.network.nodes.SynapseNode;

/**
 * Increment selected neurons and weights upward action.
 */
public final class IncrementUpAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new increment upward action with the specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public IncrementUpAction(final NetworkPanel networkPanel) {
        super("Clear selected neurons");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke("UP"), this);
        networkPanel.getActionMap().put(this, this);

    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        for (Iterator i = networkPanel.getSelectedNeurons().iterator(); i.hasNext();) {
            NeuronNode node = (NeuronNode) i.next();
            node.getNeuron().incrementActivation();
            node.update();
        }
        for (Iterator i = networkPanel.getSelectedSynapses().iterator(); i.hasNext();) {
            SynapseNode node = (SynapseNode) i.next();
            node.getSynapse().incrementWeight();
            node.updateColor();
            node.updateDiameter();
        }
    }
}