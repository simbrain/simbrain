package org.simbrain.world.threedee.environment;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import org.simbrain.resource.ResourceManager;
import org.simbrain.world.threedee.Collision;
import org.simbrain.world.threedee.MultipleViewElement;
import org.simbrain.world.threedee.SpatialData;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.renderer.Renderer;
import com.jme.util.TextureManager;

/**
 * A multiple view element that represents the sky
 * in an environment.
 */
public class Sky extends MultipleViewElement<SkyDome> {
    /** The number of planes for the dome. */
    private static final int PLANES = 5;
    /** The number of radial samples for the dome. */
    private static final int RADIAL_SAMPLES = 12;
    /** The radius of the dome. */
    private static final float RADIUS = 200;
    
    /** The count of views that have been created. */
    private int count = 0;
    
    /**
     * Returns a new SkyDome instance.
     * 
     * @return A new SkyDome instance.
     */
    @Override
    public SkyDome create() {
//        return new Skybox("sky" + ++count, 200, 200, 200);
        return new SkyDome("sky" + ++count, PLANES, RADIAL_SAMPLES, RADIUS);
    }

    /**
     * Initializes a Skybox.
     * 
     * @param renderer the renderer
     * @param skybox the skybox
     */
    @Override
    public void initSpatial(final Renderer renderer, final SkyDome skybox) {
        skybox.setRenderer(renderer);

        ImageIcon image = ResourceManager.getImageIcon("sky.png");
        
        BufferedImage img =
            new BufferedImage(
            image.getIconWidth(),
            image.getIconHeight(),
            BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.drawImage(image.getImage(), null, null);
        g.dispose();
        
        image = new ImageIcon(img);
        image.setDescription("sky");
        
        Texture skyTexture = TextureManager.loadTexture(image.getImage(),
                Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR, true);
        
        skybox.initialize();
        
        skybox.setTexture(skyTexture, 0);
        
        skybox.setModelBound(new BoundingBox());
        skybox.updateModelBound();

        skybox.setRenderQueueMode(Renderer.QUEUE_OPAQUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSpatial(final SkyDome box) {
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
    public void collision(final Collision collision) {
        /* no implementation */
    }
}
