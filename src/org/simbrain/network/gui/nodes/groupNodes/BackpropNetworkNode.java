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
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.gui.trainer.BackpropTrainerPanel;
import org.simbrain.network.gui.trainer.DataViewer.DataHolder;
import org.simbrain.network.gui.trainer.TrainerGuiActions;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.network.subnetworks.LMSNetwork;
import org.simbrain.network.trainers.BackpropTrainer;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.genericframe.GenericFrame;

/**
 * PNode representation of a group of a backprop network
 * 
 * @author jyoshimi
 */
public class BackpropNetworkNode extends SubnetworkNode {
    
    /**
     * Create a layered network
     *
     * @param networkPanel parent panel
     * @param group the layered network
     */
    public BackpropNetworkNode(NetworkPanel networkPanel, BackpropNetwork group) {
        super(networkPanel, group);
        setInteractionBox(new BackpropInteractionBox(networkPanel));
        setContextMenu();
    }
    
    /**
     * Custom interaction box for Synapse group node.
     */
    private class BackpropInteractionBox extends InteractionBox {
        public BackpropInteractionBox(NetworkPanel net) {
            super(net, BackpropNetworkNode.this);
        }

//        @Override
//        protected JDialog getPropertyDialog() {
//            TrainerPanel panel = new TrainerPanel(getNetworkPanel(),
//                    getTrainer());
//            JDialog dialog = new JDialog();
//            dialog.setContentPane(panel);
//            return dialog;
//        }
//        
//      @Override
//      protected boolean hasPropertyDialog() {
//          return true;
//      }

        @Override
        protected String getToolTipText() {
            return "Backprop...";
        }

        @Override
        protected boolean hasToolTipText() {
            return true;
        }

    };

    /**
     * Sets custom menu.
     */
    private void setContextMenu() {
        JPopupMenu menu = super.getDefaultContextMenu();
        menu.addSeparator();
        menu.add(new JMenuItem(trainAction));
        menu.addSeparator();
        
        final BackpropNetwork network = (BackpropNetwork) getGroup();
        
		// Reference to the input data
		DataHolder inputData = new DataHolder() {
			@Override
			public void setData(double[][] data) {
				network.setInputData(data);
			}

			@Override
			public double[][] getData() {
				return network.getInputData();
			}

		};
		// Reference to the training data
		DataHolder trainingData = new DataHolder() {
			@Override
			public void setData(double[][] data) {
				network.setTrainingData(data);
			}

			@Override
			public double[][] getData() {
				return network.getTrainingData();
			}

		};
		menu.add(TrainerGuiActions.getEditCombinedDataAction(getNetworkPanel(),
				network, inputData, trainingData));
		menu.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(),
				network.getInputNeurons(), inputData, "Input"));
		menu.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(),
				network.getOutputNeurons(), trainingData, "Training"));
		
		setContextMenu(menu);
    }
    
    /**
     * Action to train Backrop
     */
	Action trainAction = new AbstractAction() {

		// Initialize
		{
			putValue(SMALL_ICON, ResourceManager.getImageIcon("Trainer.png"));
			putValue(NAME, "Train backprop net...");
			putValue(SHORT_DESCRIPTION, "Train backprop net...");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			BackpropNetwork network = (BackpropNetwork) getGroup();
			BackpropTrainerPanel trainingPanel = new BackpropTrainerPanel(
					getNetworkPanel(), new BackpropTrainer(network, network.getNeuronGroupsAsList()));
            GenericFrame frame = getNetworkPanel().displayPanel(trainingPanel, "Trainer");
            trainingPanel.setFrame(frame);
		}
	};
	

}
