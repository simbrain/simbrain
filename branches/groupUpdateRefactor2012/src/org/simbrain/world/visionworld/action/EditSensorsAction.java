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
package org.simbrain.world.visionworld.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.world.visionworld.SensorSelectionEvent;
import org.simbrain.world.visionworld.SensorSelectionListener;
import org.simbrain.world.visionworld.VisionWorld;

/**
 * Edit sensors action.
 */
public final class EditSensorsAction
    extends AbstractAction {

    /** Vision world. */
    private final VisionWorld visionWorld;


    /**
     * Create a new edit sensors action.
     *
     * @param visionWorld vision world, must not be null
     */
    public EditSensorsAction(final VisionWorld visionWorld) {
        super("Edit selected sensor(s)...");
        if (visionWorld == null) {
            throw new IllegalArgumentException("visionWorld must not be null");
        }
        this.visionWorld = visionWorld;
        this.visionWorld.getSensorSelectionModel().addSensorSelectionListener(new SelectionListener());
    }


    /** {@inheritDoc} */
    public void actionPerformed(final ActionEvent event) {
        visionWorld.editSensors();
    }

    /**
     * Selection listener.
     */
    private class SelectionListener implements SensorSelectionListener {

        /** {@inheritDoc} */
        public void selectionChanged(final SensorSelectionEvent event) {
            setEnabled(!event.getSelection().isEmpty());
        }
    }
}
