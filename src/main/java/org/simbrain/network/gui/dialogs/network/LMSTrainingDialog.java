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
package org.simbrain.network.gui.dialogs.network;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.network.gui.trainer.DataPanel;
import org.simbrain.network.gui.trainer.ErrorPlotPanel;
import org.simbrain.network.subnetworks.LMSNetwork;
import org.simbrain.network.trainers.IterableTrainer;
import org.simbrain.network.trainers.TrainingSet;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog to edit an {@link LMSNetwork}.
 */
public class LMSTrainingDialog extends StandardDialog {

    /**
     * The LMS Network being edited.
     */
    private LMSNetwork lms;

    /**
     * Main tabbed pane.
     */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * Reference to input data panel.
     */
    private DataPanel inputPanel;

    /**
     * Reference to training data panel.
     */
    private DataPanel trainingPanel;

    /**
     * Reference to validate inputs panel
     */
    private TestInputPanel validateInputsPanel;

    /**
     * List of tabs in the dialog.
     */
    private List<Component> tabs = new ArrayList<Component>();

    /**
     * Network panel.
     */
    protected NetworkPanel networkPanel;

    /**
     * Default constructor.
     *
     * @param np  parent panel
     * @param lms edited network
     */
    public LMSTrainingDialog(final NetworkPanel np, final LMSNetwork lms) {
        this.lms = lms;
        this.networkPanel = np;
        init();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {
        setTitle("Edit LMS Network");

        // Set to modeless so the dialog can be left open
        setModalityType(ModalityType.MODELESS);

        // Main vertical box
        Box trainerPanel = Box.createVerticalBox();
        Box buttonPanel = Box.createHorizontalBox();

        // Time series for error.
        LMSTrainer trainer = new LMSTrainer();
        ErrorPlotPanel errorPanel = new ErrorPlotPanel(trainer);
        trainerPanel.add(errorPanel);

        // Button to initialize the network object
        JButton prefsButton = new JButton("Network Prefs");
        prefsButton.addActionListener(e -> {
            AnnotatedPropertyEditor configPanel = new AnnotatedPropertyEditor(lms.getConfig());
            StandardDialog dialog = configPanel.getDialog();
            dialog.makeVisible();
            dialog.addClosingTask(() -> {
                configPanel.commitChanges();
                lms.initNetwork();
            });
        });
        buttonPanel.add(prefsButton);

        // Train the network
        JButton trainButton = new JButton("Train");
        trainerPanel.add(trainButton);
        trainButton.addActionListener(e -> {
            try {
                trainer.iterate2();
            } catch (IterableTrainer.DataNotInitializedException dataNotInitializedException) {
                dataNotInitializedException.printStackTrace();
            }
        });
        buttonPanel.add(trainButton);
        trainerPanel.add(buttonPanel);

        // Add to tabbed pane
        tabbedPane.addTab("Train", trainerPanel);

        // Input data tab
        inputPanel = new DataPanel(lms.getInputData());
        inputPanel.setFrame(this);
        tabbedPane.addTab("Input data", inputPanel);

        // Training data tab
        trainingPanel = new DataPanel(lms.getTargetData());
        trainingPanel.setFrame(this);
        tabbedPane.addTab("Target data", trainingPanel);

        // Testing tab
        validateInputsPanel = new TestInputPanel(networkPanel, lms);
        tabbedPane.addTab("Validate Input Data", validateInputsPanel);

        // Finalize
        setContentPane(tabbedPane);

        // See SupervisedTrainingDialog.  Can use this to reset size of tabs.
        tabbedPane.addChangeListener(e -> {
            int index =  ((JTabbedPane) e.getSource()).getSelectedIndex();
            if (index == 0) {
                // TODO: Only update data when _leaving_ relevant panel,
                // maybe with a focus listener
                inputPanel.commitChanges();
                trainingPanel.commitChanges();
            }
        });

    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
        super.closeDialogOk();
    }

    private class LMSTrainer extends IterableTrainer {

        @Override
        public double getError() {
            return lms.getError();
        }

        @Override
        public void randomize() {
            // mln.init();
        }

        @Override
        protected TrainingSet getTrainingSet() {
            // TODO
            return null;
        }

        @Override
        public void apply() throws DataNotInitializedException {
            lms.train();
        }
    }

}
