/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui.nodes;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.WeightMatrixViewer;
import org.simbrain.network.gui.dialogs.group.SynapseGroupDialog;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.StandardDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Interaction box for synapse groups.
 */
public class SynapseGroupInteractionBox extends InteractionBox {

    /**
     * Reference to underlying synapse group.
     */
    private final SynapseGroup synapseGroup;

    /**
     * Construct the custom interaction box.
     *
     * @param net          parent network panel
     * @param synapseGroup
     */
    public SynapseGroupInteractionBox(NetworkPanel net, SynapseGroup synapseGroup) {
        super(net);
        this.synapseGroup = synapseGroup;

    }

    /**
     * @return the synapse group associated with this interaction box.
     */
    public SynapseGroup getSynapseGroup() {
        return synapseGroup;
    }

    @Override
    protected JDialog getPropertyDialog() {
        return this.getNetworkPanel().getSynapseGroupDialog(this);
    }

    @Override
    protected boolean hasPropertyDialog() {
        return true;
    }

    @Override
    public boolean isDraggable() {
        return false;
    }

    @Override
    protected JPopupMenu getContextMenu() {
        return getDefaultContextMenu();
    }

    /**
     * Returns default actions for a context menu.
     *
     * @return the default context menu
     */
    protected JPopupMenu getDefaultContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        // Edit
        Action editGroup = new AbstractAction("Edit Synapse Group...") {
            public void actionPerformed(final ActionEvent event) {
                StandardDialog dialog = SynapseGroupDialog.createSynapseGroupDialog(getNetworkPanel(), synapseGroup);
                dialog.setLocationRelativeTo(null);
                dialog.pack();
                dialog.setVisible(true);
            }
        };
        menu.add(editGroup);
        menu.add(removeAction);

        // Selection stuff
        menu.addSeparator();
        Action selectSynapses = new AbstractAction("Select Synapses") {
            public void actionPerformed(final ActionEvent event) {
                selectSynapses();
            }
        };
        menu.add(selectSynapses);
        Action selectIncomingNodes = new AbstractAction("Select Incoming Neurons") {
            public void actionPerformed(final ActionEvent event) {
                List<NeuronNode> incomingNodes = new ArrayList<NeuronNode>();
                for (Neuron neuron : synapseGroup.getSourceNeurons()) {
                    incomingNodes.add((NeuronNode) getNetworkPanel().getObjectNodeMap().get(neuron));

                }
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(incomingNodes);
            }
        };
        menu.add(selectIncomingNodes);
        Action selectOutgoingNodes = new AbstractAction("Select Outgoing Neurons") {
            public void actionPerformed(final ActionEvent event) {
                List<NeuronNode> outgoingNodes = new ArrayList<NeuronNode>();
                for (Neuron neuron : synapseGroup.getTargetNeurons()) {
                    outgoingNodes.add((NeuronNode) getNetworkPanel().getObjectNodeMap().get(neuron));

                }
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(outgoingNodes);
            }
        };
        menu.add(selectOutgoingNodes);

        // Weight adjustment stuff
        menu.addSeparator();
        //Action adjustSynapses = new AbstractAction("Adjust Synapses...") {
        //    public void actionPerformed(final ActionEvent event) {
        //        selectSynapses();
        // TODO: Check after synapse group code stabilizes for more
        // efficient way.
        // final SynapseAdjustmentPanel synapsePanel =
        // SynapseAdjustmentPanel
        // .createSynapseAdjustmentPanel(getNetworkPanel(),
        // synapseGroup.getAllSynapses());
        // JDialog dialog = new JDialog();
        // dialog.setTitle("Adjust selected synapses");
        // dialog.setContentPane(synapsePanel);
        // dialog.pack();
        // dialog.setLocationRelativeTo(null);
        // dialog.setVisible(true);
        // dialog.addWindowListener(new WindowAdapter() {
        // public void windowClosing(WindowEvent e) {
        // synapsePanel.removeListeners();
        // }
        // });
        //}
        //};
        //menu.add(adjustSynapses);
        menu.add(new JMenuItem(showWeightMatrixAction));

        // Freezing actions
        menu.addSeparator();
        setFreezeActionsEnabled();
        menu.add(freezeSynapsesAction);
        menu.add(unfreezeSynapsesAction);

        // Synapse Enabling actions
        menu.addSeparator();
        setSynapseEnablingActionsEnabled();
        menu.add(enableSynapsesAction);
        menu.add(disableSynapsesAction);

        // Synapse Visibility
        menu.addSeparator();
        final JCheckBoxMenuItem tsvCheckBox = new JCheckBoxMenuItem();
        Action toggleSynapseVisibility = new AbstractAction("Toggle Synapse Visibility") {
            public void actionPerformed(final ActionEvent event) {
                if (synapseGroup.isDisplaySynapses()) {
                    synapseGroup.setDisplaySynapses(false);
                } else {
                    synapseGroup.setDisplaySynapses(true);
                }
                tsvCheckBox.setSelected(synapseGroup.isDisplaySynapses());
            }
        };
        tsvCheckBox.setAction(toggleSynapseVisibility);
        tsvCheckBox.setSelected(synapseGroup.isDisplaySynapses());
        menu.add(tsvCheckBox);

