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

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.trainers.IterableAlgorithm;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.network.trainers.TrainerListener;
import org.simbrain.plot.timeseries.TimeSeriesModel;
import org.simbrain.plot.timeseries.TimeSeriesPlotPanel;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.Utils;

/**
 * Component for representing error in a trainer.
 *
 * @author Jeff Yoshimi
 */
public class ErrorPlotPanel extends JPanel {

    /** Reference to trainer object. */
    private Trainer trainer;

    /** Data for the error graph. */
    private TimeSeriesModel model;

    /** Error label. */
    private JLabel rmsError = new JLabel("Error: ----- ");

    /** Indicates that (an iterative) training algorithm is running. */
    private JLabel runningLabel = new JLabel();

    /**
     * Construct a trainer panel around a trainer object.
     *
     * @param networkPanel the parent network panel
     * @param trainer the trainer this panel represents
     */
    public ErrorPlotPanel(final NetworkPanel networkPanel, final Trainer trainer) {

        this.trainer = trainer;
        JPanel mainPanel = new JPanel();        
        mainPanel.add(createGraphPanel());
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
     * Create the Graph panel.
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
    
    
    //TODO

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
                // trainerGui.clearGraph();
            }

        };
    }

}
