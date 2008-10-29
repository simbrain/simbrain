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
package org.simbrain.world.oscworld;

import java.lang.reflect.Type;

import java.io.IOException;

import java.util.Collections;
import java.util.List;

import com.illposed.osc.OSCMessage;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;

import org.simbrain.workspace.WorkspaceComponent;

/**
 * OSC message consumer.
 */
final class OscMessageConsumer
    implements Consumer {

    /** OSC message address. */
    private final String address;

    /** OSC message argument. */
    private Double argument;

    /** OSC world component. */
    private final OscWorldComponent component;

    /** Consuming attribute. */
    private final ConsumingAttribute<Double> attribute;


    /**
     * Create a new OSC message consumer with the specified OSC world component.
     *
     * @param address OSC message address, must not be null and must start with
     *    <code>'/'</code> character
     * @param component OSC world component, must not be null
     */
    OscMessageConsumer(final String address, final OscWorldComponent component) {
        if (address == null) {
            throw new IllegalArgumentException("address must not be null");
        }
        if (!address.startsWith("/")) {
            throw new IllegalArgumentException("address must start with '/' character");
        }
        if (component == null) {
            throw new IllegalArgumentException("component must not be null");
        }
        this.address = address;
        this.component = component;
        attribute = new DoubleAttribute();
    }


    /**
     * Set the argument for this OSC message consumer to <code>argument</code>
     * and send the resulting OSC message.
     *
     * @param argument OSC message argument, must not be null
     */
    void setArgument(final Double argument) {
        if (argument == null) {
            throw new IllegalArgumentException("argument must not be null");
        }
       this.argument = argument;
        try {
            OSCMessage message = new OSCMessage(address, new Object[] { new Float(argument) });
            component.getOscPortOut().send(message);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** {@inheritDoc} */
    public String getDescription() {
        return "OSCMessageConsumer";
    }

    /** {@inheritDoc} */
    public WorkspaceComponent<?> getParentComponent() {
        return component;
    }

    /** {@inheritDoc} */
    public List<? extends ConsumingAttribute<?>> getConsumingAttributes() {
        return Collections.singletonList(attribute);
    }

    /** {@inheritDoc} */
    public ConsumingAttribute<?> getDefaultConsumingAttribute() {
        return attribute;
    }

    /** {@inheritDoc} */
    public void setDefaultConsumingAttribute(final ConsumingAttribute<?> consumingAttribute) {
        throw new UnsupportedOperationException("default attribute is not modifiable");
    }

    /** {@inheritDoc} */
    public String toString() {
        return address + ", f";
    }

    /**
     * Double attribute.
     */
    private final class DoubleAttribute
        implements ConsumingAttribute<Double> {

        /** {@inheritDoc} */
        public String getAttributeDescription() {
            return " " + address + ", f";
        }

        /** {@inheritDoc} */
        public Type getType() {
            return Double.class;
        }

        /** {@inheritDoc} */
        public Consumer getParent() {
            return OscMessageConsumer.this;
        }

        /** {@inheritDoc} */
        public void setValue(final Double value) {
            setArgument(value);
        }
    }
}