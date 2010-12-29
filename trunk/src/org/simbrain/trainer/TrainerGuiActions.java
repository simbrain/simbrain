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
    public static Action getRunAction(final TrainerPanel trainerGui) {
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
    public static Action getPropertiesDialogAction(final TrainerPanel trainerGui) {
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
     * Batch train network.
     *
     * @param trainerGui reference to trainer gui
     * @return the action
     */
    public static Action getBatchTrainAction(final TrainerPanel trainerGui) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON,  ResourceManager.getImageIcon("BatchPlay.png"));
                //putValue(NAME, "Batch");
                putValue(SHORT_DESCRIPTION, "Batch train network");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                trainerGui.batchTrain();
            }

        };
    }

    /**
     * Randomizes network.
     *
     * TODO: Randomizes the _whole_ target network; not just the trained network.
     *
     * @param trainerGui reference to trainer gui
     * @return the action
     */
    public static Action getRandomizeNetworkAction(final TrainerPanel trainerGui) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Rand.png"));
                //putValue(NAME, "Show properties");
                putValue(SHORT_DESCRIPTION, "Randomize network");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                if (trainerGui.getTrainer() != null) {
                    if (trainerGui.getTrainer() instanceof BackpropTrainer) {
                        ((BackpropTrainer) trainerGui.getTrainer()).randomize();
                    }
                }
            }

        };
    }

    /**
     * Clear the error graph.
     *
     * @param trainerGui reference to trainer gui
     * @return the action
     */
    public static Action getClearGraphAction(final TrainerPanel trainerGui) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Eraser.png"));
                putValue(SHORT_DESCRIPTION, "Clear graph data");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
               //trainerGui.clearGraph();
            }

        };
    }

}
