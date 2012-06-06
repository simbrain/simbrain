package org.simbrain.world.threedee.entities;

import java.util.Collections;
import java.util.List;

import org.simbrain.world.threedee.Collision;
import org.simbrain.world.threedee.Entity;
import org.simbrain.world.threedee.MultipleViewElement;
import org.simbrain.world.threedee.Point;
import org.simbrain.world.threedee.SpatialData;

import com.jme.bounding.BoundingSphere;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.shape.Sphere;

public class Plant extends MultipleViewElement<Node> implements Entity {
    private static final float COLLISION_RADIUS = 0.5f;

    private Point location;
    private Point tentativeLocation;
    private final Odor odor = new Odor("green", this);

    public Plant(Point location) {
        this.location = location;
        this.tentativeLocation = location;
    }

    public Plant() {
        this(new Point(0, 0, 0));
    }

    @Override
    public Node create() {
        final Sphere s = new Sphere("sphere", new Vector3f(), 12, 12, 0.5f);
        s.setModelBound(new BoundingSphere());
        s.updateModelBound();
        s.setDefaultColor(ColorRGBA.green);
        final Node node = new Node("Plant Node");
        node.attachChild(s);
        node.setModelBound(new BoundingSphere());
        node.updateModelBound();
        return node;
    }

    @Override
    public void initSpatial(Renderer renderer, Node node) {
        node.setLocalTranslation(location.toVector3f());
    }

    @Override
    public void updateSpatial(Node node) {
        node.setLocalTranslation(location.toVector3f());
    }

    public Point getLocation() {
        return location;
    }

    public List<Odor> getOdors() {
        return Collections.singletonList(odor);
    }

    public void collision(Collision collision) {
        /* ouch! */
    }

    public void commit() {
        location = tentativeLocation;
    }

    public SpatialData getTentative() {
        return new SpatialData(tentativeLocation, COLLISION_RADIUS);
    }

    public void setFloor(float height) {
        this.tentativeLocation = tentativeLocation.setY(height);
    }

    public void setTentativeLocation(Point point) {
        this.tentativeLocation = point;
    }
}
