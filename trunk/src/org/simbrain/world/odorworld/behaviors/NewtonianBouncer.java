package org.simbrain.world.odorworld.behaviors;

import java.util.List;

import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Bounce off the walls...
 */
public class NewtonianBouncer implements Behavior {

    OdorWorldEntity parentEntity;

    public NewtonianBouncer(OdorWorldEntity parentEntity) {
        this.parentEntity = parentEntity;
    }

    public List<Class> applicableEntityTypes() {
        return null;
    }

    public void apply(long elapsedTime) {
        parentEntity.setX(parentEntity.getX() + parentEntity.getVelocityX() * elapsedTime);
        parentEntity.setY(parentEntity.getY() + parentEntity.getVelocityY() * elapsedTime);
    }

    public void collisionX() {
        parentEntity.setVelocityX(-parentEntity.getVelocityX());
    }

    public void collissionY() {
        parentEntity.setVelocityY(-parentEntity.getVelocityY());
    }


}
