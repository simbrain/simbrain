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
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.trainer.DataViewer.DataHolder;
import org.simbrain.network.trainers.InvalidDataException;
import org.simbrain.network.trainers.IterableTrainer;
import org.simbrain.network.trainers.Trainable;
import org.simbrain.network.trainers.Trainer;
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
     * Action for viewing data in a table that correlate with a set of neurons.
     * It's a bit of a pain, but to use this you must create an instance of a
     * DataHolder, which is basically just a reference to an object with getData
     * and setData methods.
     *
     * @param networkPanel the parent network panel
     * @param neurons the list of neurons to which the columns correspond
     * @param dataHolder the object that holds the data (with a getData and
     *            setData method)
     * @param name the name of the data (for use in display)
     * @return an action for opening this table
     */
    public static Action getEditDataAction(final NetworkPanel networkPanel,
            final List<Neuron> neurons, final DataHolder dataHolder,
            final String name) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Table.png"));
                putValue(NAME, "Edit " + name + " data..");
                putValue(SHORT_DESCRIPTION, "Edit data...");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                networkPanel.displayPanel(DataViewer.createDataViewerPanel(
                        neurons, dataHolder, name), "Edit " + name);
            }

        };
    }

    /**
     * Action for viewing two datatables, one for input data; the other for
     * training data.
     *
     * @param networkPanel the parent network panel.
     * @param trainable the trainable object providing access to input and
     *            output neurons
     * @param inputData access to input data via dataholder object
     * @param trainingData access to trainig data via dataholder object
     * @return
     */
    public static Action getEditCombinedDataAction(
            final NetworkPanel networkPanel, final Trainable trainable,
            final DataHolder inputData, final DataHolder trainingData) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Table.png"));
                putValue(NAME, "Edit combined data...");
                putValue(SHORT_DESCRIPTION, "Edit combined data...");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                JPanel inputPanel = DataViewer.createDataViewerPanel(
                        trainable.getInputNeurons(), inputData, "Input data");
                JPanel trainingPanel = DataViewer.createDataViewerPanel(
                        trainable.getOutputNeurons(), trainingData,
                        "Training data");
                JPanel combinedPanel = DataViewer
                        .createCombinedDataViewerPanel(inputPanel,
                                trainingPanel);
                networkPanel.displayPanel(combinedPanel, "Edit data");
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
     * Action for opening data comma separated value file. Replaces the default
     * simbrainjtable action for this, so that the trainer and trainer panel can
     * be updated as appropriate.
     *
     * @param table the simbrain jtable
     * @param dataHolder the object holding the data
     * @return the action for opening csv files
     */
    public static Action getOpenCSVAction(final SimbrainJTable table,
            final DataHolder dataHolder) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Open.png"));
                putValue(NAME, "Open data (.csv)");
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
                    try {
                        ((NumericTable) table.getData()).readData(theFile);
                        dataHolder.setData(((NumericTable) table.getData())
                                .asDoubleArray());
                        ((JFrame) table.getTopLevelAncestor()).setTitle(theFile
                                .getName());
                    } catch (InvalidDataException exception) {
                        JOptionPane.showMessageDialog(null,
                                exception.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
                setDataDirectory(chooser.getCurrentLocation());
            }

        };
    }

    /**
     * Show properties dialog for the indicated trainer.
     *
     * @param trainer the trainer
     * @return the action
     */
    public static AbstractAction getPropertiesDialogAction(final Trainer trainer) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.png"));
                putValue(NAME, "Properties");
                putValue(SHORT_DESCRIPTION, "Edit Properties");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                ReflectivePropertyEditor editor = new ReflectivePropertyEditor();
                editor.setUseSuperclass(false);
                editor.setObject(trainer);
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
            final IterableTrainer trainer) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON,
                        ResourceManager.getImageIcon("CurveChart.png"));
                putValue(NAME, "Show error plot");
                putValue(SHORT_DESCRIPTION, "Show error plot");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                ErrorPlotPanel errorPanel = new ErrorPlotPanel(trainer);
                panel.displayPanel(errorPanel, "Error plot");
            }

        };
    }

}
