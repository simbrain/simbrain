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
package org.simbrain.workspace.gui;

import javax.swing.JMenuItem;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Packages elements of a coupling into a JMenuItem.
 */
public class CouplingMenuItem extends JMenuItem {

    /** The default serial version ID. */
    private static final long serialVersionUID = 1L;

    /** Reference to producing attribute. */
    private Producer<?> producer = null;

    /** Reference to consuming attribute. */
    private Consumer<?> consumer = null;

    /** Reference to a coupling container. */
    private WorkspaceComponent component = null;

    /**
     * The type of menu item being created. These items can be used to draw
     * information from a single producer or consumer, or lists of either.
     */
    public enum EventType {
        /** Identifies a single producer list event. */
        PRODUCER_LIST,
        /** Identifies a single consumer list event. */
        CONSUMER_LIST
    }

    /** The event type for this event. */
    private final EventType eventType;

    /**
     * Creates a new instance.
     *
     * @param component The component that this menuItem belongs to.
     * @param type The type of event this menuItem should fire.
     */
    public CouplingMenuItem(final WorkspaceComponent component, final EventType type) {
        this.component = component;
        this.eventType = type;
        setSelected(true);
    }

    /**
     * @return the container
     */
    public WorkspaceComponent getWorkspaceComponent() {
        return component;
    }

    /**
     * @return the eventType
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * @return the producer
     */
    public Producer<?> getProducer() {
        return producer;
    }

    /**
     * @param producer the producer to set
     */
    public void setProducer(Producer<?> producer) {
        this.producer = producer;
    }

    /**
     * @return the consumer
     */
    public Consumer<?> getConsumer() {
        return consumer;
    }

    /**
     * @param consumer the consumer to set
     */
    public void setConsumer(Consumer<?> consumer) {
        this.consumer = consumer;
    }
}
