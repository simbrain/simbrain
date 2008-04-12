package org.simbrain.world.threedee.environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.simbrain.world.threedee.Entity;
import org.simbrain.world.threedee.Entity.Odor;

/**
 * The odors that exist in the environment.
 * 
 * @author Matt Watson
 */
public class Odors {
    /** The odors indexed by type. */
    Map<String, List<Odor>> odorMap
        = new TreeMap<String, List<Odor>>(String.CASE_INSENSITIVE_ORDER);
    
    /**
     * Adds an entity.
     * 
     * @param entity The entity to add.
     */
    void addOdors(final Entity entity) {
        for (Odor odor : entity.getOdors()) {
            List<Odor> entities = odorMap.get(odor.getName());
            
            if (entities == null) {
                entities = new ArrayList<Odor>();
                odorMap.put(odor.getName(), entities);
            }
            
            entities.add(odor);
        }
    }
    
    public Set<String> getOdorTypes() {
        return odorMap.keySet();
    }
    
    /**
     * Returns the entities with the given odor.
     * 
     * @param odor The name of the odor to search for (case insensitive)
     * @return The entities with the given odor.
     */
    public List<Odor> getOdors(final String odor) {
        return odorMap.get(odor);
    }
}
