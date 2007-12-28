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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.event.EventListenerList;

/**
 * Sensor selection model.
 */
public final class SensorSelectionModel implements Iterable<Sensor> {

    /** Listener list. */
    private final EventListenerList listenerList;

    /** Source of selection events. */
    private final VisionWorld visionWorld;

    /** Set of selected sensors. */
    private final Set<Sensor> selection;


    /**
     * Create a new sensor selection model with the specified vision world.
     *
     * @param visionWorld vision world for this sensor selection model,
     *    must not be null
     */
    SensorSelectionModel(final VisionWorld visionWorld) {
        if (visionWorld == null) {
            throw new IllegalArgumentException("visionWorld must not be null");
        }
        this.visionWorld = visionWorld;
        selection = new HashSet<Sensor>();
        listenerList = new EventListenerList();
    }


    /**
     * Return the number of selected sensors for this sensor selection model.
     *
     * @return the number of selected sensors for this sensor selection model
     */
    public int size() {
        return selection.size();
    }

    /**
     * Clear this sensor selection model.
     */
    public void clear() {
        Set<Sensor> oldSelection = new HashSet<Sensor>(selection);
        selection.clear();
        if (!oldSelection.isEmpty()) {
            fireSelectionChanged(oldSelection, selection);
        }
    }

    /**
     * Return true if this sensor selection model is empty.
     *
     * @return true if this sensor selection model is empty
     */
    public boolean isEmpty() {
        return selection.isEmpty();
    }

    /**
     * Add the specified sensor to this sensor selection model.
     *
     * @param sensor sensor to add
     */
    public void add(final Sensor sensor) {
        Set<Sensor> oldSelection = new HashSet<Sensor>(selection);
        boolean rv = selection.add(sensor);
        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
    }

    /**
     * Add all of the specified collection of sensors to this sensor selection model.
     *
     * @param sensors collection of sensors to add
     */
    public void addAll(final Collection<Sensor> sensors) {
        Set<Sensor> oldSelection = new HashSet<Sensor>(selection);
        boolean rv = selection.addAll(sensors);
        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
    }

    /**
     * Remove the specified sensor from this sensor selection model.
     *
     * @param sensor sensor to remove
     */
    public void remove(final Sensor sensor) {
        Set<Sensor> oldSelection = new HashSet<Sensor>(selection);
        boolean rv = selection.remove(sensor);
        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
    }

    /**
     * Remove all of the specified collection of sensors from this sensor selection model.
     *
     * @param sensors collection of sensors to remove
     */
    public void removeAll(final Collection<Sensor> sensors) {
        Set<Sensor> oldSelection = new HashSet<Sensor>(selection);
        boolean rv = selection.removeAll(sensors);
        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
    }

    /**
     * Return true if the specified sensor is selected.
     *
     * @return true if the specified sensor is selected
     */
    public boolean isSelected(final Sensor sensor) {
        return selection.contains(sensor);
    }

    /**
     * Toggle the selection state for the specified sensor.
     *
     * @param sensor sensor to toggle
     */
    public void toggleSelection(final Sensor sensor) {
        if (isSelected(sensor)) {
            remove(sensor);
        }
        else {
            add(sensor);
        }
    }

    /**
     * Return an iterator over the selected sensors.
     *
     * @return an iterator over the selected sensors
     */
    public Iterator<Sensor> iterator() {
        return getSelection().iterator();
    }

    /**
     * Return an unmodifiable collection of selected sensors.
     *
     * @return an unmodifiable collection of selected sensors
     */
    public Collection<Sensor> getSelection() {
        return Collections.unmodifiableSet(selection);
    }

    /**
     * Set the selection for this sensor selection model to the specified
     * collection of sensors.
     *
     * @param sensors collection of selected sensors
     */
    public void setSelection(final Collection<Sensor> sensors) {
        if (selection.isEmpty() && sensors.isEmpty()) {
            return;
        }
        Set<Sensor> oldSelection = new HashSet<Sensor>(selection);
        selection.clear();
        boolean rv = selection.addAll(sensors);
        if (rv || sensors.isEmpty()) {
            fireSelectionChanged(oldSelection, selection);
        }
    }

    /**
     * Add the specified sensor selection listener.
     *
     * @param listener listener to add
     */
    public void addSensorSelectionListener(final SensorSelectionListener listener) {
        listenerList.add(SensorSelectionListener.class, listener);
    }

    /**
     * Remove the specified sensor selection listener.
     *
     * @param listener listener to remove
     */
    public void removeSensorSelectionListener(SensorSelectionListener listener) {
        listenerList.remove(SensorSelectionListener.class, listener);
    }

    /**
     * Fire a selection changed event.
     *
     * @param oldSelection old selection
     * @param selection new selection
     */
    public void fireSelectionChanged(final Set<Sensor> oldSelection, final Set<Sensor> selection) {
        Object[] listeners = listenerList.getListenerList();
        SensorSelectionEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SensorSelectionListener.class) {
                if (e == null) {
                    e = new SensorSelectionEvent(visionWorld, oldSelection, selection);
                }
                ((SensorSelectionListener) listeners[i + 1]).selectionChanged(e);
            }
        }
    }
}
