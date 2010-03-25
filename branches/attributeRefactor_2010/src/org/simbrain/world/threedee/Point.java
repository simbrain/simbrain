package org.simbrain.world.threedee;

import com.jme.math.Vector3f;

public class Point {
    private final Vector3f internal;
    
    private Point(Vector3f vector3f, boolean clone) {
        if (vector3f == null) throw new IllegalArgumentException("Vector cannot be null.");
        
        if (clone) {
            this.internal = (Vector3f) vector3f.clone();
        } else {
            this.internal = vector3f;
        }
    }
    
    public Point(Vector3f vector3f) {
        this(vector3f, true);
    }
    
    public Point(float x, float y, float z) {
        this.internal = new Vector3f(x, y, z);
    }
    
    public float getX() {
        return internal.x;
    }
    
    public float getY() {
        return internal.y;
    }
    
    public float getZ() {
        return internal.z;
    }
    
    public Point add(Vector vector) {
        return new Point(internal.add(vector.toVector3f()), false);
    }
    
    public Point setX(float x) {
        Vector3f vec = (Vector3f) internal.clone();
        vec.x = x;
        return new Point(vec, false);
    }
    
    public Point setY(float y) {
        Vector3f vec = (Vector3f) internal.clone();
        vec.y = y;
        return new Point(vec, false);
    }
    
    public Point setZ(float z) {
        Vector3f vec = (Vector3f) internal.clone();
        vec.z = z;
        return new Point(vec, false);
    }

    public float distance(Point other) {
        return internal.distance(other.internal);
    }
    
    public Vector3f toVector3f() {
        return new Vector3f(getX(), getY(), getZ());
    }
    
    public String toString() {
        return "point: (" + internal.getX() + ", " + internal.getY() + ", " + internal.getZ() + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((internal == null) ? 0 : internal.hashCode());
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        
        if (!(obj instanceof Point)) return false;
        
        final Point other = (Point) obj;
        
        return internal.equals(other.internal);
    }
}
