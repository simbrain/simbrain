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
package org.simbrain.plot.rasterchart;

import org.simbrain.resource.ResourceManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Contains actions for use in raster plot
 */
public class RasterPlotActions {

    /**
     * Shows a properties dialog for the trainer.
     *
     * @param rasterPlotPanel reference to time series plot panel
     * @return the action
     */
    public static Action getPropertiesDialogAction(final RasterPlotPanel rasterPlotPanel) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.png"));
                putValue(NAME, "Preferences...");
                putValue(SHORT_DESCRIPTION, "Show raster chart properties");
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                rasterPlotPanel.showPropertiesDialog();
            }
        };
    }

    /**
     * Clear the graph.
     *
     * @param timeSeriesPanel reference to time series plot panel
     * @return the action
     */
    public static Action getClearGraphAction(final RasterPlotPanel timeSeriesPanel) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Eraser.png"));
                putValue(SHORT_DESCRIPTION, "Clear graph data");
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                timeSeriesPanel.getRasterModel().clearData();
            }

        };
    }
}
