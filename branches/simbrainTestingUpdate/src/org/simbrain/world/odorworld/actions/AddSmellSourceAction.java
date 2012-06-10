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
package org.simbrain.world.odorworld.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.simbrain.util.SimbrainMath;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.world.odorworld.OdorWorldPanel;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Add a new smell source to this entity.
 */
public final class AddSmellSourceAction extends AbstractAction {

    /** Plot GUI component. */
    private final OdorWorldPanel component;

    /** Entity to edit. */
    private final OdorWorldEntity entity;

    /**
     * Create a new open plot action.
     *
     * @param component GUI component, must not be null.
     */
    public AddSmellSourceAction(final OdorWorldPanel component,
            OdorWorldEntity entity) {
        super("Add smell source...");
        this.entity = entity;
        if (component == null) {
            throw new IllegalArgumentException(
                    "Desktop component must not be null");
        }
        this.component = component;
        // this.putValue(this.ACCELERATOR_KEY,
        // KeyStroke.getKeyStroke(KeyEvent.VK_P,
        // Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        // putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.png"));
        putValue(SHORT_DESCRIPTION, "Add a smell source to this object...");
    }

    /** {@inheritDoc} */
    public void actionPerformed(final ActionEvent event) {
        String dimension = JOptionPane
                .showInputDialog("How many dimensions will the smell vector have?");
        if (dimension != null) {
            int dims = Integer.parseInt(dimension);
            entity.setSmellSource(new SmellSource(SimbrainMath.zeroVector(dims)));
        }
    }

}
