package org.simbrain.world.threedee;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;

/**
 * Represents the viewable world and is the container for adding elements and
 * views to that world.
 * 
 * @author Matt Watson
 */
public class Environment {
    public static final int REFRESH_WAIT = 10;

    private static final Logger LOGGER = Logger.getLogger(Environment.class);

    /** Timer that fires the update operation. */
    private Timer timer;

    /** The elements in this environment. */
    private final List<Element> elements = new ArrayList<Element>();

    /** All the views on this environment. */
    private final List<Viewable> views = new ArrayList<Viewable>();

    /** The terrain for the environment. */
    private final Terrain terrain = new Terrain();

    /** The sky for the environment. */
    private Sky sky = new Sky();
    
    /**
     * Creates a new environment.
     */
    public Environment() {
        elements.add(terrain);
        elements.add(sky);
    }

    /**
     * Adds an agent to this environment.
     * 
     * @param agent the agent to add
     */
    public void add(final Agent agent) {
        elements.add(new AgentElement(agent));
        views.add(agent);
        agent.setEnvironment(this);
    }

    /**
     * Adds a new view.
     * 
     * @param view the view to add
     */
    public void addViewable(final Viewable view) {
        views.add(view);
    }

    /**
     * Returns the floor height at the x and z coordinates of the given point.
     * 
     * @param location
     * @return
     */
    public float getFloorHeight(final Vector3f location) {
        return terrain.getHeight(location);
    }

    /**
     * Initializes the environment with the given renderer and parent.
     * 
     * @param renderer the renderer
     * @param parent the parent node
     */
    public void init(final Renderer renderer, final Node parent) {
        LOGGER.debug("init");

        for (final Element element : elements) {
            LOGGER.debug("element: " + element);
            element.init(renderer, parent);
        }

        parent.setModelBound(new BoundingBox());
        parent.updateModelBound();

        timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                update();
            }
        }, REFRESH_WAIT, REFRESH_WAIT);
    }

    /**
     * Calls updates on all the elements, looks for collisions, fires any
     * collision events, commits the elements, and updates the views.
     */
    public void update() {
        LOGGER.trace("update");

        for (final Element element : elements) {
            element.update();
        }

        for (int i = 0; i < elements.size(); i++) {
            final Element a = elements.get(i);
            final SpatialData aData = a.getTentative();

            if (aData == null)
                continue;

            final Vector3f aCenter = aData.centerPoint();
            final float aRadius = aData.radius();

            for (int j = i + 1; j < elements.size(); j++) {
                final Element b = elements.get(j);
                final SpatialData bData = b.getTentative();

                if (bData == null)
                    continue;

                final Vector3f bCenter = bData.centerPoint();
                final float bRadius = bData.radius();

                final float distance = aCenter.distance(bCenter);

                if (distance <= (aRadius + bRadius)) {
                    final CollisionData data = new CollisionData(a, aCenter, aRadius, b, bCenter,
                            bRadius);

                    a.collision(data.collisionA);
                    b.collision(data.collisionB);
                }
            }
        }

        for (final Element element : elements) {
            element.commit();
        }

        for (final Viewable view : views) {
            view.updateView();
        }
    }

    /**
     * Helper class for managing collision data temporarily creates the
     * collision data objects for both elements involved in the collision.
     * 
     * @author Matt Watson
     */
    private static class CollisionData {
        final Collision collisionA;

        final Collision collisionB;

        CollisionData(final Element a, final Vector3f aCenter, final float aRadius,
                final Element b, final Vector3f bCenter, final float bRadius) {
            class CollisionLocal implements Collision {
                final Element other;

                final Vector3f point;

                CollisionLocal(final Element other, Vector3f center, final float percent) {
                    this.other = other;

                    center = (Vector3f) center.clone();
                    center.interpolate(other.getTentative().centerPoint(), percent);

                    this.point = center;
                }

                public Element other() {
                    return other;
                }

                public Vector3f point() {
                    return point;
                }

            }

            final float total = aRadius + bRadius;

            collisionA = new CollisionLocal(b, aCenter, total / aRadius);
            collisionB = new CollisionLocal(a, bCenter, total / bRadius);
        }
    }
}
