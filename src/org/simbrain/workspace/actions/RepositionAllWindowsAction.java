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
package org.simbrain.workspace.actions;

import org.simbrain.workspace.gui.SimbrainDesktop;

import javax.swing.*;

import java.awt.Component;
import java.awt.event.ActionEvent;

/**
 * Reposition and resize all desktop windows in the upper left corner. Useful
 * when they get "lost".
 */
public final class RepositionAllWindowsAction extends WorkspaceAction {

    private static final long serialVersionUID = 1L;

    /**
     * Reference to Simbrain Desktop.
     */
    private SimbrainDesktop desktop;

    /**
     * Construct the action.
     *
     * @param desktop
     */
    public RepositionAllWindowsAction(final SimbrainDesktop desktop) {
        super("Reposition All Windows", desktop.getWorkspace());
        putValue(SHORT_DESCRIPTION, "Repositions and resize all windows. Useful when windows get \"lost\" offscreen.");
        this.desktop = desktop;
    }

    /**
     * @param event
     * @see AbstractAction
     */
    public void actionPerformed(final ActionEvent event) {
        int maxX = 0;
        int maxY = 0;
        double desktopHeight = desktop.getDesktop().getSize().getHeight();
        double desktopWidth = desktop.getDesktop().getSize().getWidth();

        for (Component c : desktop.getDesktop().getComponents()) {
            int bottomRightX = (int) (c.getWidth() + c.getX());
            int bottomRightY = (int) (c.getHeight() + c.getY());

            if (maxX < bottomRightX) {
                maxX = bottomRightX;
            }
            if (maxY < bottomRightY) {
                maxY = bottomRightY;
            }
        }

        double xScalingRatio = maxX / desktopWidth;
        double yScalingRatio = maxY / desktopHeight;

        double finalScalingRatio = xScalingRatio > yScalingRatio ? 1 / xScalingRatio : 1 / yScalingRatio;

        if (finalScalingRatio < 1) {
            for (Component c : desktop.getDesktop().getComponents()) {
                double orignalTopLeftX = c.getX();
                double orignalTopLeftY = c.getY();
                int originalWidth = c.getWidth();
                int originalHeight = c.getHeight();

                c.setBounds(
                        (int) (orignalTopLeftX * finalScalingRatio),
                        (int) (orignalTopLeftY * finalScalingRatio),
                        (int) (originalWidth * finalScalingRatio),
                        (int) (originalHeight * finalScalingRatio)
                );
            }
        }
    }
}