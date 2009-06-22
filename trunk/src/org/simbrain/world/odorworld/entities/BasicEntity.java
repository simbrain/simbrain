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
package org.simbrain.world.odorworld.entities;

import org.simbrain.world.odorworld.OdorWorld;


/**
 * <b>BasicEntity</b> represents a basic entity in the Odor World environment.
 */
public class BasicEntity extends OdorWorldEntity {


    /**
     * Construct a basic entity.
     *
     * @param world reference to parent world
     * @param anim animation associated with this entity.
     */
    public BasicEntity(OdorWorld world, Animation anim) {
        super(world, anim);
    }

    /**
     * Updates this OdorWorldEntity's Animation and its position based on the velocity.
     */
    public void update(long elapsedTime) {
        x += dx * elapsedTime;
        y += dy * elapsedTime;
        anim.update(elapsedTime);
    }

    /**
     * Called before update() if the creature collided with a tile horizontally.
     */
    public void collideHorizontal() {
        setVelocityX(-getVelocityX());
    }

    /**
     * Called before update() if the creature collided with a tile vertically.
     */
    public void collideVertical() {
        setVelocityY(-getVelocityY());
    }

}
