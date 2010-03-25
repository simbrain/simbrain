package org.simbrain.world.threedee;

import com.jme.math.Vector3f;

public class Vector {
    private final Vector3f internal;
    
    private Vector(Vector3f vector3f, boolean clone) {
        if (vector3f == null) throw new IllegalArgumentException("Vector cannot be null.");
        
        if (clone) {
            this.internal = (Vector3f) vector3f.clone();
        } else {
            this.internal = vector3f;
        }
        
        this.internal.normalizeLocal();
    }
    
    public Vector(Vector3f vector3f) {
        this(vector3f, true);
    }
    
    public Vector(float x, float y, float z) {
        this.internal = new Vector3f(x, y, z);
    }
    
    public Vector normalize() {
        return new Vector(this.internal.normalize(), false);
    }
    
    public Vector multiply(float scale) {
        return new Vector(internal.mult(scale), false);
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
    
    public Vector setX(float x) {
        Vector3f vec = (Vector3f) internal.clone();
        vec.x = x;
        return new Vector(vec, false);
    }
    
    public Vector setY(float y) {
        Vector3f vec = (Vector3f) internal.clone();
        vec.y = y;
        return new Vector(vec, false);
    }
    
    public Vector setZ(float z) {
        Vector3f vec = (Vector3f) internal.clone();
        vec.z = z;
        return new Vector(vec, false);
    }
    
    public Vector3f toVector3f() {
        return new Vector3f(getX(), getY(), getZ());
    }
    
    public String toString() {
        return "vector: (" + internal.getX() + ", " + internal.getY() + ", " + internal.getZ() + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 37;
        int result = 1;
        result = prime * result + ((internal == null) ? 0 : internal.hashCode());
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        
        if (!(obj instanceof Vector)) return false;
        
        final Vector other = (Vector) obj;
        
        return internal.equals(other.internal);
    }
}
