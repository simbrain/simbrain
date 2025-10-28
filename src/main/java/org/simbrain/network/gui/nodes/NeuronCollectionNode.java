package org.simbrain.network.gui.nodes;

import org.simbrain.network.core.AbstractNeuronCollection;
import org.simbrain.network.core.NeuronCollection;
import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.workspace.gui.SimbrainDesktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static org.simbrain.network.gui.NetworkPanelMenusKt.createCouplingMenu;

/**
 * PNode representation of a {@link NeuronCollection}.
 *
 * @author Jeff Yoshimi
 */
@SuppressWarnings("serial")
public class NeuronCollectionNode extends AbstractNeuronCollectionNode {

    /**
     * Reference to represented neuron collection
     */
    private final NeuronCollection neuronCollection;

    /**
     * Create a Neuron Group PNode.
     *
     * @param networkPanel parent panel
     * @param nc           the neuron collection
     */
    public NeuronCollectionNode(NetworkPanel networkPanel, NeuronCollection nc) {

        super(networkPanel, nc);
        this.neuronCollection = nc;

        NeuronCollectionInteractionBox interactionBox = new NeuronCollectionInteractionBox(networkPanel);
        interactionBox.setText(nc.getDisplayName());
        setInteractionBox(interactionBox);
    }

    /**
     * Sync all neuron nodes in the group to the model.
     */
    public void pullPositionFromModel() {
        for (NeuronNode neuronNode : getNeuronNodes()) {
            neuronNode.pullViewPositionFromModel();
        }
    }

    @Override
    public void offset(double dx, double dy) {
        super.offset(dx, dy);
    }

    @Override
    public AbstractNeuronCollection getModel() {
        return neuronCollection;
    }

    /**
     * Helper class to create the neuron group property dialog (since it is needed in two places.).
     *
     * @return the neuron group property dialog.
     */
    public StandardDialog getPropertyDialog() {
        return null;
    }

    /**
     * Returns default actions for a context menu.
     *
     * @return the default context menu
     */
    public JPopupMenu getNCContexMenu() {

        JPopupMenu menu = new JPopupMenu();

        menu.add(renameAction);
        menu.add(removeAction);

        menu.addSeparator();
        menu.add(getNetworkPanel().getNetworkActions().showApplyLayoutDialogAction(neuronCollection));

        // Selection submenu
        menu.addSeparator();
        Action selectNeurons = new AbstractAction("Select neurons") {
            {
                // Main key binding is in Keybindings.kt. This is here just to force the binding to show in UI.
                putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, 0));
            }

