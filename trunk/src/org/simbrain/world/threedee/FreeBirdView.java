package org.simbrain.world.threedee;

import com.jme.math.Vector3f;

public class FreeBirdView extends Moveable {

    private Vector3f direction;
    private Vector3f location;
    
    @Override
    protected Vector3f getDirection() {
        return direction;
    }

    @Override
    protected Vector3f getLocation() {
        return location;
    }

    @Override
    public void init(Vector3f direction, Vector3f location) {
        this.direction = direction;
        this.location = location;
    }

    @Override
    protected void updateDirection(Vector3f direction) {
        this.direction = direction;
    }

    @Override
    protected void updateLocation(Vector3f location) {
        this.location = location;
    }
    
}
