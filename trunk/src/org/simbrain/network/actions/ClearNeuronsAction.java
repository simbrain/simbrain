
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.resource.ResourceManager;

/**
 * Clear selected neurons action.
 */
public final class ClearNeuronsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new clear selected neurons action with the
     * specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public ClearNeuronsAction(final NetworkPanel networkPanel) {
        super("Clear selected neurons");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Eraser.gif"));
        putValue(SHORT_DESCRIPTION, "Zero selected Nodes (c)");

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke('c'), this);
        networkPanel.getActionMap().put(this, this);
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        for (Iterator i = networkPanel.getSelectedNeurons().iterator(); i.hasNext();) {
            NeuronNode node = (NeuronNode) i.next();
            node.getNeuron().setActivation(0);
            node.update();
        }
    }
}