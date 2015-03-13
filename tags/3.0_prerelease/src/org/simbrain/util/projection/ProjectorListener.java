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
package org.simbrain.util.projection;

/**
 * Classes with implement this interface fire events indicating changes in the
 * status of a trainer.
 *
 * @author jyoshimi
 */
public interface ProjectorListener {

    /**
     * Fired when the projection method is changed.
     */
    void projectionMethodChanged();

    /**
     * Fired when a new datapoint is added to the projector.
     */
    void datapointAdded();

    /**
     * Fired when the the underlying data has been changed, e.g the projector
     * has been reinitailzed, data reset, etc.
     */
    void projectorDataChanged();

    /**
     * Fired when the colors of some datapoints have changed but nothing else.
     */
    void projectorColorsChanged();

}
