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
package org.simbrain.network.gui.nodes.groupNodes;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.gui.trainer.IterativeTrainingPanel;
import org.simbrain.network.gui.trainer.LMSOfflineTrainingPanel;
import org.simbrain.network.gui.trainer.TrainerGuiActions;
import org.simbrain.network.subnetworks.LMSNetwork;
import org.simbrain.network.trainers.LMSIterative;
import org.simbrain.network.trainers.LMSOffline;
import org.simbrain.network.trainers.Trainable;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.NumericMatrix;
import org.simbrain.util.genericframe.GenericFrame;

/**
 * PNode representation of a group of a LMS network.
 *
 * @author jyoshimi
 */
public class LMSNetworkNode extends SubnetworkNode {

    /**
     * Create a layered network.
     *
     * @param networkPanel parent panel
     * @param group the layered network
     */
    public LMSNetworkNode(NetworkPanel networkPanel, LMSNetwork group) {
        super(networkPanel, group);
        setContextMenu();
    }

    /**
     * Sets custom menu.
     */
    private void setContextMenu() {
        JPopupMenu menu = super.getDefaultContextMenu();
        menu.addSeparator();
        menu.add(new JMenuItem(trainIterativelyAction));
        menu.add(new JMenuItem(trainOfflineAction));
        menu.addSeparator();
        JMenu dataActions = new JMenu("View / Edit Data");

        final LMSNetwork lms = (LMSNetwork) getGroup();

        // Reference to the input data in the LMS
        NumericMatrix inputData = new NumericMatrix() {
            @Override
            public void setData(double[][] data) {
                lms.getTrainingSet().setInputData(data);
            }

            @Override
            public double[][] getData() {
                return lms.getTrainingSet().getInputData();
            }

        };
        // Reference to the training data in the LMS
        NumericMatrix trainingData = new NumericMatrix() {
            @Override
            public void setData(double[][] data) {
                lms.getTrainingSet().setTargetData(data);
            }

            @Override
            public double[][] getData() {
                return lms.getTrainingSet().getTargetData();
            }

        };
        dataActions.add(TrainerGuiActions.getEditCombinedDataAction(getNetworkPanel(),
                (Trainable) getGroup()));
        dataActions.addSeparator();
        dataActions.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(),
                lms.getInputNeurons(), inputData, "Input"));
        dataActions.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(),
                lms.getOutputNeurons(), trainingData, "Target"));
        menu.add(dataActions);
        setContextMenu(menu);
    }

    /**
     * Action to train LMS Iteratively
     */
    Action trainIterativelyAction = new AbstractAction() {

        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Trainer.png"));
            putValue(NAME, "Train iteratively...");
            putValue(SHORT_DESCRIPTION, "Train iteratively...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            LMSNetwork network = (LMSNetwork) getGroup();
            IterativeTrainingPanel trainingPanel = new IterativeTrainingPanel(
                    getNetworkPanel(), new LMSIterative(network));
            GenericFrame frame = getNetworkPanel().displayPanel(trainingPanel,
                    "Trainer");
            trainingPanel.setFrame(frame);
        }
    };

    /**
     * Action to train LMS Offline
     */
    Action trainOfflineAction = new AbstractAction() {

        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Trainer.png"));
            putValue(NAME, "Train offline...");
            putValue(SHORT_DESCRIPTION, "Train offline...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            LMSNetwork network = (LMSNetwork) getGroup();
            LMSOfflineTrainingPanel trainingPanel = new LMSOfflineTrainingPanel(
                    getNetworkPanel(), new LMSOffline(network));
            GenericFrame frame = getNetworkPanel().displayPanel(trainingPanel,
                    "Trainer");
            trainingPanel.setFrame(frame);
        }
    };

}
