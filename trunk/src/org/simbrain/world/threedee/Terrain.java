package org.simbrain.world.threedee;

import org.simbrain.resource.ResourceManager;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;
import com.jmex.terrain.TerrainBlock;
import com.jmex.terrain.util.MidPointHeightMap;
import com.jmex.terrain.util.ProceduralTextureGenerator;

/**
 * a multiple view element that represents the ground
 * in an environment
 * 
 * @author Matt Watson
 */
public class Terrain extends MultipleViewElement<TerrainBlock> {
    /** a height map creates a 'natural' bumpy terrain */
    private MidPointHeightMap heightMap = new MidPointHeightMap(64, 1f);
    /** the underlying jME Object that represents the terrain */
    private final TerrainBlock heightBlock = create();
    
    /** 
     * returns the height at the x and z parts of the given point
     * 
     * @param location the location to check
     * @return the height at the given point
     */
    public float getHeight(Vector3f location) {
        return heightBlock.getHeight(location);
    }
    
    /**
     * creates a new TerrainBlock based on the underlying
     * height map
     */
    @Override
    public TerrainBlock create() {
        Vector3f terrainScale = new Vector3f(4, 0.0575f, 4);
        return new TerrainBlock("Terrain", heightMap.getSize(), terrainScale,
            heightMap.getHeightMap(), new Vector3f(0, 0, 0), false);
    }
    
    /**
     * initializes a TerrainBlock
     */
    @Override
    public void initSpatial(Renderer renderer, TerrainBlock block) {
        /* generate a terrain texture with 2 textures */
        ProceduralTextureGenerator pt = new ProceduralTextureGenerator(heightMap);
        
        pt.addTexture(ResourceManager.getImageIcon("grassb.png"), -128, 0, 128);
        pt.addTexture(ResourceManager.getImageIcon("dirt.jpg"), 0, 128, 255);
        pt.addTexture(ResourceManager.getImageIcon("highest.jpg"), 128, 255, 384);
        pt.createTexture(32);
       
        /* assign the texture to the terrain */
        TextureState ts = renderer.createTextureState();
        Texture t1 = TextureManager.loadTexture(pt.getImageIcon().getImage(),
            Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR, true);
        
        ts.setTexture(t1, 0);
    
        block.setModelBound(new BoundingBox());
        block.updateModelBound();
        
        block.setRenderState(ts);
        block.setRenderQueueMode(Renderer.QUEUE_OPAQUE);
    }

    /**
     * no implementation
     */
    @Override
    public void updateSpatial(TerrainBlock block) {
        /* no implementation */
    }

    /**
     * no implementation
     */
    public void collision(Collision collision) {
        /* no implementation */
    }

    /**
     * no implementation
     */
    public void commit() {
        /* no implementation */
    }

    /**
     * no implementation
     */
    public SpatialData getTentative() {
        /* no implementation */
        return null;
    }
}