package org.simbrain.world.threedee;

import org.simbrain.resource.ResourceManager;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;
import com.jmex.terrain.TerrainPage;
import com.jmex.terrain.util.MidPointHeightMap;
import com.jmex.terrain.util.ProceduralTextureGenerator;

/**
 * A multiple view element that represents the ground in an environment.
 * 
 * @author Matt Watson
 */
public class Terrain extends MultipleViewElement<TerrainPage> {
    /** A height map creates a 'natural' bumpy terrain. */
    private final MidPointHeightMap heightMap;

    /** The underlying jME Object that represents the terrain. */
    private final TerrainPage heightBlock;

    /**
     * Creates a terrain based on the given size. It will actually create 4
     * blocks of the given size in both directions so the total area will be the
     * square of 2 times size.
     */
    Terrain(final int size) {
        heightMap = new MidPointHeightMap(size, 1f);
        heightBlock = create();
    }

    /**
     * Returns the height at the x and z parts of the given point.
     * 
     * @param location the location to check
     * @return the height at the given point
     */
    public float getHeight(final Vector3f location) {
        return heightBlock.getHeight(location);
    }

    /**
     * Creates a new TerrainBlock based on the underlying height map.
     */
    @Override
    public TerrainPage create() {
        final Vector3f terrainScale = new Vector3f(4, 0.0575f, 4);

        return new TerrainPage("Terrain", 64, heightMap.getSize() + 1, terrainScale, heightMap
                .getHeightMap(), false);
    }

    /**
     * Initializes a TerrainBlock.
     */
    @Override
    public void initSpatial(final Renderer renderer, final TerrainPage block) {
        /* generate a terrain texture with 2 textures */
        final ProceduralTextureGenerator pt = new ProceduralTextureGenerator(heightMap);

        pt.addTexture(ResourceManager.getImageIcon("grassb.png"), -128, 0, 128);
        pt.addTexture(ResourceManager.getImageIcon("dirt.jpg"), 0, 128, 255);
        pt.addTexture(ResourceManager.getImageIcon("highest.jpg"), 128, 255, 384);
        pt.createTexture(256);

        /* assign the texture to the terrain */
        final TextureState ts = renderer.createTextureState();
        final Texture t1 = TextureManager.loadTexture(pt.getImageIcon().getImage(),
                Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR, true);

        ts.setTexture(t1, 0);

        block.setModelBound(new BoundingBox());
        block.updateModelBound();

        block.setRenderState(ts);
        block.setRenderQueueMode(Renderer.QUEUE_OPAQUE);
    }

    /**
     * No implementation.
     */
    @Override
    public void updateSpatial(final TerrainPage block) {
        /* no implementation */
    }

    /**
     * No implementation.
     */
    public void collision(final Collision collision) {
        /* no implementation */
    }

    /**
     * No implementation.
     */
    public void commit() {
        /* no implementation */
    }

    /**
     * No implementation.
     */
    public SpatialData getTentative() {
        /* no implementation */
        return null;
    }
}
