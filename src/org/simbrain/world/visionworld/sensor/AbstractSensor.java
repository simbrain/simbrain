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
package org.simbrain.world.visionworld.sensor;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import org.simbrain.world.visionworld.Coupling;
import org.simbrain.world.visionworld.Sensor;
import org.simbrain.world.visionworld.ReceptiveField;

/**
 * Abstract sensor.
 */
abstract class AbstractSensor
    implements Sensor {

    /** Receptive field for this sensor. */
    private final ReceptiveField receptiveField;

    /** Set of couplings for this sensor. */
    private Set<Coupling> couplings;


    /**
     * Create a new abstract sensor with the specified receptive field.
     *
     * @param receptiveField receptive field for this sensor, must not be null
     */
    protected AbstractSensor(final ReceptiveField receptiveField) {
        if (receptiveField == null) {
            throw new IllegalArgumentException("receptiveField must not be null");
        }
        this.receptiveField = receptiveField;
        couplings = new HashSet<Coupling>();
    }


    /** {@inheritDoc} */
    public ReceptiveField getReceptiveField() {
        return receptiveField;
    }

    /** {@inheritDoc} */
    public Set<Coupling> getCouplings() {
        return Collections.unmodifiableSet(couplings);
    }
}
