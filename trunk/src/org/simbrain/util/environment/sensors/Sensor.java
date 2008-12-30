package org.simbrain.util.environment.sensors;

import java.lang.reflect.Type;

import org.simbrain.util.environment.Agent;
import org.simbrain.workspace.AbstractAttribute;
import org.simbrain.workspace.ProducingAttribute;

/**
 * An abstract sensor which detects changes in a 2-d environment.
 */
public abstract class Sensor extends AbstractAttribute implements ProducingAttribute<Double> {

    /** Parent. */
    private Agent parent;

    /** Name. */
    private String name;
    
	/**
	 * @param environment
	 * @param parent
	 * @param name
	 */
	public Sensor(final Agent agent, final String name) {
		this.parent = agent;
		this.name = name;
	}

	/**
	 * Functions as key and description.
	 */
	String getName() {
        return name;
    }
    
	/**
     * {@inheritDoc}
     */
	public Agent getParent() {
		return parent;
	}
	
	/**
     * {@inheritDoc}
     */
	public abstract void update();
   
}
