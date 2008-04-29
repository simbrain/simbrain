package org.simbrain.world.threedee;

import java.lang.reflect.Type;

import org.simbrain.workspace.AbstractAttribute;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.threedee.Moveable.Action;

/**
 * Acts as the consumer and producer associated with an Agent.
 * 
 * @author Matt Watson
 */
abstract class Bindings implements Consumer, Producer {
    /** An arbitrary prime number. */
    private static final int ARBITRARY_PRIME = 29;
    
    /** The description of the bindings. */
    private final String description;
    
    /** the component associated with this binding. */
    private ThreeDeeComponent component;
    
    /** Whether the bindings should be applied to the agent. */
    private volatile boolean on = false;
    /** Once on has been set, the bindings remain on until the next iterator call is made. */
    private volatile boolean bind = false;
    
    /** The default consuming attribute for this set of Bindings. */
    private ConsumingAttribute<?> defaultConsumingAttribute = null;
    
    /** The default consuming attribute for this set of Bindings. */
    private ProducingAttribute<?> defaultProducingAttribute = null;
    
    /**
     * Creates a new bindings object for the given agent
     * and component.
     *
     * @param component The parent component.
     * @param description The description of these bindings.
     */
    Bindings(final ThreeDeeComponent component, final String description) {
        this.component = component;
        this.description = description;
    }

    void setComponent(ThreeDeeComponent parent) {
        this.component = parent;
    }
    
    /**
     * Turns the bindings on or off depending on the value of on.
     * 
     * @param on True to turn on and false to turn off.
     */
    public void setOn(final boolean on) {
        this.on = on;
        if (!bind) { bind = on; }
    }
    
    /**
     * Returns whether these bindings are on.
     * 
     * @return Whether these bindings are on.
     */
    protected boolean isOn() {
        return on;
    }
    
    /**
     * Sets bind to the value of on.
     */
    protected void setBindToOn() {
        this.bind = isOn();
    }
    
    /**
     * Returns whether bindings are being executed.
     * 
     * @return Whether bindings are being executed.
     */
    protected boolean doBind() {
        return bind;
    }
    
    /**
     * {@inheritDoc}
     */
    public WorkspaceComponent<?> getParentComponent() {
        return component;
    }
    
    /**
     * Binds to a single Action on an Agent.
     *
     * @author Matt Watson
     */
    protected abstract class Binding extends AbstractAttribute {
        /** The description for this Binding. */
        private final String description;

        /**
         * Creates a new Binding.
         * 
         * @param description The description for the Binding.
         */
        Binding(final String description) {
            this.description = description;
        }

        /**
         * {@inheritDoc}
         */
        public Bindings getParent() {
            return Bindings.this;
        }

        /**
         * {@inheritDoc}
         */
        public String getAttributeDescription() {
            return description;
        }

        /**
         * {@inheritDoc}
         */
        public Type getType() {
            return Float.TYPE;
        }
    }

    /**
     * Implements a consumer binding.
     *
     * @author Matt Watson
     */
    protected class ConsumingBinding extends Binding implements ConsumingAttribute<Double> {
        /** The action this binding is bound to. */
        private final Action action;

        /**
         * Creates a new ConsumingBinding.
         * 
         * @param description The description of the Binding.
         * @param action The action this Binding is bound to.
         */
        ConsumingBinding(final String description, final Action action) {
            super(description);

            this.action = action;
            action.setValue(0);
        }

        /**
         * Returns the action for this Binding.
         * 
         * @return The action for this Binding.
         */
        protected Action getAction() {
            return action;
        }
        
        /**
         * {@inheritDoc}
         */
        public void setValue(final Double value) {
            action.setValue(value.floatValue());
        }
    }
    
    /**
     * Implements a consumer binding.
     *
     * @author Matt Watson
     */
    protected class ProducingBinding extends Binding implements ProducingAttribute<Double> {
        /** The action this binding is bound to. */
        private final Sensor sensor;

        /**
         * Creates a new ConsumingBinding.
         * 
         * @param sensor The sensor this Binding is bound to.
         */
        protected ProducingBinding(final Sensor sensor, String description) {
            super(description + " " + sensor.getDescription());

            this.sensor = sensor;
        }

        /**
         * {@inheritDoc}
         */
        public Double getValue() {
            return sensor.getValue();
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return ARBITRARY_PRIME * sensor.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof ProducingBinding)) return false;
            
            ProducingBinding other = (ProducingBinding) obj;
            
            return sensor.equals(other.sensor);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public String getDescription() {
//        return "3D Agent";
        return description;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setDefaultConsumingAttribute(final ConsumingAttribute<?> consumingAttribute) {
        defaultConsumingAttribute = consumingAttribute;
    }
    
    /**
     * {@inheritDoc}
     */
    public ConsumingAttribute<?> getDefaultConsumingAttribute() {
        return defaultConsumingAttribute;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setDefaultProducingAttribute(final ProducingAttribute<?> producingAttribute) {
        this.defaultProducingAttribute = producingAttribute;
    }
    
    /**
     * {@inheritDoc}
     */
    public ProducingAttribute<?> getDefaultProducingAttribute() {
        return defaultProducingAttribute;
    }
}
