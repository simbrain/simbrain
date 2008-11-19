package org.simbrain.world.midiworld;

import java.lang.reflect.Type;

import java.util.Collections;
import java.util.List;

import org.simbrain.workspace.AbstractAttribute;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;

import org.simbrain.workspace.WorkspaceComponent;

/**
 * Abstract MIDI consumer.
 */
abstract class AbstractMidiConsumer
    implements Consumer {

    /** Trigger attribute.  Any value above <code>0.0d</code> enables the trigger. */
    private final ConsumingAttribute<Double> trigger;

    /** MIDI world component. */
    private final MidiWorldComponent component;


    /**
     * Create a new abstract MIDI consumer with the specified MIDI world component.
     *
     * @param component MIDI world component, must not be null
     */
    protected AbstractMidiConsumer(final MidiWorldComponent component) {
        if (component == null) {
            throw new IllegalArgumentException("component must not be null");
        }
        this.component = component;
        trigger = new TriggerAttribute();
    }


    /**
     * Notify subclasses the value for the trigger attribute has been set
     * to a value above <code>0.0d</code>.
     *
     * @param value new trigger value
     */
    protected abstract void trigger(final double value);

    /**
     * Return the parent component for this consumer as a MidiWorldComponent.
     * Saves subclasses from having to cast the result of <code>getParentComponent()</code>.
     *
     * @return the parent component for this consumer as a MidiWorldComponent
     */
    protected final MidiWorldComponent getMidiWorldComponent() {
        return component;
    }

    /** {@inheritDoc} */
    public final WorkspaceComponent<?> getParentComponent() {
        return component;
    }

    /** {@inheritDoc} */
    public final List<? extends ConsumingAttribute<?>> getConsumingAttributes() {
        return Collections.singletonList(trigger);
    }

    /** {@inheritDoc} */
    public final ConsumingAttribute<?> getDefaultConsumingAttribute() {
        return trigger;
    }

    /** {@inheritDoc} */
    public final void setDefaultConsumingAttribute(final ConsumingAttribute<?> consumingAttribute) {
        throw new UnsupportedOperationException("default attribute is not modifiable");
    }

    /**
     * Trigger attribute.
     */
    private final class TriggerAttribute extends AbstractAttribute implements ConsumingAttribute<Double> {

        /** Value. */
        private Double value = 0.0d;

        /** {@inheritDoc} */
        public String getKey() {
            return "Trigger Attribute";
        }

        /** {@inheritDoc} */
        public Type getType() {
            return Double.class;
        }

        /** {@inheritDoc} */
        public Consumer getParent() {
            return AbstractMidiConsumer.this;
        }

        /** {@inheritDoc} */
        public void setValue(final Double value) {
            if (value == null) {
                throw new IllegalArgumentException("value must not be null");
            }
            this.value = value;
            if (this.value.doubleValue() > 0.0d) {
                trigger(this.value.doubleValue());
            }
        }
    }
}