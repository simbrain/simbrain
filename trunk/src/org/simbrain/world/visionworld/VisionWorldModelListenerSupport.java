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

import javax.swing.event.EventListenerList;

/**
 * Vision world model listener support.
 */
class VisionWorldModelListenerSupport {

    /** Event source. */
    private VisionWorldModel source;

    /** Listener list. */
    private final EventListenerList listenerList;


    /**
     * Create a new vision world model listener support class meant
     * to be subclassed.  The subclass should call <code>setSource(this)</code>
     * before calling any of the <code>fireX</code> methods.
     */
    protected VisionWorldModelListenerSupport() {
        listenerList = new EventListenerList();
    }

    /**
     * Create a new vision world model listener support class with the
     * specified VisionWorldModel as the source of events.
     *
     * @param source the event source, must not be null
     */
    public VisionWorldModelListenerSupport(final VisionWorldModel source) {
        this();
        setSource(source);
    }


    /**
     * Set the source of vision world model events to <code>source</code>.
     *
     * @param source the event source, must not be null
     */
    protected final void setSource(final VisionWorldModel source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        this.source = source;
    }

    /**
     * Return the <code>EventListenerList</code> backing this vision world
     * model listener support class.
     *
     * @return the <code>EventListenerList</code> backing this vision world
     *    model listener support class
     */
    protected final EventListenerList getEventListenerList() {
        return listenerList;
    }

    /**
     * Add the specified vision world model listener.
     *
     * @param listener vision world model listener to add
     */
    public final void addModelListener(final VisionWorldModelListener listener) {
        listenerList.add(VisionWorldModelListener.class, listener);
    }

    /**
     * Remove the specified vision world model listener.
     *
     * @param listener vision world model listener to remove
     */
    public final void removeModelListener(final VisionWorldModelListener listener) {
        listenerList.remove(VisionWorldModelListener.class, listener);
    }

    /**
     * Fire a pixel matrix changed event to all registered vision world model
     * listeners.
     *
     * @param oldPixelMatrix old pixel matrix, must not be null
     * @param pixelMatrix new pixel matrix, must not be null
     */
    public final void firePixelMatrixChanged(final PixelMatrix oldPixelMatrix,
                                             final PixelMatrix pixelMatrix) {
        if (oldPixelMatrix == null) {
            throw new IllegalArgumentException("oldPixelMatrix must not be null");
        }
        if (pixelMatrix == null) {
            throw new IllegalArgumentException("pixelMatrix must not be null");
        }
        Object[] listeners = listenerList.getListenerList();
        VisionWorldModelEvent event = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == VisionWorldModelListener.class) {
                if (event == null) {
                    event = new VisionWorldModelEvent(source, oldPixelMatrix, pixelMatrix);
                }
                ((VisionWorldModelListener) listeners[i + 1]).pixelMatrixChanged(event);
            }
        }
    }

    /**
     * Fire a sensor matrix added event to all registered vision world model
     * listeners.
     *
     * @param sensorMatrix added sensor matrix, must not be null
     */
    public final void fireSensorMatrixAdded(final SensorMatrix sensorMatrix) {
        if (sensorMatrix == null) {
            throw new IllegalArgumentException("sensorMatrix must not be null");
        }
        Object[] listeners = listenerList.getListenerList();
        VisionWorldModelEvent event = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == VisionWorldModelListener.class) {
                if (event == null) {
                    event = new VisionWorldModelEvent(source, sensorMatrix);
                }
                ((VisionWorldModelListener) listeners[i + 1]).sensorMatrixAdded(event);
            }
        }
    }

    /**
     * Fire a sensor matrix removed event to all registered vision world model
     * listeners.
     *
     * @param sensorMatrix removed sensor matrix, must not be null
     */
    public final void fireSensorMatrixRemoved(final SensorMatrix sensorMatrix) {
        if (sensorMatrix == null) {
            throw new IllegalArgumentException("sensorMatrix must not be null");
        }
        Object[] listeners = listenerList.getListenerList();
        VisionWorldModelEvent event = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == VisionWorldModelListener.class) {
                if (event == null) {
                    event = new VisionWorldModelEvent(source, sensorMatrix);
                }
                ((VisionWorldModelListener) listeners[i + 1]).sensorMatrixRemoved(event);
            }
        }
    }
}
