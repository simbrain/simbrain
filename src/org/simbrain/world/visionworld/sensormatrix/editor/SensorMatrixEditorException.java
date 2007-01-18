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
package org.simbrain.world.visionworld.sensormatrix.editor;

/**
 * Sensor matrix editor exception.
 */
public final class SensorMatrixEditorException
    extends Exception {

    /**
     * Create a new sensor matrix editor exception.
     */
    public SensorMatrixEditorException() {
        super();
    }

    /**
     * Create a new sensor matrix editor exception with the specified message.
     *
     * @param message message
     */
    public SensorMatrixEditorException(final String message) {
        super(message);
    }

    /**
     * Create a new sensor matrix editor exception with the specified cause.
     *
     * @param cause cause
     */
    public SensorMatrixEditorException(final Throwable cause) {
        super(cause);
    }

    /**
     * Create a new sensor matrix editor exception with the specified message and cause.
     *
     * @param message message
     * @param cause cause
     */
    public SensorMatrixEditorException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
