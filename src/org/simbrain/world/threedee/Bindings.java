package org.simbrain.world.threedee;

import java.lang.reflect.Type;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.simbrain.workspace.Attribute;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.world.threedee.Moveable.Action;

class Bindings implements Consumer, Producer {

    private final ThreeDeeComponent component;

    private final List<ConsumingBinding> consumers = new ArrayList<ConsumingBinding>();

    private ConsumingAttribute defaultConsumingAttribute = null;

    /**
     * 
     * @param agent
     * @param component
     */
    Bindings(Agent agent, ThreeDeeComponent component) {

        this.component = component;
        
        consumers.add(new ConsumingBinding("left", agent.left()));
        consumers.add(new ConsumingBinding("right", agent.right()));
        consumers.add(new ConsumingBinding("forward", agent.forward()));
        consumers.add(new ConsumingBinding("backward", agent.backward()));
        
        defaultConsumingAttribute = consumers.get(0);

        agent.addInput(10, new AbstractCollection<Action>(){
            @Override
            public Iterator<Action> iterator() {
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
            }

            @Override
            public int size() {
                return consumers.size();
            }
        });
    }
    
    private abstract class Binding implements Attribute
    {
        final String description;
        
        Binding(String description) {
            this.description = description;
        }
        
        public Bindings getParent() {
            return Bindings.this;
        }
        
        public String getAttributeDescription() {
            return description;
        }
        
        public Type getType() {
            return Float.TYPE;
        }
    }
    
//    private abstract class ProducingBinding extends Binding implements ProducingAttribute<Float> {
//        ProducingBinding(String description) {
//            super(description);
//        }
//    }
    
    private class ConsumingBinding extends Binding implements ConsumingAttribute<Double> {
        final Action action;
        
        ConsumingBinding(String description, Action action) {
            super(description);
            
            this.action = action;
            action.setValue(0);
        }

        public void setValue(Double value) {
            action.setValue(value.floatValue());
        }
    }
    
    public ProducingAttribute<?> getDefaultProducingAttribute() {
        // TODO is there a sensible choice?
        return null;
    }

    public List<? extends ProducingAttribute<?>> getProducingAttributes() {
        return Collections.emptyList();
    }

    public String getDescription() {
        return "3D Agent";
    }

    public ConsumingAttribute<?> getDefaultConsumingAttribute() {
        return defaultConsumingAttribute;
    }

    public List<? extends ConsumingAttribute<?>> getConsumingAttributes() {
        return Collections.unmodifiableList(consumers);
    }

    public void setDefaultConsumingAttribute(ConsumingAttribute consumingAttribute) {
        defaultConsumingAttribute = consumingAttribute;
    }

    public void setDefaultProducingAttribute(ProducingAttribute<?> producingAttribute) {
        // TODO Auto-generated method stub        
    }
    public ThreeDeeComponent getParentComponent() {
        return component;
    }

}
