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
    private final Action openPlotAction;

    /** Save component action. */
    private final Action savePlotAction;

    /** Save component as action. */
    private final Action savePlotAsAction;

    /**
     * Plot component action manager.
     *
     * @param component Gui component.
     */
    @SuppressWarnings("unchecked")
    public PlotActionManager(GuiComponent component) {

        openPlotAction = new OpenPlotAction(component);
        savePlotAction = new SavePlotAction(component);
        savePlotAsAction = new SavePlotAsAction(component);
    }

    /**
     * @return the open/save plot actions.
     */
    public List<Action> getOpenSavePlotActions() {
        return Arrays.asList(new Action[] { openPlotAction, savePlotAction,
                savePlotAsAction });
    }

    /**
     * @return the openPlotAction
     */
    public Action getOpenPlotAction() {
        return openPlotAction;
    }

    /**
     * @return the savePlotAction
     */
    public Action getSavePlotAction() {
        return savePlotAction;
    }

    /**
     * @return the savePlotAsAction
     */
    public Action getSavePlotAsAction() {
        return savePlotAsAction;
    }
}
