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
package org.simbrain.network.listeners;

import java.util.EventObject;

import org.simbrain.network.interfaces.Network;

/**
 * Network event which holds an old and new version of some type of object.
 * 
 *
 * @param <T> The type of the object involved in the event.
 */
public final class NetworkEvent<T>
    extends EventObject {

    /** New version of object */
    private T theObject;

    /** Previous version of object. */
    private T oldObject;
    
    /** An auxiliarity object. */
    private Object auxiliaryObject;

	/**
     * Create a network event with a new and old object.
     *
     * @param net
     *            reference to parent network.
     * @param oldThing
     *            old version of object.
     * @param theThing
     *            reference to relevant object.
     */
    public NetworkEvent(Network net, final T oldThing, final T theThing) {
        super(net);
        this.theObject = theThing;
        this.oldObject = oldThing;
    }

    /**
     * Create a network event with one object only.
     *
     * @param net reference to parent network.
     * @param theThing reference to relevant object.
     */
    public NetworkEvent(Network net, final T theThing) {
        super(net);
        this.theObject = theThing;
    }


    /**
     * @return the newObject
     */
    public T getObject() {
        return theObject;
    }

    /**
     * @param newObject the newObject to set
     */
    public void setObject(T newObject) {
        this.theObject = newObject;
    }

    /**
     * @return the oldObject
     */
    public T getOldObject() {
        return oldObject;
    }

    /**
     * @param oldObject the oldObject to set
     */
    public void setOldObject(T oldObject) {
        this.oldObject = oldObject;
    }

    /**
     * Event's default getSource() just returns the same object
     * as this class's getObject() method.
     */
    @Override
    public Object getSource() {
        return theObject;
    }

    /**
     * @return the auxiliaryObject
     */
    public Object getAuxiliaryObject() {
        return auxiliaryObject;
    }

    /**
     * @param auxiliaryObject the auxiliaryObject to set
     */
    public void setAuxiliaryObject(Object auxiliaryObject) {
        this.auxiliaryObject = auxiliaryObject;
    }

}