        // Coupling menu
        JMenu consumerMenu = this.getNetworkPanel().getSynapseGroupConsumerMenu(synapseGroup);
        JMenu producerMenu = getNetworkPanel().getSynapseGroupProducerMenu(synapseGroup);
        if ((consumerMenu != null) || (producerMenu != null)) {
            menu.addSeparator();
        }
        if (consumerMenu != null) {
            menu.add(consumerMenu);
        }
        if (producerMenu != null) {
            menu.add(producerMenu);
        }
        return menu;
    }

    /**
     * Select the synapses in this group.
     */
    private void selectSynapses() {
        List<SynapseNode> nodes = new ArrayList<SynapseNode>();
        for (Synapse synapse : synapseGroup.getExcitatorySynapses()) {
            nodes.add((SynapseNode) getNetworkPanel().getObjectNodeMap().get(synapse));

        }
        for (Synapse synapse : synapseGroup.getInhibitorySynapses()) {
            nodes.add((SynapseNode) getNetworkPanel().getObjectNodeMap().get(synapse));

        }
        getNetworkPanel().clearSelection();
        getNetworkPanel().setSelection(nodes);
    }

    /**
     * Action for showing the weight matrix for this neuron group.
     */
    Action showWeightMatrixAction = new AbstractAction() {

        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("grid.png"));
            putValue(NAME, "Show Weight Matrix");
            putValue(SHORT_DESCRIPTION, "Show Weight Matrix");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            List<Neuron> sourceNeurons = synapseGroup.getSourceNeurons();
            List<Neuron> targetNeurons = synapseGroup.getTargetNeurons();
            JPanel panel = WeightMatrixViewer.getWeightMatrixPanel(new WeightMatrixViewer(sourceNeurons, targetNeurons, getNetworkPanel()));
            getNetworkPanel().displayPanel(panel, "Edit weights");
        }
    };

    /**
     * Action for editing the group name.
     */
    protected Action renameAction = new AbstractAction("Rename Group...") {
        public void actionPerformed(final ActionEvent event) {
            String newName = JOptionPane.showInputDialog("Name:", synapseGroup.getLabel());
            synapseGroup.setLabel(newName);
        }
    };

    /**
     * Action for removing this group
     */
    protected Action removeAction = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("RedX_small.png"));
            putValue(NAME, "Remove Group...");
            putValue(SHORT_DESCRIPTION, "Remove synapse group...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            getNetworkPanel().getNetwork().removeGroup(synapseGroup);
        }
    };

    /**
     * Sets whether the freezing actions are enabled based on whether the
     * synapses are all frozen or not.
     * <p>
     * If all synapses are frozen already, then "freeze synapses" is disabled.
     * <p>
     * If all synapses are unfrozen already, then "unfreeze synapses" is
     * disabled.
     */
    private void setFreezeActionsEnabled() {
        try {
            freezeSynapsesAction.setEnabled(!synapseGroup.isFrozen(Polarity.BOTH));
            unfreezeSynapsesAction.setEnabled(synapseGroup.isFrozen(Polarity.BOTH));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Action for freezing synapses
     */
    protected Action freezeSynapsesAction = new AbstractAction() {

        {
            // putValue(SMALL_ICON, ResourceManager.getImageIcon("Clamp.png"));
            putValue(NAME, "Freeze Synapses");
            putValue(SHORT_DESCRIPTION, "Freeze all synapses in this group (prevent learning)");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            synapseGroup.setFrozen(true, Polarity.BOTH);
        }
    };

    /**
     * Action for unfreezing synapses
     */
    protected Action unfreezeSynapsesAction = new AbstractAction() {

        {
            // putValue(SMALL_ICON, ResourceManager.getImageIcon("Clamp.png"));
            putValue(NAME, "Unfreeze Synapses");
            putValue(SHORT_DESCRIPTION, "Unfreeze all synapses in this group (allow learning)");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            synapseGroup.setFrozen(false, Polarity.BOTH);
        }
    };

    /**
     * Sets whether the synapse-enabling actions are enabled based on whether
     * the synapses themselves are all enabled or not. Of course, "enable" means
     * two things here, (1) a property of synapses whereby the let current pass
     * or not and (2) a property of swing actions where being disabled means
     * being grayed out and unusable.
     * <p>
     * If all synapses are enabled already, then the "enable synapses" action is
     * disabled.
     * <p>
     * If all synapses are disabled already, then the "disable synapses" actions
     * is disabled.
     */
    private void setSynapseEnablingActionsEnabled() {
        enableSynapsesAction.setEnabled(!synapseGroup.isEnabled(Polarity.BOTH));
        disableSynapsesAction.setEnabled(synapseGroup.isEnabled(Polarity.BOTH));
    }

    /**
     * Action for enabling synapses
     */
    protected Action enableSynapsesAction = new AbstractAction() {

        {
            // putValue(SMALL_ICON, ResourceManager.getImageIcon("Clamp.png"));
            putValue(NAME, "Enable Synapses");
            putValue(SHORT_DESCRIPTION, "Enable all synapses in this group (allow activation " + "to pass through synapses)");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            synapseGroup.setEnabled(true, Polarity.BOTH);
        }
    };

    /**
     * Action for disabling synapses
     */
    protected Action disableSynapsesAction = new AbstractAction() {

        {
            // putValue(SMALL_ICON, ResourceManager.getImageIcon("Clamp.png"));
            putValue(NAME, "Disable Synapses");
            putValue(SHORT_DESCRIPTION, "Disable all synapses in this group (don't allow " + "activation to pass through synapses)");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            synapseGroup.setEnabled(false, Polarity.BOTH);
        }
    };

    @Override
    protected String getToolTipText() {
        return "Synapses: " + synapseGroup.size() + " Density: " + (double) synapseGroup.size() / (synapseGroup.getSourceNeuronGroup().size() * synapseGroup.getTargetNeuronGroup().size());
    }

    @Override
    protected boolean hasToolTipText() {
        return true;
    }

}
