package org.simbrain.world.threedee;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.jme.bounding.BoundingBox;
import com.jme.intersection.Intersection;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;

public class Environment {
    private static final Logger LOGGER = Logger.getLogger(Environment.class);
    
    private Timer timer;
    private final List<Element> elements = new ArrayList<Element>();
    private final List<Viewable> views = new ArrayList<Viewable>();
    
    private Terrain terrain = new Terrain();
    
    public Environment() {
        elements.add(terrain);
    }
    
    public void add(Agent agent) {
        elements.add(new AgentElement(agent));
        views.add(agent);
        agent.setEnvironment(this);
    }
    
    public void addViewable(Viewable view) {
        views.add(view);
    }
    
    public float getFloorHeight(Vector3f location) {
        return terrain.getHeight(location);
    }

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
        }, 10, 10);
    }
    
    Intersection intersection = new Intersection();
    
    public void update() {
        LOGGER.trace("update");
        
        for (Element element : elements) {
            element.update();
        }
        
        for (int i = 0; i < elements.size(); i++) {
            Element a = elements.get(i);
            SpatialData aData = a.getTenative();
            
            if (aData == null) continue;
            
            Vector3f aCenter = aData.centerPoint();
            float aRadius = aData.radius();
            
            for (int j = i + 1; j < elements.size(); j++) {
                Element b = elements.get(j);
                SpatialData bData = b.getTenative();
                
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
                    center.interpolate(other.getTenative().centerPoint(), percent);
                    
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
