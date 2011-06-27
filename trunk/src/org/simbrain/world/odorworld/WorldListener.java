/*
* Part of Simbrain--a java-based neural network kit
* Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General License for more details.
*
* You should have received a copy of the GNU General License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package org.simbrain.world.odorworld;

import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Sensor;

/**
 * Listener for receiving odor world events.
 */
interface WorldListener {

    /**
     * World updated.
     */
    void updated();

    /**
     * An entity was added to the world.
     *
     * @param entity the entity that was added
     */
    void entityAdded(final OdorWorldEntity entity);

    /**
     * An entity was changed.
     *
     * @param entity the entity that was changed
     */
    void entityChanged(final OdorWorldEntity entity);

    /**
     * An entity was removed to the world.
     *
     * @param entity the entity that was removed
     */
    void entityRemoved(final OdorWorldEntity entity);

    /**
     * A sensor was added.
     *
     * @param sensor the new sensor
     */
    void sensorAdded(final Sensor sensor);

    /**
     * A sensor was removed.
     *
     * @param sensor the removed sensor
     */
    void sensorRemoved(final Sensor sensor);

    /**
     * An effector was added.
     *
     * @param effector the effector that was removed
     */
    void effectorRemoved(final Effector effector);

    /**
     * An effector was added.
     *
     * @param effector the effector that was added
     */
    void effectorAdded(final Effector effector);

    /**
     * Some world parameter has changed.
     */
    void propertyChanged();

}
