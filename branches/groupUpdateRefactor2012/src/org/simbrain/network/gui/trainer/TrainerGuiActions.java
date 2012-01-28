/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.trainer;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.trainer.TrainerPanel.TrainerDataType;
import org.simbrain.network.trainers.InvalidDataException;
import org.simbrain.network.trainers.IterableAlgorithm;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.network.trainers.TrainingMethod;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.propertyeditor.ReflectivePropertyEditor;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.table.SimbrainJTable;

/**
 * Contains actions for use in Trainer GUI.
 * 
 * @author jyoshimi
 */
public class TrainerGuiActions {

    /** System file separator property. */
    public static final String FS = System.getProperty("file.separator");

    /** Directory where text files for dictionaries are stored. */
    private static String DEFAULT_DIR = "." + FS + "simulations" + FS
            + "tables";

    /** The main user preference object. */
    private static final Preferences THE_PREFS = Preferences.userRoot().node(
            "org/simbrain/network/trainer");

    /**
     * Action invoked when pressing the input or training data button on the
     * trainer panel.
     * 
     * @param networkPanel the parent network panel
     * @param type whether this is input or training data
     * @return the action
     */
    public static Action getEditDataAction(final NetworkPanel networkPanel,
            final Trainer trainer, final TrainerDataType type) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Table.png"));
                if (type == TrainerDataType.Input) {
                    putValue(NAME, "Input data");
                } else {
                    putValue(NAME, "Training data");
                }
                putValue(SHORT_DESCRIPTION, "Edit data");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                networkPanel.displayPanel(
                        DataViewer.createDataViewerPanel(trainer, type),
                        type.name() + " Data");
            }

        };
    }

    /**
     * Sets the current data directory in user preferences (memory for file
     * chooser).
     * 
     * @param dir directory to set
     */
    public static void setDataDirectory(final String dir) {
        THE_PREFS.put("dataDirectory", dir);
    }

    /**
     * Return the current data directory.
     * 
     * @return return the data directory
     */
    public static String getDataDirectory() {
        return THE_PREFS.get("dataDirectory", DEFAULT_DIR);
    }

    /**
     * Action for opening from comma separated value file. Replaces the default
     * simbrainjtable action for this, so that the trainer and trainer panel can
     * be updated as appropriate.
     * 
     * @param table table to load data in to
     * @return the action
     */
    public static Action getOpenCSVAction(final Trainer trainer,
            final SimbrainJTable table, final TrainerDataType type) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Open.png"));
                if (type == TrainerDataType.Input) {
                    putValue(NAME, "Open input data (.csv)");
                } else {
                    putValue(NAME, "Open training data (.csv)");
                }
                putValue(SHORT_DESCRIPTION, "Open .csv data...");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                SFileChooser chooser = new SFileChooser(getDataDirectory(),
                        "comma-separated-values (csv)", "csv");
                File theFile = chooser.showOpenDialog();
                if (theFile != null) {
                    if (type == TrainerDataType.Input) {
                        try {
                            trainer.setInputData(theFile);
                            ((NumericTable) table.getData()).readData(theFile);
                            ((JFrame) table.getTopLevelAncestor())
                                    .setTitle(theFile.getName());
                        } catch (InvalidDataException exception) {
                            JOptionPane.showMessageDialog(null,
                                    exception.getMessage(), "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        try {
                            trainer.setTrainingData(theFile);
                            ((NumericTable) table.getData()).readData(theFile);
                            ((JFrame) table.getTopLevelAncestor())
                                    .setTitle(theFile.getName());
                        } catch (InvalidDataException exception) {
                            JOptionPane.showMessageDialog(null,
                                    exception.getMessage(), "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    setDataDirectory(chooser.getCurrentLocation());
                }
            }

        };
    }
    
    /**
     * Randomizes network.
     * 
     * @param trainer reference to trainer
     * @return the action
     */
    public static Action getRandomizeNetworkAction(final Trainer trainer) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Rand.png"));
                // putValue(NAME, "Show properties");
                putValue(SHORT_DESCRIPTION, "Randomize network");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                if (trainer != null) {
                    if (trainer.getTrainingMethod() instanceof IterableAlgorithm) {
                        trainer.randomize();
                        // trainerGui.setUpdateCompleted(true); // Stop trainer
                        // if it's running

                        // Update Display
                        trainer.getNetwork().getRootNetwork()
                                .fireNetworkChanged();
                    }
                }
            }

        };
    }


    /**
     * Show properties dialog for currently selected trainer.
     * 
     * @param trainer the trainer
     * @return the action
     */
    public static AbstractAction getPropertiesDialogAction(
            final Trainer trainer) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.png"));
                // putValue(NAME, "Show properties");
                putValue(SHORT_DESCRIPTION, "Show properties");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                TrainingMethod method = trainer.getTrainingMethod();
                ReflectivePropertyEditor editor = new ReflectivePropertyEditor();
                editor.setUseSuperclass(false);
                editor.setObject(method);
                JDialog dialog = editor.getDialog();
                dialog.setModal(true);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }

        };
    }
    
    /**
     * Show an error plot for this trainer.
     * 
     * @param panel the network panel in which to display the plot
     * @param trainer the trainer
     * @return the action
     */
    public static AbstractAction getShowPlotAction(final NetworkPanel panel,
            final Trainer trainer) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("CurveChart.png"));
                putValue(NAME, "Show error plot");
                putValue(SHORT_DESCRIPTION, "Show error plot");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                ErrorPlotPanel errorPanel = new ErrorPlotPanel(
                        panel, trainer);
                panel.displayPanel(errorPanel, "Error plot");
            }

        };
    }

}
