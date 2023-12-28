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

import org.simbrain.util.ResourceManager;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Delete entity.
 */
public final class DeleteEntityAction extends AbstractAction {

    /**
     * Entity to edit.
     */
    private final OdorWorldEntity entity;

    public DeleteEntityAction(OdorWorldEntity entity) {

        super("Delete entity");
        this.entity = entity;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Delete.png"));
        putValue(SHORT_DESCRIPTION, "Delete entity");
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
        entity.getWorld().deleteEntity(entity);
    }
}
