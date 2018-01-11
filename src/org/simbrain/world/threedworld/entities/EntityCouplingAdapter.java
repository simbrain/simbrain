package org.simbrain.world.threedworld.entities;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import com.jme3.math.Vector3f;

public class EntityCouplingAdapter {

    private Entity entity;
    
    public EntityCouplingAdapter(Entity entity) {
        this.entity = entity;
    }

    @Producible(idMethod="getName")
    public double getX() {
        return entity.getPosition().x;
    }

    @Consumable(idMethod="getName")
    public void setX(double value) {
        Vector3f location = entity.getPosition();
        location.x = (float)value;
        entity.queuePosition(location);
    }

    @Producible(idMethod="getName")
    public double getY() {
        return entity.getPosition().y;
    }

    @Consumable(idMethod="getName")
    public void setY(double value) {
        Vector3f location = entity.getPosition();
        location.y = (float)value;
        entity.queuePosition(location);
    }

    @Producible(idMethod="getName")
    public double getZ() {
        return entity.getPosition().z;
    }

    @Consumable(idMethod="getName")
    public void setZ(double value) {
        Vector3f location = entity.getPosition();
        location.z = (float)value;
        entity.queuePosition(location);
    }

    @Producible(idMethod="getName")
    public double[] getPosition() {
        Vector3f position = entity.getPosition();
        return new double[] {position.x, position.y, position.z};
    }

    @Consumable(idMethod="getName")
    public void setPosition(double[] value) {
        Vector3f position = new Vector3f((float) value[0], (float) value[1], (float) value[2]);
        entity.queuePosition(position);
    }

    @Producible(idMethod="getName")
    public double getYaw() {
        return FastMath.RAD_TO_DEG * getRotation()[1];
    }

    @Consumable(idMethod="getName")
    public void setYaw(double value) {
        double[] rotation = getRotation();
        rotation[1] = FastMath.DEG_TO_RAD * (float)value;
        setRotation(rotation);
    }

    @Producible(idMethod="getName")
    public double getPitch() {
        return FastMath.RAD_TO_DEG * getRotation()[0];
    }

    @Consumable(idMethod="getName")
    public void setPitch(double value) {
        double[] rotation = getRotation();
        rotation[0] = FastMath.DEG_TO_RAD * (float)value;
        setRotation(rotation);
    }

    @Producible(idMethod="getName")
    public double getRoll() {
        return FastMath.RAD_TO_DEG * getRotation()[2];
    }

    @Consumable(idMethod="getName")
    public void setRoll(double value) {
        double[] rotation = getRotation();
        rotation[2] = FastMath.DEG_TO_RAD * (float)value;
        setRotation(rotation);
    }

    @Producible(idMethod="getName")
    private double[] getRotation() {
        float[] angles = entity.getRotation().toAngles(null);
        return new double[] {angles[0], angles[1], angles[2]};
    }

    @Consumable(idMethod="getName")
    private void setRotation(double[] values) {
        Quaternion rotation = entity.getRotation();
        float[] angles = new float[] {(float) values[0], (float) values[1], (float) values[2]};
        rotation.fromAngles(angles);
        entity.queueRotation(rotation);
    }

}