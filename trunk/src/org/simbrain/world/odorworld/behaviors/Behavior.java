package org.simbrain.world.odorworld.behaviors;

import java.util.List;

/**
 * Represents a type of behavior for an OdorWorldEntity. These behaviors can be
 * supplemented or overridden by scripts or other external components, in
 * particular neural networks. For example, a simple bouncing motion can be
 * supplemented by pushes from a simple neural network.
 */
public interface Behavior {
        
    public void apply(long elapsedTime);
    
    public void collisionX();
    
    public void collissionY();
    
    public List<Class> applicableEntityTypes();

}
