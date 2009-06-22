package org.simbrain.world.odorworld.sensors;

import java.util.List;

import org.simbrain.util.SimbrainMath;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * A sensor which detector a specified component of a set of stimulus vectors.
 */
public class SmellSensor implements Sensor {

    /** Which dimension of the stimulus to read. */
    private final int dimension;
        
	/** Current value of this sensor. */ 
	private Double currentValue = new Double(0);

    /** Relative location of the sensor in polar coordinates. */
    private double theta = DEFAULT_THETA;
    
    /** Angle of whisker in radians. */
    public static double DEFAULT_THETA = Math.PI / 4;

    /** Initial length of mouse whisker. */
    private final double DEFAULT_RADIUS= 23;

    /** Relative location of the sensor in polar coordinates. */
    private double radius = DEFAULT_RADIUS;
 
   /**
     * Construct a sensor.
     *
     * @param parent reference
     * @param sensorName name
     * @param dim stimulus dimension
     */
    public SmellSensor(OdorWorldEntity parent, String name, final int dim, final double theta) {
        this.dimension = dim;
        this.theta = theta;
    }

    /**
     * Which dimension of a smell source vector to read.
     * @return the dimension to read.
     */
    public int getDimension() {
        return dimension;
    }

	/**
	 * @return the location
	 */
	public double[] getLocation() {
//		int x = (int) (getParent().getSuggestedLocation()[0] + (radius * Math.cos(theta)));
//		int y = (int) (getParent().getSuggestedLocation()[1] - (radius * Math.sin(theta)));
		return new double[]{1, 1};		
	}
	
	/**
	 *  Update the current value of this sensor.
	 *  
	 *  TODO: will cause problems if an agent has a smell.  Must check as noted below.
	 */
	public void update() {
		// Maybe a check so that only attributes which are bound are updated?
		Double val = new Double(0);
//		for (SmellSource source : getParent().getObjec
//				.getSmellSources()) {
//			// Check that this is not attached to the same object
//			if (dimension <= source.getStimulus().length) {
//				val += source.getStimulus(dimension, SimbrainMath.distance(this
//						.getLocation(), source.getSuggestedLocation()));
//			}
//			currentValue = val;
//		}
	}

    public List<Class> getApplicableTypes() {
        // TODO Auto-generated method stub
        return null;
    }

    public Double getValue() {
        // TODO Auto-generated method stub
        return null;
    }
}
