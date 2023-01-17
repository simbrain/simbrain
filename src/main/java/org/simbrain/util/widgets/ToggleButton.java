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
package org.simbrain.util.widgets;

import javax.swing.*;
import java.util.List;

/**
 * JButton that "toggles" through states that are externally set.
 */
public final class ToggleButton extends JButton {

    private List<Action> actions;

    public ToggleButton(final List<Action> actions) {
        super();
        this.actions = actions;
    }

    /**
     * Set the current action
     */
    public void setAction(String name) {

        var action = actions.stream().filter(a -> a.getValue(Action.NAME) == name).findFirst();
        if (action.isPresent()) {
            setAction(action.get());
            // no label for toolbar buttons
            setText("");
        }
    }
}