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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Toggle auto zoom mode on double click, fit objects to screen on 
 * single click.
 */
public final class ToggleAutoZoom extends JToggleButton {

    //TODO: Possibly misplaced in this action package, but oh well...

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /**
     * Create a new set auto zoom action with the specified network panel.
     *
     * @param np networkPanel, must not be null
     */
    public ToggleAutoZoom(final NetworkPanel np) {

        if (np == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }
        this.networkPanel = np;
        setIcon(ResourceManager.getImageIcon("ZoomFitPage.png"));
        setToolTipText("Single click fits objects on screen; "
                + "double-click toggles auto-zoom mode");

        setSelected(networkPanel.getAutoZoomMode());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int numClicks = e.getClickCount();
                // Do an immediate refocus
                if (numClicks == 1) {
                    networkPanel.zoomToFitPage(true);
                }
                // Toggle the mode
                if (numClicks == 2) {
                    networkPanel
                            .setAutoZoomMode(!networkPanel.getAutoZoomMode());
                    setButtonBorder();
                }
            }
        });

        setButtonBorder();
    }

    /**
     * Sets the button border based on whether the button is pressed.
     */
    private void setButtonBorder() {
        if (networkPanel.getAutoZoomMode()) {
            setBorder(BorderFactory.createLoweredBevelBorder());
        } else {
            setBorder(BorderFactory.createEmptyBorder());
        }
    }

}
