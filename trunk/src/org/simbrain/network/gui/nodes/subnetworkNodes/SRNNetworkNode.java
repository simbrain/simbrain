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
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.gui.nodes.SubnetworkNode;
import org.simbrain.network.gui.trainer.IterativeTrainingPanel;
import org.simbrain.network.gui.trainer.TrainerGuiActions;
import org.simbrain.network.subnetworks.SimpleRecurrentNetwork;
import org.simbrain.network.trainers.SRNTrainer;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.genericframe.GenericFrame;

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
        //setInteractionBox(new SRNInteractionBox(networkPanel));
        setContextMenu();

    }

//    /**
//     * Custom interaction box for SRN's.
//     */
//    private class SRNInteractionBox extends InteractionBox {
//        public SRNInteractionBox(NetworkPanel net) {
//            super(net, SRNNetworkNode.this);
//        }
//
//        // @Override
//        // protected JDialog getPropertyDialog() {
//        // TrainerPanel panel = new TrainerPanel(getNetworkPanel(),
//        // getTrainer());
//        // JDialog dialog = new JDialog();
//        // dialog.setContentPane(panel);
//        // return dialog;
//        // }
//        //
//        // @Override
//        // protected boolean hasPropertyDialog() {
//        // return true;
//        // }
//
//        @Override
//        protected String getToolTipText() {
//            return "SRN...";
//        }
//
//        @Override
//        protected boolean hasToolTipText() {
//            return true;
//        }
//
//    };

    /**
     * Sets custom menu.
     */
    private void setContextMenu() {
        final SimpleRecurrentNetwork network = (SimpleRecurrentNetwork) getSubnetwork();
        JPopupMenu menu = super.getDefaultContextMenu();
        menu.addSeparator();
        menu.add(new JMenuItem(trainAction));
        menu.addSeparator();
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
            SimpleRecurrentNetwork network = (SimpleRecurrentNetwork) getSubnetwork();
            IterativeTrainingPanel trainingPanel = new IterativeTrainingPanel(
                    getNetworkPanel(), new SRNTrainer(network));
            GenericFrame frame = getNetworkPanel().displayPanel(trainingPanel,
                    "Trainer");
            trainingPanel.setFrame(frame);
        }
    };

}