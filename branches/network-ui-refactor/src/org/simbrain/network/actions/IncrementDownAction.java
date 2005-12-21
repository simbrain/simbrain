
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;
import org.simbrain.network.nodes.NeuronNode;

/**
 * Increment selected neurons and weights downwards action.
 */
public final class IncrementDownAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new increment down action with the specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public IncrementDownAction(final NetworkPanel networkPanel) {
        super("Increment selected neurons and weights downwards");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), this);
        networkPanel.getActionMap().put(this, this);
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        for (Iterator i = networkPanel.getSelectedNeurons().iterator(); i.hasNext();) {
            NeuronNode node = (NeuronNode) i.next();
            node.getNeuron().decrementActivation();
            node.update();
        }
    }
}