package org.simbrain.world.threedee;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import org.simbrain.resource.ResourceManager;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.renderer.Renderer;
import com.jme.util.TextureManager;

/**
 * A multiple view element that represents the sky
 * in an environment.
 */
public class Sky extends MultipleViewElement<SkyDome> {
    int count = 0;
    
    /**
     * Creates a skybox.
     */
    @Override
    public SkyDome create() {
//        return new Skybox("sky" + ++count, 200, 200, 200);
        return new SkyDome("sky" + ++count, 5, 12, 200);
    }

    /**
     * Initializes a Skybox.
     * 
     * @param renderer the renderer
     * @param skybox the skybox
     */
    @Override
    public void initSpatial(final Renderer renderer, final SkyDome skybox) {
        skybox.renderer = renderer;

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

    @Override
    public void updateSpatial(SkyDome box) {
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
        /* no implementation */
    }
}
