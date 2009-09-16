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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.propertyeditor.ReflectivePropertyEditor;
import org.simbrain.world.odorworld.DialogOdorWorldEntity;
import org.simbrain.world.odorworld.OdorWorldPanel;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Action for showing an entity dialog.
 */
public final class ShowEntityDialog
    extends AbstractAction {

    /** Reference to parent. */
    private final OdorWorldPanel component;

    /** Entity to edit. */
    private final OdorWorldEntity entity;

    /**
     * Construct a show entity action.
     *
     * @param component GUI component, must not be null.
     */
    public ShowEntityDialog(final OdorWorldPanel component, OdorWorldEntity entity) {
        super("World Preferences...");
        this.entity = entity;
        if (component == null) {
            throw new IllegalArgumentException("Desktop component must not be null");
        }
        this.component = component;
        this.putValue(this.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_P,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.png"));
        putValue(SHORT_DESCRIPTION, "Odor world preferences...");
    }


    /** {@inheritDoc} */
    public void actionPerformed(final ActionEvent event) {
        DialogOdorWorldEntity theDialog = new DialogOdorWorldEntity(entity);
        theDialog.pack();
        theDialog.setLocationRelativeTo(null);
        theDialog.setVisible(true);

        JDialog dialog = new JDialog();
        dialog.setContentPane(new ReflectivePropertyEditor(entity, dialog));

        if (!theDialog.hasUserCancelled()) {
            theDialog.commitChanges();
        }

        component.repaint();
    }
}
