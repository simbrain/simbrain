package org.simbrain.world.threedee;

/**
 * A simple abstraction for holding the temporary spatial data used after
 * updating an element. This can be expanded upon as needed.
 *
 * @author Matt Watson
 */
public class SpatialData {
    /** The center of the Spatial. */
    private final Point center;

    /** The Radius of the rough bounding sphere for the Spatial. */
    private final float radius;

    /**
     * Creates a new SpatialData for the given center and radius.
     *
     * @param center The center of the Spatial.
     * @param radius The radius of the Spatial's bounding sphere.
     */
    public SpatialData(final Point center, final float radius) {
        this.center = center;
        this.radius = radius;
    }

    /**
     * Gets the center point of the element.
     *
     * @return the center point
     */
    public Point centerPoint() {
        return center;
    }

    /**
     * Gets the radius from the center point.
     *
     * @return the radius
     */
    public float radius() {
        return radius;
    }

    /**
     * Returns whether this object intersects with the given object.
     *
     * @param other The other spatialData.
     * @return Whether this object intersects with the given object.
     */
    public boolean intersects(final SpatialData other) {
        if (other == null) {
            return false;
        }

        final Point aCenter = this.centerPoint();
        final float aRadius = this.radius();
        final Point bCenter = other.centerPoint();
        final float bRadius = other.radius();

        final float distance = aCenter.distance(bCenter);

        // System.out.println("distance: " + distance);
        // System.out.println("aRadius: " + aRadius);
        // System.out.println("bRadius: " + bRadius);

        return distance <= (aRadius + bRadius);
    }
}
