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
package org.simbrain.network.gui.actions.modelgroups;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.group.NeuronGroupDialog;
import org.simbrain.util.StandardDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;

/**
 * Create a new group using the specified dialog.
 */
public final class AddGroupAction extends AbstractAction {

    /**
     * Network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Parent dialog.
     */
    private Class<? extends StandardDialog> dialogClass;

    /**
     * Create a new add group properties action with the specified network
     * panel.
     *
     * @param networkPanel networkPanel, must not be null
     * @param dialogClass  the class to be instantiated when this action invoked
     * @param name         string description of this action
     */
    public AddGroupAction(final NetworkPanel networkPanel, Class<? extends StandardDialog> dialogClass, final String name) {

        super(name);
        this.dialogClass = dialogClass;
        this.networkPanel = networkPanel;

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        putValue(SHORT_DESCRIPTION, "Add " + name + " group to network");
        if (dialogClass == NeuronGroupDialog.class) {
            networkPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('g'), this);
            networkPanel.getActionMap().put(this, this);
        }
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        networkPanel.repaint();
        StandardDialog dialog;
        try {
            dialog = dialogClass.getDeclaredConstructor(new Class[]{NetworkPanel.class}).newInstance(networkPanel);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            // Not sure why call below needed, but for some reason the ok button
            // sometimes goes out of focus when creating a new dialog.
            dialog.getRootPane().setDefaultButton(dialog.getOkButton());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}