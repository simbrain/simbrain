package org.simbrain.custom_sims.helper_classes;

import org.simbrain.util.environment.SmellSource;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.BasicEntity;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * Utility for building odor worlds in simulation files.
 */
public class OdorWorldBuilder {

    /**
     * Parent odor world component.
     */
    private final OdorWorldComponent odorWorldComponent;

    /**
     * Parent odor world.
     */
    private final OdorWorld world;

    /**
     * Construct the builder.
     */
    public OdorWorldBuilder(OdorWorldComponent odorWorldComponent) {
        this.odorWorldComponent = odorWorldComponent;
        world = odorWorldComponent.getWorld();
    }

    /**
     * Add an agent to the odor world.
     *
     * @param x    x location
     * @param y    y location
     * @param type what kind of agent it is. Cow, lion, etc. (cf. options in
     *             RotatingEntity around line 250).
     * @return reference to the agent
     */
    public RotatingEntity addAgent(int x, int y, String type) {
        RotatingEntity agent = new RotatingEntity(world);
        world.addAgent(agent);
        agent.setEntityType(type);
        agent.setLocation(x, y);
        // TODO: Note that setCenterLocation fails here. Problem in OdorWorld
        return agent;
    }

    /**
     * Add a static entity.
     *
     * @param x         x location
     * @param y         y location
     * @param imageName image for this object. See BasicEntity around line 85.
     * @return reference to the entity
     */
    public OdorWorldEntity addEntity(int x, int y, String imageName) {
        // TODO: Reimplement using EntityType
        BasicEntity entity = new BasicEntity(null, world);
        entity.setLocation(x, y);
        entity.setSmellSource(new SmellSource(6));
        world.addEntity(entity);
        return entity;
    }

    /**
     * Add a static entity with a smell source.
     *
     * @param stimulus the smell source
     * @return the entity
     */
    public OdorWorldEntity addEntity(int x, int y, String imageName, double[] stimulus) {
        BasicEntity entity = (BasicEntity) addEntity(x, y, imageName);
        entity.setSmellSource(new SmellSource(stimulus));
        return entity;
    }

    public OdorWorld getWorld() {
        return world;
    }

    public OdorWorldComponent getOdorWorldComponent() {
        return odorWorldComponent;
    }

}
