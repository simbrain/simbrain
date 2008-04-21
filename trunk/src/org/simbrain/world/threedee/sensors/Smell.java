package org.simbrain.world.threedee.sensors;

import org.simbrain.world.threedee.Agent;
import org.simbrain.world.threedee.Entity;
import org.simbrain.world.threedee.Point;
import org.simbrain.world.threedee.Sensor;
import org.simbrain.world.threedee.Vector;
import org.simbrain.world.threedee.Entity.Odor;

/**
 * Instances of Smell are sensors for a specific odor.
 * 
 * @author Matt Watson
 */
public class Smell implements Sensor {
    /** An arbitrary prime number. */
    private static final int ARBITRARY_PRIME = 97;
    
    /** The scent this sensor responds to. */
    private final String odorName;
    /** The parent agent for this sensor. */
    private final Agent agent;
    /** */
    private final float offset;
    
    /**
     * Creates a new odor sensor for the given type of scent.
     * 
     * @param odor The scent this sensor responds to.
     * @param agent The agent this sensor applies to.
     */
    public Smell(final String odor, final Agent agent, float offset) {
        if (agent == null || odor == null) {
            throw new IllegalArgumentException("neither agent nor smell can be null");
        }
        
        this.odorName = odor;
        this.agent = agent;
        this.offset = offset;
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue() {
        double total = 0;
        
        Vector direction = agent.getDirection();
        Point location = agent.getLocation();
        
        Vector perpendicular = new Vector(-direction.getZ(), direction.getY(), direction.getX());
        
        Point p = location.add(direction).add(perpendicular.multiply(offset));
        
//        System.out.println("location: " + location);
//        System.out.println("direction: " + direction);
//        System.out.println("perpendicular: " + perpendicular);
//        System.out.println("nostril: " + p);
        
        for (Odor odor : agent.getEnvironment().getOdors().getOdors(odorName)) {
            Entity odorParent = odor.getParent();
            
            if (odorParent == agent) continue;
            
            double distance = p.distance(odorParent.getLocation());
            
            if (distance == 0) continue;
            
            total += odor.getStrength() / distance;
        }
        
        return total;
    }
    
    /**
     * Returns The scent this sensor responds to.
     * 
     * @return The scent this sensor responds to.
     */
    public String getOdor() {
        return odorName;
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return odorName + " smell";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return agent.hashCode() + (ARBITRARY_PRIME * odorName.toLowerCase().hashCode());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Smell)) return false;
        
        Smell other = (Smell) obj;
        
        return agent.equals(other.agent) && odorName.equalsIgnoreCase(other.odorName) && offset == other.offset;
    }
}
