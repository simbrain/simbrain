package org.simbrain.world.threedee;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.world.threedee.Moveable.Action;
import org.simbrain.world.threedee.gui.AgentView;
import org.simbrain.world.threedee.sensors.Sight;
import org.simbrain.world.threedee.sensors.Smell;

/**
 * Acts as the consumer and producer associated with an Agent.
 * 
 * @author Matt Watson
 */
public class AgentBindings extends Bindings {
    /**
     * The priority of the agent.  There's nothing special about the number
     * 10.  It's just sufficiently high to allow others to preempt bindings.
     */
    private static final int PRIORITY = 10;

    /** The consumers for the wrapped agent. */
    private final List<ConsumingBinding> consumers 
        = Collections.synchronizedList(new ArrayList<ConsumingBinding>());

    /** The agent for these bindings. */
    private final Agent agent;
    
    /** Temporary strength variable. */
    private static final float STRENGTH = 1;

    private Sight sight;
    
    /**
     * Creates a new bindings object for the given agent
     * and component.
     *
     * @param agent the agent to bind to.
     * @param component the parent component.
     */
    AgentBindings(final Agent agent, final ThreeDeeComponent component) {
        super(component, "3D Agent " + agent.getName());

        this.agent = agent;
        
//        System.out.println(agent);
//        sight = new Sight(agent);
        
        setInputs();
    }
    
    public Sight createSight(AgentView view) {
        if (sight == null) {
            sight = new Sight(view, agent.getName(), getParentComponent().getWorkspace());
        } else {
            sight.createVisionWorld();
        }
        
        return sight;
    }
    
    void setInputs() {
        consumers.add(new ConsumingBinding("left", agent.left()));
        consumers.add(new ConsumingBinding("right", agent.right()));
        consumers.add(new ConsumingBinding("forward", agent.forward()));
        consumers.add(new ConsumingBinding("backward", agent.backward()));
        consumers.add(new ConsumingBinding("up", agent.up()));
        consumers.add(new ConsumingBinding("down", agent.down()));
        
        agent.addInput(PRIORITY, new AbstractCollection<Action>() {
            @Override
            public Iterator<Action> iterator() {
                if (doBind()) {
                    /*
                     * set the bind value to the on parameter.  This is not done until
                     * the iterator it to be created to ensure at least one iterator
                     * is returned every time the bindings are turned on.
                     */
                    update();
                    
                    final Iterator<ConsumingBinding> internal = consumers.iterator();
    
                    return new Iterator<Action>() {
    
                      public boolean hasNext() {
                          return internal.hasNext();
                      }
    
                      public Action next() {
                          return internal.next().getAction();
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
                return doBind() ? consumers.size() : 0;
            }
        });
    }
    
    /**
     * {@inheritDoc}
     */
    public List<? extends ProducingAttribute<?>> getProducingAttributes() {
        List<ProducingBinding> producing = new ArrayList<ProducingBinding>();
        
        for (String odorType : agent.getEnvironment().getOdors().getOdorTypes()) {
            producing.add(new ProducingBinding(new Smell(odorType, agent, 1f), "right"));
            producing.add(new ProducingBinding(new Smell(odorType, agent, -1f), "left"));
        }
        
        if (sight != null) {
            for (Sensor sensor : sight.getProducingAttributes()) {
                producing.add(new ProducingBinding(sensor, null));
            }
        }
        
        return producing;
    }

    protected void update() {
        super.update();
    }
    
    protected void updateExternal() {
        if (sight != null) sight.update();
    }
    
    /**
     * {@inheritDoc}
     */
    public List<? extends ConsumingAttribute<?>> getConsumingAttributes() {
        return Collections.unmodifiableList(consumers);
    }
}
