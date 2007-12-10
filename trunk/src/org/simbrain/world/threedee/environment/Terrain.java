package org.simbrain.world.threedee.environment;

import org.simbrain.resource.ResourceManager;
import org.simbrain.world.threedee.Collision;
import org.simbrain.world.threedee.MultipleViewElement;
import org.simbrain.world.threedee.SpatialData;

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
    /** temporary hard-coding. */
    private static final int GRASS_LOW = -128;
    /** temporary hard-coding. */
    private static final int GRASS_OPTIMAL = 0;
    /** temporary hard-coding. */
    private static final int GRASS_HIGH = 128;
    /** temporary hard-coding. */
    private static final int DIRT_LOW = 0;
    /** temporary hard-coding. */
    private static final int DIRT_OPTIMAL = 128;
    /** temporary hard-coding. */
    private static final int DIRT_HIGH = 255;
    /** temporary hard-coding. */
    private static final int SNOW_LOW = 128;
    /** temporary hard-coding. */
    private static final int SNOW_OPTIMAL = 255;
    /** temporary hard-coding. */
    private static final int SNOW_HIGH = 384;
    
    /** The size of the blocks in the TerrainPage. */
    private static final int BLOCK_SIZE = 64;
    
    /** A height map creates a 'natural' bumpy terrain. */
    private final MidPointHeightMap heightMap;

    /** The underlying jME Object that represents the terrain. */
    private final TerrainPage heightBlock;
    
    /** The size of the terrain. */
    private final int size;

    /**
     * Creates a terrain based on the given size. It will actually create 4
     * blocks of the given size in both directions so the total area will be the
     * square of 2 times size.
     * 
     * @param size The size of the terrain.
     */
    Terrain(final int size) {
        this.size = size;
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
     * {@inheritDoc}
     */
    @Override
    public TerrainPage create() {
        final Vector3f terrainScale = new Vector3f(4, 0.0575f, 4);

        return new TerrainPage("Terrain", BLOCK_SIZE, heightMap.getSize() + 1, terrainScale,
            heightMap.getHeightMap(), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initSpatial(final Renderer renderer, final TerrainPage block) {
        /* generate a terrain texture with 2 textures */
        final ProceduralTextureGenerator pt = new ProceduralTextureGenerator(heightMap);

        pt.addTexture(ResourceManager.getImageIcon("grassb.png"),
            GRASS_LOW, GRASS_OPTIMAL, GRASS_HIGH);
        pt.addTexture(ResourceManager.getImageIcon("dirt.jpg"),
            DIRT_LOW, DIRT_OPTIMAL, DIRT_HIGH);
        pt.addTexture(ResourceManager.getImageIcon("highest.jpg"),
            SNOW_LOW, SNOW_OPTIMAL, SNOW_HIGH);
        pt.createTexture(size);

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
     * 
     * @param block Not used.
     */
    @Override
    public void updateSpatial(final TerrainPage block) {
        /* no implementation */
    }

    /**
     * No implementation.
     * 
     * @param collision Not used.
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
     * 
     * @return null
     */
    public SpatialData getTentative() {
        /* no implementation */
        return null;
    }
}