            @Override
            public void actionPerformed(final ActionEvent event) {
                selectNeurons();
            }
        };
        menu.add(selectNeurons);
        Action editNeurons = new AbstractAction("Edit neurons...") {

            @Override
            public void actionPerformed(final ActionEvent event) {
                editNeurons();
            }
        };
        menu.add(editNeurons);
        menu.addSeparator();
        Action selectIncomingNodes = new AbstractAction("Select Incoming Synapses") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                getNetworkPanel().getSelectionManager().clear();
                neuronCollection.getIncomingWeights().forEach(Synapse::select);
            }
        };
        menu.add(selectIncomingNodes);
        Action selectOutgoingNodes = new AbstractAction("Select Outgoing Synapses") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                getNetworkPanel().getSelectionManager().clear();
                neuronCollection.getOutgoingWeights().forEach(Synapse::select);
            }
        };
        menu.add(selectOutgoingNodes);

        // Connect neuron connections
        menu.addSeparator();
        menu.add(getNetworkPanel().getNetworkActions().getConnectWithWeightMatrix());
        menu.add(getNetworkPanel().getNetworkActions().getConnectWithSynapseGroup());

        menu.addSeparator();
        Action createSupervisedModel = getNetworkPanel().getNetworkActions().getCreateSupervisedModelAction();
        menu.add(createSupervisedModel);
        Action testInputs = getNetworkPanel().getNetworkActions().createTestInputPanelAction(neuronCollection);
        menu.add(testInputs);
        Action addActivationToInput = getNetworkPanel().getNetworkActions().createAddActivationToInputAction(neuronCollection);
        menu.add(addActivationToInput);

        // Clamping actions
        menu.addSeparator();
        setClampActionsEnabled();
        menu.add(clampNeuronsAction);
        menu.add(unclampNeuronsAction);

        menu.addSeparator();

        // Projection Plot Action
        var plotAction = SimbrainDesktop.INSTANCE.getActionManager().createCoupledPlotMenu(
                SimbrainDesktop.INSTANCE.getWorkspace().getCouplingManager().getProducer(neuronCollection, "getActivationArray"),
                neuronCollection.getDisplayName() + " Activations",
                "Plot"
        );
        menu.add(plotAction);
        if (neuronCollection.getNeuronList().stream().findFirst().stream().anyMatch(it -> it.getUpdateRule() instanceof SpikingNeuronUpdateRule<?, ?>)) {
            plotAction.addSeparator();
            plotAction.add(
                    SimbrainDesktop.INSTANCE.getActionManager().createCoupledRasterPlotAction(
                            SimbrainDesktop.INSTANCE.getWorkspace().getCouplingManager().getProducer(neuronCollection, "getSpikes"),
                            neuronCollection.getDisplayName() + " Spikes"
                    )
            );
        }
        menu.add(getNetworkPanel().getNetworkActions().createAbstractNeuronCollectionCoupledImageWorld(neuronCollection));
        menu.add(getNetworkPanel().getNetworkActions().createRecordActivationAction(neuronCollection));

        // Coupling menu
        menu.addSeparator();
        JMenu couplingMenu = createCouplingMenu(getNetworkPanel().getNetworkComponent(), neuronCollection);
        if (couplingMenu != null) {
            menu.add(couplingMenu);
        }

        return menu;
    }

    public NeuronCollection getNeuronCollection() {
        return neuronCollection;
    }

    /**
     * Custom interaction box for Neuron Collections.
     */
    public class NeuronCollectionInteractionBox extends InteractionBox {

        /**
         * Color for the neuron collection interaction box
         */
        private final Color BOX_COLOR = new Color(209, 255, 204);

        /**
         * Construct the interaction box
         */
        public NeuronCollectionInteractionBox(NetworkPanel net) {
            super(net);
            setPaint(BOX_COLOR);
            //setTransparency(.2f);
        }

        @Override
        public StandardDialog getPropertyDialog() {
            return NeuronCollectionNode.this.getPropertyDialog();
        }

        @Override
        public NeuronCollection getModel() {
            return NeuronCollectionNode.this.getNeuronCollection();
        }

        @Override
        public JPopupMenu getContextMenu() {
            return getNCContexMenu();
        }

    }

    /**
     * Action for removing this group.
     */
    protected Action removeAction = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getSmallIcon("menu_icons/minus.png"));
            putValue(NAME, "Remove Neuron Collection.");
            putValue(SHORT_DESCRIPTION, "Remove neuron collection.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            neuronCollection.deleteBlocking();
        }
    };

    /**
     * Sets whether the clamping actions are enabled based on whether the neurons are all clamped or not.
     * <p>
     * If all neurons are clamped already, then "clamp neurons" is disabled.
     * <p>
     * If all neurons are unclamped already, then "unclamp neurons" is disabled.
     */
    private void setClampActionsEnabled() {
        clampNeuronsAction.setEnabled(!neuronCollection.isAllClamped());
        unclampNeuronsAction.setEnabled(!neuronCollection.isAllUnclamped());
    }

    /**
     * Action for clamping neurons.
     */
    protected Action clampNeuronsAction = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getSmallIcon("menu_icons/Clamp.png"));
            putValue(NAME, "Clamp Neurons");
            putValue(SHORT_DESCRIPTION, "Clamp all neurons in this group.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            neuronCollection.setClamped(true);
        }
    };

    /**
     * Action for unclamping neurons.
     */
    protected Action unclampNeuronsAction = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getSmallIcon("menu_icons/Clamp.png"));
            putValue(NAME, "Unclamp Neurons");
            putValue(SHORT_DESCRIPTION, "Unclamp all neurons in this group.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            neuronCollection.setClamped(false);
        }
    };

    @Override
    public boolean acceptsSourceHandle() {
        return true;
    }

}
