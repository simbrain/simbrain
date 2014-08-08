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
package org.simbrain.network.gui.nodes.subnetworkNodes;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.BackpropEditorDialog;
import org.simbrain.network.gui.dialogs.network.ESNTrainingDialog;
import org.simbrain.network.gui.nodes.SubnetworkNode;
import org.simbrain.network.gui.trainer.TrainerGuiActions;
import org.simbrain.network.gui.trainer.subnetworkTrainingPanels.ESNOfflineTrainingPanel;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.network.subnetworks.EchoStateNetwork;
import org.simbrain.network.trainers.TrainingSet;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.genericframe.GenericJDialog;

/**
 * PNode representation of an Echo State Network.
 */
public class ESNNetworkNode extends SubnetworkNode {

    /**
     * Create an ESN network.
     *
     * @param networkPanel
     *            parent panel
     * @param group
     *            the ESN
     */
    public ESNNetworkNode(NetworkPanel networkPanel, EchoStateNetwork group) {
        super(networkPanel, group);
        setContextMenu();
    }

    /**
     * Sets custom menu.
     */
    private void setContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        editAction.putValue("Name", "Edit / Train ESN...");
        menu.add(editAction);
        menu.add(renameAction);
        menu.add(removeAction);
        menu.addSeparator();
        final EchoStateNetwork esn = (EchoStateNetwork) getSubnetwork();
        final TrainingSet trainingSet = new TrainingSet(esn.getInputData(),
            esn.getTargetData());
        JMenu dataActions = new JMenu("View / Edit Data");
        dataActions.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(),
            esn.getInputLayer().getNeuronList(),
            trainingSet.getInputDataMatrix(), "Input"));
        dataActions.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(),
            esn.getOutputLayer().getNeuronList(),
            trainingSet.getTargetDataMatrix(), "Target"));
        menu.add(dataActions);
        setContextMenu(menu);
    }

    @Override
    protected StandardDialog getPropertyDialog() {
        return new ESNTrainingDialog(getNetworkPanel(),
                (EchoStateNetwork) getSubnetwork());
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
            GenericJDialog frame = new GenericJDialog();
            frame.setTitle("Trainer");
            EchoStateNetwork network = (EchoStateNetwork) getSubnetwork();
            ESNOfflineTrainingPanel trainingPanel =
                new ESNOfflineTrainingPanel(
                    getNetworkPanel(), network, frame);
            frame.setContentPane(trainingPanel);
            frame.setVisible(true);
            frame.pack();
        }
    };

}
