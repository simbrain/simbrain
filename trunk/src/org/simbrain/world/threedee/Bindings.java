package org.simbrain.world.threedee;

import java.lang.reflect.Type;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.simbrain.workspace.AbstractAttribute;
import org.simbrain.workspace.Attribute;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.world.threedee.Moveable.Action;

/**
 * Acts as the consumer and producer associated with an Agent.
 * 
 * @author Matt Watson
 */
class Bindings implements Consumer, Producer {
    /**
     * the priority of the agent.  There's nothing special about the number
     * 10.  It's just sufficiently high to allow others to preempt bindings.
     */
    private static final int PRIORITY = 10;

    /** the component associated with this binding. */
    private final ThreeDeeComponent component;
    
    /** the consumers for the wrapped agent. */
    private final List<ConsumingBinding> consumers = new ArrayList<ConsumingBinding>();

    /** The default consuming attribute for this set of Bindings. */
    private ConsumingAttribute<?> defaultConsumingAttribute = null;

    /** Whether the bindings should be applied to the agent. */
    private volatile boolean on = false;
    /** Once on has been set, the bindings remain on until the next iterator call is made. */
    private volatile boolean bind = false;
    
    /**
     * Creates a new bindings object for the given agent
     * and component.
     *
     * @param agent the agent to bind to.
     * @param component the parent component.
     */
    Bindings(final Agent agent, final ThreeDeeComponent component) {
        this.component = component;

        consumers.add(new ConsumingBinding("left", agent.left()));
        consumers.add(new ConsumingBinding("right", agent.right()));
        consumers.add(new ConsumingBinding("forward", agent.forward()));
        consumers.add(new ConsumingBinding("backward", agent.backward()));
        defaultConsumingAttribute = consumers.get(0);
        
        agent.addInput(PRIORITY, new AbstractCollection<Action>() {
            @Override
            public Iterator<Action> iterator() {
                if (bind) {
                    /*
                     * set the bind value to the on parameter.  This is not done until
                     * the iterator it to be created to ensure at least one iterator
                     * is returned every time the bindings are turned on.
                     */
                    bind = on;
                    
                    final Iterator<ConsumingBinding> internal = consumers.iterator();
    
                    return new Iterator<Action>() {
    
                      public boolean hasNext() {
                          return internal.hasNext();
                      }
    
                      public Action next() {
                          return internal.next().action;
                      }
    
                      public void remove() {
                          throw new UnsupportedOperationException();
                      }
                   };
                } else {
                    return Collections.<Action>emptySet().iterator();
                }
            }

            @Override
            public int size() {
                return bind ? consumers.size() : 0;
            }
        });
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
     * Binds to a single Action on an Agent.
     *
     * @author Matt Watson
     */
    private abstract class Binding extends AbstractAttribute {
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
    private class ConsumingBinding extends Binding implements ConsumingAttribute<Double> {
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
         * {@inheritDoc}
         */
        public void setValue(final Double value) {
            action.setValue(value.floatValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    public ProducingAttribute<?> getDefaultProducingAttribute() {
        // TODO is there a sensible choice?
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public List<? extends ProducingAttribute<?>> getProducingAttributes() {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "3D Agent";
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
    public List<? extends ConsumingAttribute<?>> getConsumingAttributes() {
        return Collections.unmodifiableList(consumers);
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
    public void setDefaultProducingAttribute(final ProducingAttribute<?> producingAttribute) {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    public ThreeDeeComponent getParentComponent() {
        return component;
    }
}
