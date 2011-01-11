package org.simbrain.network.desktop;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.workspace.*;
import org.simbrain.workspace.gui.*;

/**
 * Version of a Neuron Node with a coupling menu.
 */
public class NeuronNodeDesktop extends NeuronNode {

    /** Reference to parent component. */
    NetworkComponent component;

    /**
     * Constructs a Neuron Node.
     *
     * @param component parent component.
     * @param netPanel network panel.
     * @param neuron logical neuron this node represents
     */
    public NeuronNodeDesktop(final NetworkComponent component, final NetworkPanel netPanel, Neuron neuron) {
        super(netPanel, neuron);
        this.component = component;
    }

    /**
     * Add coupling menu to neuron node.
     */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = super.getContextMenu();

        // Add coupling menus
        Workspace workspace = component.getWorkspace();
        if (getNetworkPanel().getSelectedNeurons().size() == 1) {
            contextMenu.addSeparator();

            PotentialProducer producer = component.getAttributeManager()
                    .createPotentialProducer(neuron, "getActivation", double.class);
            PotentialConsumer consumer = component.getAttributeManager()
                    .createPotentialConsumer(neuron, "setInputValue", double.class);

            JMenu producerMenu = new CouplingMenuProducer(
                    "Send coupling to", workspace, producer);
            contextMenu.add(producerMenu);
            JMenu consumerMenu = new CouplingMenuConsumer(
                    "Receive coupling from", workspace, consumer);
            contextMenu.add(consumerMenu);
        }
        return contextMenu;
    }

}
