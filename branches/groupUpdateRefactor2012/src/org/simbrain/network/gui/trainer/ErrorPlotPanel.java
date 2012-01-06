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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.groups.FeedForward;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.trainers.IterableAlgorithm;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.network.trainers.TrainerListener;
import org.simbrain.plot.timeseries.*;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.ClassDescriptionPair;
import org.simbrain.util.Utils;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.genericframe.GenericJFrame;

/**
 * GUI for supervised learning in Simbrain, using back-propagation, LMS, and
 * (eventually) other algorithms. A GUI front end for the trainer class.
 *
 * @author Jeff Yoshimi
 */
public class ErrorPlotPanel extends JPanel {

    /** Reference to trainer object. */
    private Trainer trainer;

    /** Data for the error graph. */
    private TimeSeriesModel model;

    /** Text field for setting number of iterations to run. */
    private JTextField tfIterations;

    /** Error label. */
    private JLabel rmsError = new JLabel("Error: ----- ");

    /** Update completed boolean value. */
    private boolean updateCompleted = true;

    /** Top panel. */
    private JPanel topItems;

    /** Parent frame. */
    private GenericFrame parentFrame;

    /** Indicates that (an iterative) training algorithm is running. */
    private JLabel runningLabel = new JLabel();

    /** Reference to parent panel. Used as a reference for displaying the trainer panel. */
    private final NetworkPanel panel;
    
    /**
     * Construct a trainer panel around a trainer object.
     *
     * @param networkPanel the parent network panel
     * @param trainer the trainer this panel represents
     */
    public ErrorPlotPanel(final NetworkPanel networkPanel, final Trainer trainer) {

        // Initial setup
        this.trainer = trainer;
        this.panel = networkPanel;

        // Main Panel
        JPanel mainPanel = new JPanel();
     //   mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Top items
//        topItems = new JPanel();
//        topItems.setLayout(new BoxLayout(topItems, BoxLayout.Y_AXIS));
//        mainPanel.add(topItems);
        
        mainPanel.add(createGraphPanel());

        // Add mainPanel
        add(mainPanel);
        
        trainer.addListener(new TrainerListener() {

            public void errorUpdated() {
                if (trainer.getTrainingMethod() instanceof IterableAlgorithm) {
                    if (model != null) {
                        model.update();
                        IterableAlgorithm theTrainer = (IterableAlgorithm) trainer.getTrainingMethod();
                        model.addData(0, theTrainer.getIteration(),
                                theTrainer.getError());
                    }
                    updateErrorField();
                }
           }

            public void inputDataChanged(double[][] inputData) {
            }

            public void trainingDataChanged(double[][] trainingData) {
            }
            
        });

    }
    
    /**
     * Create the run panel for iterable algorithms.
     *
     * @return the panel
     */
    private JPanel createGraphPanel() {

         model = new TimeSeriesModel(1);
         model.setRangeLowerBound(0);
         model.setRangeUpperBound(1);
         model.setAutoRange(false);
         model.setWindowSize(1000);
         //Configure time series plot
         TimeSeriesPlotPanel graphPanel = new TimeSeriesPlotPanel(model);
         graphPanel.getChartPanel().getChart().setTitle("");
         graphPanel.getChartPanel().getChart().getXYPlot().getDomainAxis()
         .setLabel("Iterations");
         graphPanel.getChartPanel().getChart().getXYPlot().getRangeAxis()
         .setLabel("Error");
         graphPanel.getChartPanel().getChart().removeLegend();
         graphPanel.setPreferredSize(new Dimension(graphPanel.getPreferredSize().width, 250));

         //Customize button panel; first remove all buttons
         graphPanel.removeAllButtonsFromToolBar();
         //Add clear and prefs button
         graphPanel.addClearGraphDataButton();
         graphPanel.addPreferencesButton();
         graphPanel.getButtonPanel().add(rmsError);
         graphPanel.getButtonPanel().add(runningLabel);

        return graphPanel;
    }

    /**
     * Update error text field.
     */
    private void updateErrorField() {
        if (trainer.getTrainingMethod() instanceof IterableAlgorithm) {
            rmsError.setText("Error:"
                    + Utils.round(((IterableAlgorithm) trainer
                            .getTrainingMethod()).getError(), 4));
        }
    }

    /**
     * Batch train network, using text field.
     */
    public final void batchTrain() {
        if (trainer != null) {
            // trainer.train(Integer.parseInt(tfIterations.getText()));
            updateErrorField();
        }
    }

    /**
     * @return boolean updated completed.
     */
    final boolean isUpdateCompleted() {
        return updateCompleted;
    }

    /**
     * Sets updated completed value.
     *
     * @param updateCompleted Updated completed value to be set
     */
    final void setUpdateCompleted(final boolean updateCompleted) {
        this.updateCompleted = updateCompleted;
        if (runningLabel != null) {
            if (updateCompleted) {
                runningLabel.setVisible(false);
            } else {
                runningLabel.setVisible(true);
            }
            parentFrame.pack();
        }
    }

    /**
     * @return the trainer
     */
    final Trainer getTrainer() {
        return trainer;
    }

}
