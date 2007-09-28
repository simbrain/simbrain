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
 * Represents the viewable world and is the container 
 * for adding elements and views to that world
 * 
 * @author Matt Watson
 */
public class Environment {
    public static final int REFRESH_WAIT = 10;
    
    private static final Logger LOGGER = Logger.getLogger(Environment.class);
    
    /** timer that fires the update operation */
    private Timer timer;
    /** the elements in this environment */
    private final List<Element> elements = new ArrayList<Element>();
    /** all the views on this environment */
    private final List<Viewable> views = new ArrayList<Viewable>();
    /** the terrain for the environment */
    private Terrain terrain = new Terrain();
    
    /**
     * creates a new environment
     */
    public Environment() {
        elements.add(terrain);
    }
    
    /**
     * Adds an agent to this environment
     * 
     * @param agent the agent to add
     */
    public void add(Agent agent) {
        elements.add(new AgentElement(agent));
        views.add(agent);
        agent.setEnvironment(this);
    }
    
    /**
     * adds a new view
     * 
     * @param view the view to add
     */
    public void addViewable(Viewable view) {
        views.add(view);
    }
    
    /**
     * returns the floor height at the x and z coordinates
     * of the given point
     * 
     * @param location
     * @return
     */
    public float getFloorHeight(Vector3f location) {
        return terrain.getHeight(location);
    }

    /**
     * initializes the environment with the given
     * renderer and parent
     * 
     * @param renderer the renderer
     * @param parent the parent node
     */
    public void init(Renderer renderer, Node parent) {
        LOGGER.debug("init");
        
        for (Element element : elements) {
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
     * calls updates on all the elements, looks for collisions, fires any
     * collision events, commits the elements, and updates the views
     */
    public void update() {
        LOGGER.trace("update");
        
        for (Element element : elements) {
            element.update();
        }
        
        for (int i = 0; i < elements.size(); i++) {
            Element a = elements.get(i);
            SpatialData aData = a.getTentative();
            
            if (aData == null) continue;
            
            Vector3f aCenter = aData.centerPoint();
            float aRadius = aData.radius();
            
            for (int j = i + 1; j < elements.size(); j++) {
                Element b = elements.get(j);
                SpatialData bData = b.getTentative();
                
                if (bData == null) continue;
                
                Vector3f bCenter = bData.centerPoint();
                float bRadius = bData.radius();
                
                float distance = aCenter.distance(bCenter);
                
                if (distance <= (aRadius + bRadius)) {
                    CollisionData data = new CollisionData(a, aCenter, aRadius, b, bCenter, bRadius);
                    
                    a.collision(data.collisionA);
                    b.collision(data.collisionB);
                }
            }
        }
        
        for (Element element : elements) {
            element.commit();
        }
        
        for (Viewable view : views) {
            view.updateView();
        }
    }
    
    /**
     * Helper class for managing collision data temporarily
     * creates the collision data objects for both elements involved
     * in the collision
     * 
     * @author Matt Watson
     */
    private static class CollisionData {
        final Collision collisionA;
        final Collision collisionB;
        
        CollisionData(Element a, Vector3f aCenter, float aRadius, 
                Element b, Vector3f bCenter, float bRadius) {
            class CollisionLocal implements Collision {
                final Element other;
                final Vector3f point;
                
                CollisionLocal(Element other, Vector3f center, float percent) {
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
            
            float total = aRadius + bRadius;
            
            collisionA = new CollisionLocal(b, aCenter, total / aRadius);
            collisionB = new CollisionLocal(a, bCenter, total / bRadius);
        }  
    }
}
