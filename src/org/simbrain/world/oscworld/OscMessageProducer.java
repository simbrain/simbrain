package org.simbrain.world.oscworld;

import java.lang.reflect.Type;

import java.io.IOException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.illposed.osc.OSCMessage;

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

    /** OSC world component. */
    private final OscWorldComponent component;

    /** Producing attribute. */
    private final ProducingAttribute<Double> attribute;


    /**
     * Create a new OSC message producer with the specified OSC world component.
     *
     * @param address OSC message address, must not be null and must start with <code>'/'</code> character
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
    }


    /**
     * Return true if the specified incoming OSC message matches the address of this
     * OSC message producer.  Note the matching rules of the OSC specification.
     *
     * @see http://opensoundcontrol.org/spec-1_0
     * @return true if the specified incoming OSC message matches the address of this
     *    OSC message producer
     */
    boolean matches(final OSCMessage message) {
        return false;
    }

    /**
     * Dispatch the specified incoming OSC message to this OSC message producer.
     *
     * @param message incoming OSC message to dispatch
     */
    void dispatch(final OSCMessage message) {
        //this.argument = message.getArgument(); ...
    }


    /** {@inheritDoc} */
    public String getDescription() {
        return "OSC Message";
    }

    /** {@inheritDoc} */
    public final WorkspaceComponent<?> getParentComponent() {
        return component;
    }

    /** {@inheritDoc} */
    public final List<? extends ProducingAttribute<?>> getProducingAttributes() {
        return Collections.singletonList(attribute);
    }

    /** {@inheritDoc} */
    public final ProducingAttribute<?> getDefaultProducingAttribute() {
        return attribute;
    }

    /** {@inheritDoc} */
    public final void setDefaultProducingAttribute(final ProducingAttribute<?> producingAttribute) {
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
        implements ProducingAttribute<Double> {

        /** {@inheritDoc} */
        public String getAttributeDescription() {
            return " " + address + ", f";
        }

        /** {@inheritDoc} */
        public Type getType() {
            return Double.class;
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