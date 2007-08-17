package org.simbrain.world.threedee;

import java.util.ArrayList;
import java.util.List;

import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;

public class Environment implements Element {
    private final List<Element> elements = new ArrayList<Element>();
    private TerrainManager terrainManager = new TerrainManager();
    
    public void add(Agent agent) {
        elements.add(agent);
        agent.setEnvironment(this);
    }
    
    public float getHeight(Vector3f location) {
        return terrainManager.getHeight(location);
    }

    public void init(Renderer renderer, Node parent) {
        parent.attachChild(terrainManager.getTerrainBlock(renderer));
        
        for (Element element : elements) {
            element.init(renderer, parent);
        }
    }
    
    public void update() {
        for (Element element : elements) {
            element.update();
        }
    }
}
