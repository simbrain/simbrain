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

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.SubnetworkNode;
import org.simbrain.network.gui.trainer.IterativeTrainingPanel;
import org.simbrain.network.gui.trainer.TrainerGuiActions;
import org.simbrain.network.subnetworks.BPTTNetwork;
import org.simbrain.network.trainers.BPTTTrainer;
import org.simbrain.util.ResourceManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * PNode representation of a group of a BPTT network.
 *
 * @author jyoshimi
 */
public class BPTTNode extends SubnetworkNode {

    /**
     * Create a layered network.
     *
     * @param networkPanel parent panel
     * @param group        the layered network
     */
    public BPTTNode(final NetworkPanel networkPanel, final BPTTNetwork group) {
        super(networkPanel, group);
        //setInteractionBox(new BackpropInteractionBox(networkPanel));
        setContextMenu();

    }

    /**
     * Sets custom menu.
     */
    private void setContextMenu() {
        final BPTTNetwork network = (BPTTNetwork) getSubnetwork();
        JPopupMenu menu = super.getDefaultContextMenu();
        menu.addSeparator();
        menu.add(new JMenuItem(trainAction));
        menu.addSeparator();
        JMenu dataActions = new JMenu("View / Edit Data");
        dataActions.add(TrainerGuiActions.getEditCombinedDataAction(getNetworkPanel(), network));
        dataActions.addSeparator();
        dataActions.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(), network.getInputNeurons(), network.getTrainingSet().getInputDataMatrix(), "Input"));
        dataActions.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(), network.getOutputNeurons(), network.getTrainingSet().getTargetDataMatrix(), "Target"));
        menu.add(dataActions);

        setContextMenu(menu);
    }

    /**
     * Action to train Backprop
     */
    private Action trainAction = new AbstractAction() {

        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Trainer.png"));
            putValue(NAME, "Train using backprop through time...");
            putValue(SHORT_DESCRIPTION, "Train using backprop through time...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            BPTTNetwork network = (BPTTNetwork) getSubnetwork();
            IterativeTrainingPanel trainingPanel = new IterativeTrainingPanel(getNetworkPanel(), new BPTTTrainer(network));
            JDialog frame = getNetworkPanel().displayPanelInWindow(trainingPanel, "Trainer");
            trainingPanel.setFrame(frame);
        }
    };

}