package org.simbrain.simulation;

import org.simbrain.util.environment.SmellSource;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.BasicEntity;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

//TODO: Not sure this is the best name.  Use it a while then decide.

/**
 * A wrapper for a OdorWorldComponent that makes it easy to add stuff to an odor
 * world.
 */
public class OdorWorldBuilder {

    private final OdorWorldComponent odorWorldComponent;

    private final OdorWorld world;

    public OdorWorldBuilder(OdorWorldComponent odorWorldComponent) {
        this.odorWorldComponent = odorWorldComponent;
        world = odorWorldComponent.getWorld();
    }

    public RotatingEntity addAgent(int x, int y, String type) {
        // TODO: Put in check for type
        RotatingEntity agent = new RotatingEntity(world);
        world.addAgent(agent);
        agent.setLocation(x, y); // TODO: Note that setCenterLocation fails
                                 // here. Problem in OdorWorld
        return agent;
    }

    public OdorWorldEntity addEntity(int x, int y, String imageName) {
        BasicEntity entity = new BasicEntity(imageName, world);
        entity.setLocation(x, y);
        entity.setSmellSource(new SmellSource(6));
        world.addEntity(entity);
        return entity;
    }

    public OdorWorldEntity addEntity(int x, int y, String imageName, double[] stimulus) {
        BasicEntity entity = new BasicEntity(imageName, world);
        entity.setLocation(x, y);
        entity.setSmellSource(new SmellSource(stimulus));
        world.addEntity(entity);
        return entity;
    }
    
    public OdorWorld getWorld() {
        return world;
    }

    public OdorWorldComponent getOdorWorldComponent() {
        return odorWorldComponent;
    }


}
