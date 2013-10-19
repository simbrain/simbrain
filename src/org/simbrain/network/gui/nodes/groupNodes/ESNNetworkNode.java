/*
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
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.gui.trainer.ESNOfflineTrainingPanel;
import org.simbrain.network.gui.trainer.TrainerGuiActions;
import org.simbrain.network.subnetworks.EchoStateNetwork;
import org.simbrain.network.trainers.Trainable;
import org.simbrain.network.trainers.TrainingSet;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.NumericMatrix;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.propertyeditor.ReflectivePropertyEditor;

/**
 * PNode representation of an Echo State Network.
 */
public class ESNNetworkNode extends SubnetworkNode {

    /**
     * Create an ESN network.
     *
     * @param networkPanel parent panel
     * @param group the ESN
     */
    public ESNNetworkNode(NetworkPanel networkPanel, EchoStateNetwork group) {
        super(networkPanel, group);
        setContextMenu();
    }

    /**
     * Sets custom menu.
     */
    private void setContextMenu() {
        JPopupMenu menu = super.getDefaultContextMenu();
        menu.addSeparator();
        menu.add(new JMenuItem(trainOfflineAction));
        menu.addSeparator();

        final EchoStateNetwork esn = (EchoStateNetwork) getGroup();
        final TrainingSet trainingSet =  new TrainingSet(esn.getInputData(), esn
                .getTargetData());

        JMenu dataActions = new JMenu("View / Edit Data");
        dataActions.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(), esn
                .getInputLayer().getNeuronList(), trainingSet
                .getInputDataMatrix(), "Input"));
        dataActions.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(), esn
                .getOutputLayer().getNeuronList(), trainingSet
                .getTargetDataMatrix(), "Target"));
        menu.add(dataActions);
        setContextMenu(menu);
    }

    /**
     * Action to train ESN Offline
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
            EchoStateNetwork network = (EchoStateNetwork) getGroup();
            ESNOfflineTrainingPanel trainingPanel = new ESNOfflineTrainingPanel(
                    getNetworkPanel(), network);
            GenericFrame frame  = getNetworkPanel().displayPanel(
                    trainingPanel, "Trainer");
            trainingPanel.setFrame(frame);
        }
    };

}
