package org.simbrain.util.environment;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.util.environment.effectors.Effector;
import org.simbrain.util.environment.sensors.Sensor;
import org.simbrain.util.environment.sensors.SmellSensor;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Represents an agent in 2-d environment, with sensors, effectors, and simple
 * movement.
 * 
 * TODO: Possibly add:
 * 	- velocity
 *  - collision
 */
public class Agent implements TwoDEntity, Consumer, Producer {

    /** Current heading / orientation. */
    private double heading = DEFAULT_HEADING;

    /** Current Location. */
    private double[] location = new double[2];

    /** Initial heading of agent. */
    private final static double DEFAULT_HEADING = 300;

    /** List of things this agent can do. */
    private ArrayList<ConsumingAttribute<Double>> effectorList = new ArrayList<ConsumingAttribute<Double>>();

    /** List of things this agent can sense. */
    private ArrayList<SmellSensor> sensorList = new ArrayList<SmellSensor>();

    /** Environment this agent is embedded in. */
    private TwoDEnvironment environment;

    /** Name. */
    private String name;
    
	/**
	 * @param workspace
	 * @param location
	 * @param location
	 */
	public Agent(TwoDEnvironment environment, String name, double[] location) {
		super();
		this.environment = environment;
		this.location = location;
		this.name = name;	
	}
	
	/**
	 * @param heading
	 * @param location
	 * @param environment
	 * @param workspaceComponent
	 * @param name
	 */
	public Agent(TwoDEnvironment environment,
			String name,
			double heading, double[] location) {
		this.heading = heading;
		this.location = location;
		this.environment = environment;
		this.name = name;
	}

	/**
	 * @return the heading
	 */
	public double getHeading() {
		return heading;
	}

	/**
	 * @param heading the heading to set
	 */
	public void setHeading(double heading) {
		this.heading = heading;
	}
	
    /**
     * @return orientation in degrees
     */
    public double getHeadingRadians() {
        return (heading * Math.PI) / 180;
    }

	/**
	 * @return the location
	 */
	public double[] getSuggestedLocation() {
		return location;
	}

	//TODO: When the location is updated, the underlying location is not...
	/**
	 * @param location the location to set
	 */
	public void setSuggestedLocation(double[] location) {
		this.location = location;
	}

	/**
	 * @return the sensors
	 */
	public List<Sensor> getSensors() {
		return (List<Sensor>) getProducingAttributes();
	}

	/**
	 * @return the effectors
	 */
	public List<Effector> getEffectors() {
		return (List<Effector>) getConsumingAttributes();
	}

	/**
	 * @return the environment
	 */
	public TwoDEnvironment getEnvironment() {
		return environment;
	}

	/**
	 * @param environment the environment to set
	 */
	public void setEnvironment(TwoDEnvironment environment) {
		this.environment = environment;
	}

	/**
     * {@inheritDoc}
     */
	public List<? extends ConsumingAttribute<?>> getConsumingAttributes() {
		return effectorList;
	}

	/**
     * {@inheritDoc}
     */
	public List<? extends ProducingAttribute<?>> getProducingAttributes() {
		return sensorList;
	}

	/**
     * {@inheritDoc}
     */
	public String getDescription() {
		return name;
	}

	/**
     * {@inheritDoc}
     */
	public WorkspaceComponent getParentComponent() {
		return environment.getParent();
	}

	/**
     * {@inheritDoc}
     */
	public void update() {		
		for (Sensor sensor : getSensors()) {
			sensor.update();
		}
		// TODO: Why not this?
		// for (Effector effector: getEffectors()) {
		//	effector.update();
		//}
	}
}
