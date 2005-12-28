
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;

import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.network.nodes.SynapseNode;

import org.simbrain.resource.ResourceManager;

import edu.umd.cs.piccolo.PNode;

/**
 * Delete selected neurons action.
 */
public final class DeleteSelectedObjects
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new delete neurons action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public DeleteSelectedObjects(final NetworkPanel networkPanel) {

        super("Delete Neuron");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Delete.gif"));

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke("BACK_SPACE"), this);
        networkPanel.getActionMap().put(this, this);

    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        for (Iterator i = networkPanel.getSelection().iterator(); i.hasNext();) {
            PNode node = (PNode) i.next();
            if (node instanceof NeuronNode) {
                networkPanel.getNetwork().deleteNeuron(((NeuronNode) node).getNeuron());
            } else if (node instanceof SynapseNode) {
                networkPanel.getNetwork().deleteWeight(((SynapseNode) node).getSynapse());
            }  else {
                networkPanel.getLayer().removeChild(node);  
            }
        }
    }
}