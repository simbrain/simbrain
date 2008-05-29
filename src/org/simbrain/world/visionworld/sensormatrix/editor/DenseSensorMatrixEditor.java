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

import org.simbrain.world.visionworld.Filter;
import org.simbrain.world.visionworld.SensorMatrix;
import org.simbrain.world.visionworld.sensormatrix.DenseSensorMatrix;

/**
 * Dense sensor matrix editor.
 */
public final class DenseSensorMatrixEditor extends AbstractSensorMatrixEditor {
    /** serial version ID */
    private static final long serialVersionUID = 1L;
    
    @Override
    public SensorMatrix createSensorMatrix(int rows, int columns, int width, int height,
            Filter defaultFilter) throws SensorMatrixEditorException {
        return new DenseSensorMatrix(rows, columns, width, height, defaultFilter);
    }
}
