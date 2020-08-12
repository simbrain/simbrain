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
package org.simbrain.network.gui.actions.synapse;

import org.simbrain.network.gui.NetworkDialogsKt;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.WeightMatrixViewer;
import org.simbrain.network.gui.actions.ConditionallyEnabledAction;
import org.simbrain.util.ResourceManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Show weight matrix connecting selected source and target neurons.
 */
public final class ShowWeightMatrixAction extends ConditionallyEnabledAction {

    /**
     * Construct action.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public ShowWeightMatrixAction(final NetworkPanel networkPanel) {

        super(networkPanel, "Display / Edit Weight Matrix...", EnablingCondition.SOURCE_AND_TARGET_NEURONS);
        putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/grid.png"));
        putValue(SHORT_DESCRIPTION, "Show a weight matrix connecting source neurons (adorned with red squares) and target neurons (regular green selection)");
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
        NetworkDialogsKt.showWeightMatrix(getNetworkPanel());
    }
}