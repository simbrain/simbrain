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


/**
 * <b>WorldClipboard</b> is a static clipboard utility class.
 *
 * @author RJB
 */
public final class WorldClipboard {
    /** Clipboard entity. */
    private static AbstractEntity clipboardEntity;

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
    public static void cutItem(final AbstractEntity selectedEntity, final OdorWorld parent) {
        setClipboardEntity(selectedEntity);
        parent.getAbstractEntityList().remove(selectedEntity);
        parent.repaint();
    }

    /**
     * Paste an object to a world.
     *
     * @param p Point to paste object
     * @param parent Parent world
     */
    public static void pasteItem(final Point p, final OdorWorld parent) {
        AbstractEntity temp = getClipboardEntity();

        if (temp != null) {
            temp.setParent(parent);
            temp.setX(p.x);
            temp.setY(p.y);
            parent.getAbstractEntityList().add(temp);
            parent.repaint();
        }

        copyItem(temp);
    }

    /**
     * Copy an abstract entity from a world.
     *
     * @param entity Entity to copy
     */
    public static void copyItem(final AbstractEntity entity) {
        if (entity instanceof OdorWorldEntity && !(entity instanceof OdorWorldAgent)) {
            copyEntity((OdorWorldEntity) entity);
        } else if (entity instanceof OdorWorldAgent) {
            copyAgent((OdorWorldAgent) entity);
        } else if (entity instanceof Wall) {
            copyWall((Wall) entity);
        }
    }

    /**
     * Copy an entity from a world.
     *
     * @param entity Entity to copy
     */
    public static void copyEntity(final OdorWorldEntity entity) {
        OdorWorldEntity temp = new OdorWorldEntity();
        temp.setImageName(entity.getImageName());
        temp.setName("Copy of " + entity.getName());
        temp.setStimulus(entity.getStimulus());
        temp.setTheImage(entity.getTheImage());
        setClipboardEntity(temp);
    }

    /**
     * Copy an agent from a world.
     *
     * @param agent Agent to be copied
     */
    public static void copyAgent(final OdorWorldAgent agent) {
        OdorWorldAgent temp = new OdorWorldAgent();
        temp.setImageName(agent.getImageName());
        temp.setMovementIncrement(agent.getMovementIncrement());
        temp.setName("Copy of " + agent.getName());
        temp.setOrientation(agent.getOrientation());
        temp.setStimulus(agent.getStimulus());
        temp.setTheImage(agent.getTheImage());
        temp.setTurnIncrement(agent.getTurnIncrement());
        temp.setWhiskerAngle(agent.getWhiskerAngle());
        temp.setWhiskerLength(agent.getWhiskerLength());
        setClipboardEntity(temp);
    }

    /**
     * Copy a wall from a world.
     *
     * @param wall Wall to copy
     */
    public static void copyWall(final Wall wall) {
        Wall temp = new Wall();
        temp.setWidth(wall.getWidth());
        temp.setHeight(wall.getHeight());
        setClipboardEntity(temp);
    }

    /**
     * @param clipboardEntity The clipboardEntity to set.
     */
    public static void setClipboardEntity(final AbstractEntity clipboardEntity) {
        WorldClipboard.clipboardEntity = clipboardEntity;
    }

    /**
     * @return Returns the clipboardEntity.
     */
    public static AbstractEntity getClipboardEntity() {
        return clipboardEntity;
    }
}
