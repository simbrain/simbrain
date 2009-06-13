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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;

import org.simbrain.workspace.AbstractAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;

import org.simbrain.workspace.WorkspaceComponent;

/**
 * OSC message producer.
 */
final class OscMessageProducer
    implements Producer {

    /** OSC message address. */
    private final String address;

    /** Last matching OSC message argument. */
    private Double argument = Double.valueOf(0.0d);

    /** OSC listener. */
    private final OSCListener listener;

    /** OSC world component. */
    private final OscWorldComponent component;

    /** Producing attribute. */
    private final ProducingAttribute<Double> attribute;


    /**
     * Create a new OSC message producer with the specified OSC world component.
     *
     * @param address OSC message address, must not be null and must start with
     *    <code>'/'</code> character
     * @param component OSC world component, must not be null
     */
    OscMessageProducer(final String address, final OscWorldComponent component) {
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
        listener = new OSCListener() {
                /** {@inheritDoc} */
                public void acceptMessage(final Date time, final OSCMessage message) {
                    // TODO:  if (date is null or time previous to now)
                    dispatch(message);
                    // otherwise queue until time is reached
                }
            };

        this.component.getOscPortIn().addListener(this.address, listener);
    }


    /**
     * Dispatch the specified incoming OSC message to this OSC message producer.
     *
     * @param message incoming OSC message to dispatch
     */
    void dispatch(final OSCMessage message) {
        if (message.getArguments().length > 0) {
            try {
                Float f = (Float) message.getArguments()[0];
                argument = Double.valueOf(f.doubleValue());
            }
            catch (ClassCastException e) {
                // ignore
            }
            catch (NullPointerException e) {
                // ignore
            }
        }
    }

    /** {@inheritDoc} */
    public String getDescription() {
        return "OSCMessageProducer";
    }

    /** {@inheritDoc} */
    public WorkspaceComponent<?> getParentComponent() {
        return component;
    }

    /** {@inheritDoc} */
    public List<? extends ProducingAttribute<?>> getProducingAttributes() {
        return Collections.singletonList(attribute);
    }

    /** {@inheritDoc} */
    public String toString() {
        return address + ", f";
    }

    /**
     * Double attribute.
     */
    private final class DoubleAttribute extends AbstractAttribute implements ProducingAttribute<Double> {

        /** {@inheritDoc} */
        public String getKey() {
            return " " + address + ", f";
        }

        /** {@inheritDoc} */
        public Producer getParent() {
            return OscMessageProducer.this;
        }

        /** {@inheritDoc} */
        public Double getValue() {
            return argument;
        }
    }
}