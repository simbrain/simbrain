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
package org.simbrain.trainer;

import java.awt.event.ActionEvent;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.builders.LayeredNetworkBuilder;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.propertyeditor.ReflectivePropertyEditor;

/**
 * Contains actions for use in Trainer GUI.
 *
 * @author jyoshimi
 */
public class TrainerGuiActions {

    /**
     * Returns a "play" action, that can be used to repeatedly iterate the
     * trainer.
     *
     * @param trainerGui reference to trainer gui
     * @return the action
     */
    public static Action getRunAction(final TrainerGUI trainerGui) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Play.png"));
                //putValue(NAME, "Open (.csv)");
                //putValue(SHORT_DESCRIPTION, "Import table from .csv");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                if (trainerGui.isUpdateCompleted()) {
                    // Start running
                    trainerGui.setUpdateCompleted(false);
                    Executors.newSingleThreadExecutor().submit(new Runnable() {
                        public void run() {
                            while (!trainerGui.isUpdateCompleted()) {
                                trainerGui.iterate();
                            }
                        }
                    });
                    putValue(SMALL_ICON, ResourceManager.getImageIcon("Stop.png"));
                } else {
                    // Stop running
                    trainerGui.setUpdateCompleted(true);
                    putValue(SMALL_ICON, ResourceManager
                            .getImageIcon("Play.png"));
                }

            }

        };
    }

    /**
     * Shows a properties dialog for the trainer.
     *
     * @param trainerGui reference to trainer gui
     * @return the action
     */
    public static Action getPropertiesDialogAction(final TrainerGUI trainerGui) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.png"));
                //putValue(NAME, "Show properties");
                putValue(SHORT_DESCRIPTION, "Show properties");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                Trainer trainer = trainerGui.getTrainer();

                if (trainer instanceof BackpropTrainer) {
                    ReflectivePropertyEditor editor = new ReflectivePropertyEditor();
                    editor.setUseSuperclass(false);
                    editor.setObject((BackpropTrainer)trainer);
                    JDialog dialog = editor.getDialog();
                    dialog.setModal(true);
                    dialog.pack();
                    dialog.setLocationRelativeTo(null);
                    dialog.setVisible(true);
                }

            }

        };
    }

    /**
     * Returns an action for building a three layer network.
     *
     * @param trainerGui reference to trainer gui
     * @return the action
     */
    public static Action getBuildThreeLayerAction(final TrainerGUI trainerGui) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Network.png"));
                putValue(NAME, "Build three-layer network...");
                putValue(SHORT_DESCRIPTION, "Create a three-layer network");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                LayeredNetworkBuilder builder = new LayeredNetworkBuilder();
                int numInputs = trainerGui.getInputDataTable().getData()
                        .getColumnCount();
                String hiddenUnitsString = (String) JOptionPane
                        .showInputDialog(null, "Number of hidden units", "4");
                int numHidden;
                if (hiddenUnitsString != null) {
                    numHidden = Integer.parseInt(hiddenUnitsString);
                } else {
                    return; // User pressed cancel
                }
                int numOutputs = trainerGui.getTrainingDataTable().getData()
                        .getColumnCount();
                int[] nodesPerLayer = new int[] { numInputs, numHidden,
                        numOutputs };
                builder.setNodesPerLayer(nodesPerLayer);
                NetworkComponent network = new NetworkComponent(
                        "Backprop network");
                builder.buildNetwork(network.getRootNetwork());
                trainerGui.getWorkspace().addWorkspaceComponent(network);
            }

        };
    }

    /**
     * Returns an action for building a multi layer network.
     *
     * @param trainerGui reference to trainer gui
     * @return the action
     */
    public static Action getBuildMultiLayerAction(final TrainerGUI trainerGui) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Network.png"));
                putValue(NAME, "Build multi-layer network...");
                putValue(SHORT_DESCRIPTION, "Create a mutli-layer network");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                LayeredNetworkBuilder builder = new LayeredNetworkBuilder();
                int[] topology;
                int numInputs = trainerGui.getInputDataTable().getData()
                        .getColumnCount();
                int numOutputs = trainerGui.getTrainingDataTable().getData()
                        .getColumnCount();
                String defaultTopologyString = numInputs + ",3,3," + numOutputs;
                String topologyString = (String) JOptionPane
                        .showInputDialog(
                                null,
                                "Network structure, specifid as a list of numbers, \n"
                                        + "corresponding to the number of neurons in each layer.",
                                defaultTopologyString);
                if (topologyString != null) {
                    String[] parsedString = topologyString.split(",");
                    topology = new int[parsedString.length];
                    for (int i = 0; i < parsedString.length; i++) {
                        topology[i] = Integer.parseInt(parsedString[i]);
                    }
                } else {
                    return; // User pressed cancel
                }
                builder.setNodesPerLayer(topology);
                NetworkComponent network = new NetworkComponent(
                        "Backprop network");
                builder.buildNetwork(network.getRootNetwork());
                trainerGui.getWorkspace().addWorkspaceComponent(network);
            }

        };
    }

}
