package org.simbrain.util.environment.effectors;

import java.lang.reflect.Type;

import org.simbrain.util.environment.Agent;
import org.simbrain.workspace.AbstractAttribute;
import org.simbrain.workspace.ConsumingAttribute;

/**
 * An effector which changes the position of a 2-d object.
 */
public abstract class Effector extends AbstractAttribute implements ConsumingAttribute<Double> {

    /** Name. */
    private String name;

    /** Reference to parent agent. */
	private Agent agent;
    		
    /**
     * Construct a sensor.
     *
     * @param parent reference
     * @param sensorName name
     * @param dim stimulus dimension
     */
    public Effector(Agent agent, String name) {
    	this.agent = agent;
        this.name = name;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getKey() {
    	return name;
    }
    
    /**
     * Name functions as the key and description for this object.
     */
    String getName() {
        return name;
    }
    
    /**
     * {@inheritDoc}
     */
 	public Agent getParent() {
		return agent;
	}

    /**
     * {@inheritDoc}
     */
 	public Type getType() {
		return null;
	}

}
