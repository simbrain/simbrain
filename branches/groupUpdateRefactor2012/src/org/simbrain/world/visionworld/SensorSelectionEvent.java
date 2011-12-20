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
package org.simbrain.world.visionworld;

import java.util.Collections;
import java.util.EventObject;
import java.util.Set;

/**
 * Sensor selection event.
 */
public final class SensorSelectionEvent
    extends EventObject {

    /** Old set of selected sensors. */
    private final Set<Sensor> oldSelection;

    /** Set of selected sensors. */
    private final Set<Sensor> selection;


    /**
     * Create a new sensor selection event with the specified source.
     *
     * @param source source of this event
     * @param oldSelection old set of selected sensors, must not be null
     * @param selection set of selected sensors, must not be null
     */
    public SensorSelectionEvent(final VisionWorld source,
                                final Set<Sensor> oldSelection,
                                final Set<Sensor> selection) {

        super(source);
        if (oldSelection == null) {
            throw new IllegalArgumentException("oldSelection must not be null");
        }
        if (selection == null) {
            throw new IllegalArgumentException("selection must not be null");
        }
        this.oldSelection = oldSelection;
        this.selection = selection;
    }


    /**
     * Return the source of this event as a vision world.
     *
     * @return the source of this event as a vision world
     */
    public VisionWorld getVisionWorld() {
        return (VisionWorld) super.getSource();
    }

    /**
     * Return the old set of selected sensors.
     *
     * @return the old set of selected sensors
     */
    public Set<Sensor> getOldSelection() {
        return Collections.unmodifiableSet(oldSelection);
    }

    /**
     * Return the set of selected sensors.
     *
     * @return the set of selected sensors
     */
    public Set<Sensor> getSelection() {
        return Collections.unmodifiableSet(selection);
    }
}
