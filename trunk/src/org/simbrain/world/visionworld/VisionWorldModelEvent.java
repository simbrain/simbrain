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

import java.util.EventObject;

/**
 * Vision world model event.
 */
public final class VisionWorldModelEvent
    extends EventObject {

    /**
     * Create a new vision world model event with the specified event source.
     *
     * @param source source of this event, must not be null
     */
    public VisionWorldModelEvent(final VisionWorldModel source) {
        super(source);
    }


    /**
     * Return the source of this event as a vision world model.
     * The vision world model will not be null.
     *
     * @return the source of this event as a vision world model
     */
    public VisionWorldModel getVisionWorldModel() {
        return (VisionWorldModel) super.getSource();
    }
}
