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
package org.simbrain.network.gui.nodes.subnetworkNodes;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.SRNEditorDialog;
import org.simbrain.network.gui.nodes.SubnetworkNode;
import org.simbrain.network.gui.trainer.IterativeTrainingPanel;
import org.simbrain.network.gui.trainer.TrainerGuiActions;
import org.simbrain.network.subnetworks.SimpleRecurrentNetwork;
import org.simbrain.network.trainers.SRNTrainer;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.StandardDialog;

/**
 * PNode representation of a group of a srn network.
 *
 * @author jyoshimi
 */
public class SRNNetworkNode extends SubnetworkNode {

    /**
     * Create a layered network.
     *
     * @param networkPanel parent panel
     * @param group the layered network
     */
    public SRNNetworkNode(final NetworkPanel networkPanel,
        final SimpleRecurrentNetwork group) {
        super(networkPanel, group);
        setContextMenu();
    }

    @Override
    protected StandardDialog getPropertyDialog() {
        return new SRNEditorDialog(this.getNetworkPanel(),
            (SimpleRecurrentNetwork) getSubnetwork());
    }

    /**
     * Sets custom menu.
     */
    private void setContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        editAction.putValue("Name", "Edit / Train SRN Network...");
        menu.add(editAction);
        menu.add(renameAction);
        menu.add(removeAction);
        menu.addSeparator();
        menu.add(clearAction);
        menu.addSeparator();
        final SimpleRecurrentNetwork network =
            (SimpleRecurrentNetwork) getSubnetwork();
        JMenu dataActions = new JMenu("View / Edit Data");
        dataActions.add(TrainerGuiActions.getEditCombinedDataAction(
            getNetworkPanel(), network));
        dataActions.addSeparator();
        dataActions.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(),
            network.getInputNeurons(), network.getTrainingSet()
                .getInputDataMatrix(), "Input"));
        dataActions.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(),
            network.getOutputNeurons(), network.getTrainingSet()
                .getTargetDataMatrix(), "Target"));
        menu.add(dataActions);

        setContextMenu(menu);
    }

    /**
     * Action to train srn
     */
    private Action trainAction = new AbstractAction() {

        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Trainer.png"));
            putValue(NAME, "Train SRN...");
            putValue(SHORT_DESCRIPTION, "Train SRN...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            SimpleRecurrentNetwork network =
                (SimpleRecurrentNetwork) getSubnetwork();
            IterativeTrainingPanel trainingPanel = new IterativeTrainingPanel(
                getNetworkPanel(), new SRNTrainer(network));
            JDialog frame =
                getNetworkPanel().displayPanelInWindow(trainingPanel,
                    "Trainer");
            trainingPanel.setFrame(frame);
        }
    };

    /**
     * Action to clear srn nodes.
     */
    private Action clearAction = new AbstractAction() {

        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Eraser.png"));
            putValue(NAME, "Clear SRN...");
            putValue(SHORT_DESCRIPTION, "Clear SRN Nodes...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            SimpleRecurrentNetwork network =
                (SimpleRecurrentNetwork) getSubnetwork();
            network.initNetwork();
            network.getParentNetwork().fireNeuronsUpdated(
                network.getFlatNeuronList());
        }
    };

}