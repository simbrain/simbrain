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
package org.simbrain.plot.timeseries;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.simbrain.resource.ResourceManager;

/**
 * Contains actions for use in Time Series Component.
 *
 * TODO: Possibly abstract these actions and move to top level of plot package.
 *
 * @author jyoshimi
 */
public class TimeSeriesPlotActions {


    /**
     * Shows a properties dialog for the trainer.
     *
     * @param TimeSeriesPlotPanel reference to time series plot panel
     * @return the action
     */
    public static Action getPropertiesDialogAction(final TimeSeriesPlotPanel timeSeriesPanel) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.png"));
                putValue(NAME, "Preferences...");
                putValue(SHORT_DESCRIPTION, "Show time series graph properties");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                timeSeriesPanel.showPropertiesDialog();
            }
        };
    }

    /**
     * Clear the  graph.
     *
     * @param TimeSeriesPlotPanel reference to time series plot panel
     * @return the action
     */
    public static Action getClearGraphAction(final TimeSeriesPlotPanel timeSeriesPanel) {
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
                timeSeriesPanel.getTimeSeriesModel().clearData();
            }

        };
    }

    /**
     * Add a data source.
     *
     * @param TimeSeriesPlotPanel reference to time series plot panel
     * @return the action
     */
    public static Action getAddSourceAction(final TimeSeriesPlotPanel timeSeriesPanel) {
        return new AbstractAction() {

            // Initialize
            {
                //putValue(SMALL_ICON, ResourceManager.getImageIcon("Eraser.png"));
                putValue(NAME, "Add");
                putValue(SHORT_DESCRIPTION, "Add a data source");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                timeSeriesPanel.getTimeSeriesModel().addDataSource();
            }

        };
    }

    /**
     * Add a data source.
     *
     * @param TimeSeriesPlotPanel reference to time series plot panel
     * @return the action
     */
    public static Action getRemoveSourceAction(final TimeSeriesPlotPanel timeSeriesPanel) {
        return new AbstractAction() {

            // Initialize
            {
                //putValue(SMALL_ICON, ResourceManager.getImageIcon("Eraser.png"));
                putValue(NAME, "Remove");
                putValue(SHORT_DESCRIPTION, "Remove a data source");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                timeSeriesPanel.getTimeSeriesModel().removeDataSource();
            }

        };
    }
}
