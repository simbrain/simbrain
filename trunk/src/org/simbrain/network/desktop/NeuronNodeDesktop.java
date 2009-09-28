package org.simbrain.network.desktop;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.ConsumingAttributeMenu;
import org.simbrain.workspace.gui.ProducingAttributeMenu;

/**
 * Version of a Neuron Node with a coupling menu.
 */
public class NeuronNodeDesktop extends NeuronNode {

    /** Reference to parent component. */
    NetworkDesktopComponent component;

    /**
     * Constructs a Neuron Node.
     *
     * @param component parent component.
     * @param netPanel network panel.
     * @param neuron logical neuron this node represents
     */
    public NeuronNodeDesktop(final NetworkDesktopComponent component, final NetworkPanel netPanel, Neuron neuron) {
        super(netPanel, neuron);
        this.component = component;
    }

    /**
     * Add coupling menu to neuron node.
     */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = super.getContextMenu();
        // Add coupling menus
        Workspace workspace = component.getWorkspaceComponent().getWorkspace();
        if (getNetworkPanel().getSelectedNeurons().size() == 1) {
            contextMenu.addSeparator();
            JMenu producerMenu = new ProducingAttributeMenu(
                    "Receive coupling from", workspace, component
                            .getWorkspaceComponent().findConsumingActivationAttribute(neuron));
               contextMenu.add(producerMenu);
               JMenu consumerMenu = new ConsumingAttributeMenu(
                       "Send coupling to", workspace, component
                               .getWorkspaceComponent().findProducingActivationAttribute(neuron));
                  contextMenu.add(consumerMenu);
        }
        return contextMenu;
    }

}
