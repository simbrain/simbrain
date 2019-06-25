package org.simbrain.custom_sims.helper_classes;

import org.simbrain.util.environment.SmellSource;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Utility for building odor worlds in simulation files.
 */
public class OdorWorldWrapper {

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
    public OdorWorldWrapper(OdorWorldComponent odorWorldComponent) {
        this.odorWorldComponent = odorWorldComponent;
        world = odorWorldComponent.getWorld();
    }

    /**
     * Helper to create entities
     */
    private OdorWorldEntity createEntity(int x, int y, EntityType type) {
        OdorWorldEntity entity = new OdorWorldEntity(world, type);
        entity.setLocation(x, y);
        entity.setSmellSource(new SmellSource(6));
        entity.setEntityType(type);
        return entity;
    }

    /**
     * Add an entity at a specified location.
     *
     * @return ref to the new entity
     */
    public OdorWorldEntity addEntity(int x, int y, EntityType type) {
        OdorWorldEntity entity = createEntity(x, y, type);
        world.addEntity(entity);
        return entity;
    }

    /**
     * Add an entity at a location with a smell stimulus.
     *
     * @return ref to the new entity
     */
    public OdorWorldEntity addEntity(int x, int y, EntityType type, double[] stimulus) {
        OdorWorldEntity entity = createEntity(x,y,type);
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
