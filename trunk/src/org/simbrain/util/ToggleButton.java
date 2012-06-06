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
package org.simbrain.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

/**
 * Toggle button. Used when a button iterates through a sequence of modes.
 */
public final class ToggleButton extends JButton {

    /** List of actions. */
    private final List actions;

    /** Index to current action. */
    private int index;

    /**
     * Create a new toggle button with the specified list of actions.
     *
     * @param actions list of actions, must not be null and must not be empty
     */
    public ToggleButton(final List actions) {

        super();

        if (actions == null) {
            throw new IllegalArgumentException("actions must not be null");
        }
        if (actions.size() == 0) {
            throw new IllegalArgumentException("actions must not be empty");
        }

        index = 0;
        this.actions = new ArrayList(actions);
        updateAction();

        addActionListener(new ActionListener() {
            /** @see ActionListener */
            public void actionPerformed(final ActionEvent event) {
                SwingUtilities.invokeLater(new Runnable() {
                    /** @see Runnable */
                    public void run() {
                        incrementIndex();
                        updateAction();
                    }
                });
            }
        });
    }

    /**
     * Increment index.
     */
    private void incrementIndex() {
        index++;

        if (index >= actions.size()) {
            index = 0;
        }
    }

    /**
     * Update action.
     */
    private void updateAction() {
        setAction((Action) actions.get(index));
        // no label for toolbar buttons
        setText("");
    }
}