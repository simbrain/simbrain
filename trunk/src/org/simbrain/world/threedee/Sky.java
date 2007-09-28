package org.simbrain.world.threedee;

import org.simbrain.resource.ResourceManager;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Skybox;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;
import com.jmex.terrain.TerrainBlock;
import com.jmex.terrain.util.MidPointHeightMap;
import com.jmex.terrain.util.ProceduralTextureGenerator;

/**
 * A multiple view element that represents the sky
 * in an environment.
 */
public class Sky extends MultipleViewElement<Skybox> {

    /**
     * Creates a skybox.
     */
    @Override
    public Skybox create() {
        Skybox skybox = new Skybox("sky", 200, 200, 200);
        return skybox;
    }

    /**
     * Initializes a Skybox.
     * 
     * @param renderer the renderer
     * @param skybox the skybox
     */
    @Override
    public void initSpatial(final Renderer renderer, final Skybox skybox) {
        Texture skyTexture = TextureManager.loadTexture(ResourceManager.getImage("sky.png"),
                Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR, true);
        skybox.setTexture(Skybox.UP, skyTexture);
        skybox.setTexture(Skybox.DOWN, skyTexture);
        skybox.setTexture(Skybox.NORTH, skyTexture);
        skybox.setTexture(Skybox.SOUTH, skyTexture);
        skybox.setTexture(Skybox.EAST, skyTexture);
        skybox.setTexture(Skybox.WEST, skyTexture);
    }

    @Override
    public void updateSpatial(Skybox box) {
        /* no implementation */
    }

    /**
     * {@inheritDoc}
     */
    public void collision(final Skybox box) {
        /* no implementation */
    }

    /**
     * {@inheritDoc}
     */
    public void commit() {
        /* no implementation */
    }

    /**
     * {@inheritDoc}
     */
    public SpatialData getTentative() {
        /* no implementation */
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void collision(Collision collision) {
        // TODO Auto-generated method stub
    }

}