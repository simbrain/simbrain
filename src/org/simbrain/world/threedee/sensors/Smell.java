package org.simbrain.world.threedee.sensors;

import org.simbrain.world.threedee.Agent;
import org.simbrain.world.threedee.Entity;
import org.simbrain.world.threedee.Sensor;
import org.simbrain.world.threedee.Entity.Odor;

/**
 * Instances of Smell are sensors for a specific odor.
 * 
 * @author Matt Watson
 */
public class Smell implements Sensor {
    /** The scent this sensor responds to. */
    private final String odorName;
    /** The parent agent for this sensor. */
    private final Agent agent;
    
    /**
     * Creates a new odor sensor for the given type of scent.
     * 
     * @param odor The scent this sensor responds to.
     * @param agent The agent this sensor applies to.
     */
    public Smell(final String odor, final Agent agent) {
        if (agent == null || odor == null) {
            throw new IllegalArgumentException("neither agent nor smell can be null");
        }
        
        this.odorName = odor;
        this.agent = agent;
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue() {
        double total = 0;
        
        for (Odor odor : agent.getEnvironment().getOdors().getOdors(odorName)) {
            Entity odorParent = odor.getParent();
            
            if (odorParent == agent) continue;
            
            double distance = agent.getLocation().distance(odorParent.getLocation());
            
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

    public String getDescription() {
        return odorName + " smell";
    }

    @Override
    public int hashCode() {
        return agent.hashCode() + 97 * odorName.toLowerCase().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Smell)) return false;
        
        Smell other = (Smell) obj;
        
        return agent.equals(other.agent) && odorName.equalsIgnoreCase(other.odorName);
    }
}
