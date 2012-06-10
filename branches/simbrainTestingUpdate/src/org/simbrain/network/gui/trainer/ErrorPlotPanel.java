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

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.trainers.ErrorListener;
import org.simbrain.network.trainers.IterableTrainer;
import org.simbrain.plot.timeseries.TimeSeriesModel;
import org.simbrain.plot.timeseries.TimeSeriesPlotPanel;
import org.simbrain.util.Utils;

/**
 * Component for representing error in a trainer.
 *
 * @author Jeff Yoshimi
 */
public class ErrorPlotPanel extends JPanel {

    /** Reference to trainer object. */
    private IterableTrainer trainer;

    /** Data for the error graph. */
    private final TimeSeriesModel model;

    /** Error label. */
    private JLabel rmsError = new JLabel("Error: ----- ");

    /** Indicates that (an iterative) training algorithm is running. */
    private JLabel runningLabel = new JLabel();

    /**
     * Construct a trainer panel around a trainer object.
     *
     * @param trainer the trainer this panel represents
     */
    public ErrorPlotPanel(final IterableTrainer trainer) {

        this.trainer = trainer;
        JPanel mainPanel = new JPanel();

        // Configure time series plot
        model = new TimeSeriesModel(1);
        model.setRangeLowerBound(0);
        model.setRangeUpperBound(1);
        model.setAutoRange(false);
        model.setWindowSize(1000);
        TimeSeriesPlotPanel graphPanel = new TimeSeriesPlotPanel(model);
        graphPanel.getChartPanel().getChart().setTitle("");
        graphPanel.getChartPanel().getChart().getXYPlot().getDomainAxis()
                .setLabel("Iterations");
        graphPanel.getChartPanel().getChart().getXYPlot().getRangeAxis()
                .setLabel("Error");
        graphPanel.getChartPanel().getChart().removeLegend();
        graphPanel.setPreferredSize(new Dimension(
                graphPanel.getPreferredSize().width, 250));

        // Customize button panel; first remove all buttons
        graphPanel.removeAllButtonsFromToolBar();
        graphPanel.addClearGraphDataButton();
        graphPanel.addPreferencesButton();
        graphPanel.getButtonPanel().add(rmsError);
        graphPanel.getButtonPanel().add(runningLabel);
        mainPanel.add(graphPanel);
        add(mainPanel);

        trainer.addErrorListener(new ErrorListener() {
            @Override
            public void errorUpdated() {
                if (model != null) {
                    model.update();
                    model.addData(0, trainer.getIteration(), trainer.getError());
                    updateErrorField();
                }
            }
        });

    }

    /**
     * Update error text field.
     */
    private void updateErrorField() {
        rmsError.setText("Error:" + Utils.round(trainer.getError(), 4));
    }

}
