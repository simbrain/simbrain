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
package org.simbrain.world.odorworld;

import java.awt.Point;

import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.MovingEntity;
import org.simbrain.world.odorworld.entities.StaticEntity;
import org.simbrain.world.odorworld.entities.Wall;


/**
 * <b>WorldClipboard</b> is a static clipboard utility class.
 *
 * @author RJB
 */
public final class WorldClipboard {
    /** Clipboard entity. */
    private static OdorWorldEntity clipboardEntity;

    /**
     * Default constructor.
     */
    private WorldClipboard() {
    }

    /**
     * Clear all items from clipboard.
     */
    public static void clearClipboard() {
        setClipboardEntity(null);
    }

    /**
     * Cuts an object from a world.
     *
     * @param selectedEntity Selected entity
     * @param parent Parent world
     */
    public static void cutItem(final OdorWorldEntity selectedEntity, final OdorWorldPanel parent) {
        setClipboardEntity(selectedEntity);
        parent.getWorld().getEntityList().remove(selectedEntity);
        parent.repaint();
    }

    /**
     * Paste an object to a world.
     *
     * @param p Point to paste object
     * @param parent Parent world
     */
    public static void pasteItem(final Point p, final OdorWorldPanel parent) {
        OdorWorldEntity temp = getClipboardEntity();
// TODO
        if (temp != null) {
//            temp.setParent(parent.getWorld());
//            temp.setX(p.x);
//            temp.setY(p.y);
//            parent.getWorld().getAbstractEntityList().add(temp);
//            parent.repaint();
        }

        copyItem(temp);
    }

    /**
     * Copy an abstract entity from a world.
     *
     * @param entity Entity to copy
     */
    public static void copyItem(final OdorWorldEntity entity) {
    	// TODO!
//        if (entity instanceof OdorWorldEntity && !(entity instanceof OdorWorldAgent)) {
//            copyEntity((OdorWorldEntity) entity);
//        } else if (entity instanceof OdorWorldAgent) {
//            copyAgent((OdorWorldAgent) entity);
//        } else if (entity instanceof Wall) {
//            copyWall((Wall) entity);
//        }
    }

    // TODO REDO ALL CLIPBOARD STUFF!
    
    /**
     * Copy an entity from a world.
     *
     * @param entity Entity to copy
     */
    public static void copyEntity(final StaticEntity entity) {
//        OdorWorldEntity temp = new OdorWorldEntity();
//        temp.setImageName(entity.getImageName());
//        temp.setName("Copy of " + entity.getName());
////        temp.setStimulus(entity.getStimulus());
//        temp.setImage(entity.getImage().getImage());
//        setClipboardEntity(temp);
    }

    /**
     * Copy an agent from a world.
     *
     * @param agent Agent to be copied
     */
    public static void copyAgent(final MovingEntity agent) {
        MovingEntity temp = agent.copy();
//        setClipboardEntity(temp);  // TODO
    }

    /**
     * Copy a wall from a world.
     *
     * @param wall Wall to copy
     */
    public static void copyWall(final Wall wall) {
        Wall temp = new Wall(wall.getParent());
        temp.setWidth(wall.getWidth());
        temp.setHeight(wall.getHeight());
       // setClipboardEntity(temp);
    }

    /**
     * @param clipboardEntity The clipboardEntity to set.
     */
    public static void setClipboardEntity(final OdorWorldEntity clipboardEntity) {
        WorldClipboard.clipboardEntity = clipboardEntity;
    }

    /**
     * @return Returns the clipboardEntity.
     */
    public static OdorWorldEntity getClipboardEntity() {
        return clipboardEntity;
    }
}
