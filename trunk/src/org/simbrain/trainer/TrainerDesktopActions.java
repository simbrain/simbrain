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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.builders.LayeredNetworkBuilder;
import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * Contains actions for use in the trainer GUI, when used in the Simbrain
 * Desktop.
 *
 * @author jyoshimi
 */
public class TrainerDesktopActions {

    /**
     * Returns an action for building a three layer network.
     *
     * @param desktopComponnent reference to desktop component
     * @return the action
     */
    public static Action getBuildThreeLayerAction(
            final TrainerDesktopComponent desktopComponnent) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON,
                        ResourceManager.getImageIcon("Network.png"));
                putValue(NAME, "Build three-layer network...");
                putValue(SHORT_DESCRIPTION, "Create a three-layer network");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                LayeredNetworkBuilder builder = new LayeredNetworkBuilder();
                int numInputs = desktopComponnent.getTrainerPanel()
                        .getInputData().getColumnCount() - 1;
                // TODO: Another way to do this? Make getNumInputs() function?
                String hiddenUnitsString = (String) JOptionPane
                        .showInputDialog(null, "Number of hidden units", "4");
                int numHidden;
                if (hiddenUnitsString != null) {
                    numHidden = Integer.parseInt(hiddenUnitsString);
                } else {
                    return; // User pressed cancel
                }
                int numOutputs = desktopComponnent.getTrainerPanel()
                        .getTrainingData().getColumnCount() - 1;
                int[] nodesPerLayer = new int[] { numInputs, numHidden,
                        numOutputs };
                builder.setNodesPerLayer(nodesPerLayer);
                NetworkComponent network = new NetworkComponent(
                        "Backprop network");
                builder.buildNetwork(network.getRootNetwork());
                desktopComponnent.getWorkspaceComponent().getWorkspace()
                        .addWorkspaceComponent(network);
                desktopComponnent.getTrainerPanel().getTrainer()
                        .setNetwork(network.getRootNetwork());
            }

        };
    }

    /**
     * Returns an action for building a multi-layer network.
     *
     * @param desktopComponnent reference to desktop component
     * @return the action
     */
    public static Action getBuildMultiLayerAction(
            final TrainerDesktopComponent desktopComponnent) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON,
                        ResourceManager.getImageIcon("Network.png"));
                putValue(NAME, "Build multi-layer network...");
                putValue(SHORT_DESCRIPTION, "Create a mutli-layer network");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                LayeredNetworkBuilder builder = new LayeredNetworkBuilder();
                int[] topology;
                int numInputs = desktopComponnent.getTrainerPanel()
                        .getInputData().getColumnCount() - 1;
                int numOutputs = desktopComponnent.getTrainerPanel()
                        .getTrainingData().getColumnCount() - 1;
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
                desktopComponnent.getWorkspaceComponent().getWorkspace()
                        .addWorkspaceComponent(network);
                desktopComponnent.getTrainerPanel().getTrainer()
                        .setNetwork(network.getRootNetwork());
            }

        };
    }

    // TODO: Note that open, save, save-as are a bit odd for this component
    // type, since it relies on the presence of at least one other component.

    /**
     * Open a saved trainer component.
     *
     * @param component reference to gui component
     * @return the action
     */
    public static Action getOpenAction(final GuiComponent component) {
        return new AbstractAction() {

            // Initialize
            {
                if (component == null) {
                    throw new IllegalArgumentException(
                            "Desktop component must not be null");
                }
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Open.png"));
                putValue(NAME, "Open...");
                putValue(SHORT_DESCRIPTION, "Open trainer component");
            }

            /** {@inheritDoc} */
            public void actionPerformed(final ActionEvent event) {
                component.showOpenFileDialog();
            }

        };
    }

    /**
     * Save current trainer component.
     *
     * @param component reference to gui component
     * @return the action
     */
    public static Action getSaveAction(final GuiComponent component) {
        return new AbstractAction() {

            // Initialize
            {
                if (component == null) {
                    throw new IllegalArgumentException(
                            "Desktop component must not be null");
                }
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Save.png"));
                putValue(NAME, "Save");
                putValue(SHORT_DESCRIPTION, "Save trainer component");
            }

            /** {@inheritDoc} */
            public void actionPerformed(final ActionEvent event) {
                component.save();
            }

        };
    }

    /**
     * Save current trainer component to a specific name.
     *
     * @param component reference to gui component
     * @return the action
     */
    public static Action getSaveAsAction(final GuiComponent component) {
        return new AbstractAction() {

            // Initialize
            {
                if (component == null) {
                    throw new IllegalArgumentException(
                            "Desktop component must not be null");
                }
                putValue(SMALL_ICON, ResourceManager.getImageIcon("SaveAs.png"));
                putValue(NAME, "Save as...");
                putValue(SHORT_DESCRIPTION, "Save trainer component");
            }

            /** {@inheritDoc} */
            public void actionPerformed(final ActionEvent event) {
                component.showSaveFileDialog();
            }

        };
    }

}
