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
package org.simbrain.network.gui.actions.edit;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Action to re-scale the screen to fit all objects.
 * 
 * TODO: No longer used.
 */
public final class ZoomToFitPageAction extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /**
     * Create a new zoom to fit page action.
     *
     * @param networkPanel network panel, must not be null
     */
    public ZoomToFitPageAction(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("ZoomFitPage.png"));
        putValue(SHORT_DESCRIPTION, "Fit all objects on screen (f)");
        putValue(SHORT_DESCRIPTION, "Zoom to fit all objects on screen (f)");
        networkPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke('f'), this);
        networkPanel.getActionMap().put(this, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        networkPanel.zoomToFitPage(true);
    }
}