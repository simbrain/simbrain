package org.simbrain.world.odorworld.behaviors;

import java.util.List;

/**
 * Null behavior; do nothing. However, as with all behaviors, this can be
 * overridden by effectors, which can be driven by, for example, a neural
 * network.
 */
public class StationaryBehavior implements Behavior {

    public List<Class> applicableEntityTypes() {
        return null;
    }

    public void apply(long elapsedTime) {
    }

    public void collisionX() {
    }

    public void collissionY() {
    }


}
