/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.plot.actions;

import java.util.Arrays;
import java.util.List;

import javax.swing.Action;

import org.simbrain.workspace.gui.GuiComponent;

/**
 * Manages the actions for the plot components.
 *
 */
public class PlotActionManager {

    /** Open component action. */
    private final Action exportPlotAction;

    /** Save component as action. */
    private final Action importPlotAction;

    /**
     * Plot component action manager.
     *
     * @param component Gui component.
     */
    @SuppressWarnings("unchecked")
    public PlotActionManager(GuiComponent component) {

        exportPlotAction = new ImportPlotAction(component);
        importPlotAction = new ExportPlotAction(component);
    }

    /**
     * @return the open/save plot actions.
     */
    public List<Action> getOpenSavePlotActions() {
        return Arrays.asList(exportPlotAction, importPlotAction);
    }

}
