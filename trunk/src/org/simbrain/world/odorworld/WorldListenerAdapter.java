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
 * Adapter for world listener class.
 *
 * @author Jeff Yoshimi
 *
 */
public class WorldListenerAdapter implements WorldListener {

    @Override
    public void updated() {
    }

    @Override
    public void entityAdded(OdorWorldEntity entity) {
    }

    @Override
    public void entityChanged(OdorWorldEntity entity) {
    }

    @Override
    public void entityRemoved(OdorWorldEntity entity) {
    }

    @Override
    public void sensorAdded(Sensor sensor) {
    }

    @Override
    public void sensorRemoved(Sensor sensor) {
    }

    @Override
    public void effectorRemoved(Effector effector) {
    }

    @Override
    public void effectorAdded(Effector effector) {
    }

    @Override
    public void propertyChanged() {
    }

}
