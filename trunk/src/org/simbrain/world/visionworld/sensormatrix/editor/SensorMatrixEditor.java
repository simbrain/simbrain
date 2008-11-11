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

import java.awt.Component;

import org.simbrain.world.visionworld.Filter;
import org.simbrain.world.visionworld.SensorMatrix;

/**
 * Sensor matrix editor.
 */
public interface SensorMatrixEditor {

    /**
     * Return the editor component for this sensor matrix editor.
     * The editor component will not be null.
     *
     * @return the editor component for this sensor matrix editor
     */
    Component getEditorComponent();
    
    /**
     * Create a new instance of SensorMatrix from the properties of this
     * sensor matrix editor and the specified default filter.  The sensor matrix
     * will not be null.
     *
     * @param defaultFilter default filter
     * @return a new instance of SensorMatrix created from the properties
     *    of this sensor matrix editor
     * @throws SensorMatrixEditorException if a SensorMatrix cannot properly be
     *    created from the properties of this sensor matrix editor
     */
    SensorMatrix createSensorMatrix(final Filter defaultFilter) throws SensorMatrixEditorException;
}
