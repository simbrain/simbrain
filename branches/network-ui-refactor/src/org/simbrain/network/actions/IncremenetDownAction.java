
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;
import org.simbrain.network.nodes.NeuronNode;

import org.simbrain.resource.ResourceManager;

/**
 * Incremenet selected neurons and weights downwards.
 */
public final class IncremenetDownAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create an action with the specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public IncremenetDownAction(final NetworkPanel networkPanel) {
        super("Clear selected neurons");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Eraser.gif"));

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