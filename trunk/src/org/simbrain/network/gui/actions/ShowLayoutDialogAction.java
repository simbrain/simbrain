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
package org.simbrain.network.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.layout.LayoutDialog;

/**
 * Show layout dialog action.
 */
public class ShowLayoutDialogAction extends AbstractAction {

    /**
     * Serial UID.
     */
    private static final long serialVersionUID = -3355422356278099294L;

    /**
     * Show layout dialog action.
     */
    public ShowLayoutDialogAction() {
        super("Set Layout Properties...");
    }

    /** @see ActionEvent. */
    public void actionPerformed(ActionEvent e) {
        LayoutDialog dialog = new LayoutDialog();
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

}