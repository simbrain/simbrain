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
final class SensorSelectionModel implements Iterable<Sensor> {

    /** Listener list. */
    private final EventListenerList listenerList;

    /** Source of selection events. */
    private final VisionWorld visionWorld;

    /** Set of selected sensors. */
    private final Set<Sensor> selection;


    SensorSelectionModel(final VisionWorld visionWorld) {

        if (visionWorld == null) {
            throw new IllegalArgumentException("visionWorld must not be null");
        }
        this.visionWorld = visionWorld;
        selection = new HashSet<Sensor>();
        listenerList = new EventListenerList();
    }

    public int size() {
        return selection.size();
    }

    public void clear() {
        Set<Sensor> oldSelection = new HashSet<Sensor>(selection);
        selection.clear();
        if (!oldSelection.isEmpty()) {
            fireSelectionChanged(oldSelection, selection);
        }
    }

    public boolean isEmpty() {
        return selection.isEmpty();
    }

    public void add(final Sensor sensor) {
        Set<Sensor> oldSelection = new HashSet<Sensor>(selection);
        boolean rv = selection.add(sensor);
        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
    }

    public void addAll(final Collection<Sensor> sensors) {
        Set<Sensor> oldSelection = new HashSet<Sensor>(selection);
        boolean rv = selection.addAll(sensors);
        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
    }

    public void remove(final Sensor sensor) {
        Set<Sensor> oldSelection = new HashSet<Sensor>(selection);
        boolean rv = selection.remove(sensor);
        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
    }

    public void removeAll(final Collection<Sensor> sensors) {
        Set<Sensor> oldSelection = new HashSet<Sensor>(selection);
        boolean rv = selection.removeAll(sensors);
        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
    }

    public boolean isSelected(final Sensor sensor) {
        return selection.contains(sensor);
    }

    public void toggleSelection(final Sensor sensor) {
        if (isSelected(sensor)) {
            remove(sensor);
        }
        else {
            add(sensor);
        }
    }

    public Iterator<Sensor> iterator() {
        return getSelection().iterator();
    }

    public Collection<Sensor> getSelection() {
        return Collections.unmodifiableSet(selection);
    }

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

    public void addSensorSelectionListener(SensorSelectionListener listener) {
        listenerList.add(SensorSelectionListener.class, listener);
    }

    public void removeSensorSelectionListener(SensorSelectionListener listener) {
        listenerList.remove(SensorSelectionListener.class, listener);
    }

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
