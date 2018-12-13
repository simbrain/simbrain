package org.simbrain.custom_sims.helper_classes;

import org.simbrain.util.environment.SmellSource;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

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


    /// NEW STUFF ///

    public OdorWorldEntity addEntity(int x, int y, EntityType type) {
        OdorWorldEntity entity = createEntity(x,y, type);
        world.addEntity(entity);
        return entity;
    }

    public OdorWorldEntity createEntity(int x, int y, EntityType type) {
        OdorWorldEntity entity = new OdorWorldEntity(world, type);
        entity.setLocation(x, y);
        entity.setSmellSource(new SmellSource(6));
        entity.setEntityType(type);
        return entity;
    }

    public OdorWorldEntity createEntity(int x, int y, EntityType type, double[] stimulus) {
        OdorWorldEntity entity = createEntity(x, y, type);
        entity.setSmellSource(new SmellSource(stimulus));
        return entity;
    }

    public OdorWorldEntity addEntity(int x, int y, EntityType type, double[] stimulus) {
        OdorWorldEntity entity = createEntity(x,y,type,stimulus);
        world.addEntity(entity);
        return entity;
    }

    /// OLD STUFF ///

    /**
     * Add an agent to the odor world.
     *
     * @param x    x location
     * @param y    y location
     * @param type what kind of agent it is. Cow, lion, etc. (cf. options in
     *             RotatingEntity around line 250).
     * @return reference to the agent
     */
    public OdorWorldEntity addAgent(int x, int y, String type) {
        OdorWorldEntity entity = new OdorWorldEntity(world, EntityType.MOUSE);
        world.addEntity(entity);
        entity.setLocation(x, y);
        return entity;
    }

    /**
     * Add a static entity.
     *
     * @param x         x location
     * @param y         y location
     * @param imageName image for this object. See OdorWorldEntity around line 85.
     * @return reference to the entity
     */
    public OdorWorldEntity addEntity(int x, int y, String imageName) {
        // TODO: Reimplement using EntityType
        OdorWorldEntity entity = new OdorWorldEntity(world);
        entity.setLocation(x, y);
        entity.setSmellSource(new SmellSource(6));
        world.addEntity(entity);
        return entity;
    }

    //TODO: PHase out since EntityType not used
    /**
     * Add a static entity with a smell source.
     *
     * @param stimulus the smell source
     * @return the entity
     */
    public OdorWorldEntity addEntity(int x, int y, String imageName, double[] stimulus) {
        OdorWorldEntity entity = (OdorWorldEntity) addEntity(x, y, imageName);
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
