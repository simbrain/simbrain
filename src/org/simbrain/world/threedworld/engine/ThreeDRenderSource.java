package org.simbrain.world.threedworld.engine;

import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;
import com.jme3.util.Screenshots;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.simbrain.world.imageworld.ImageSourceAdapter;

/**
 * ThreeDRenderer joins a JME3 rendering buffer to an ImageSource interface. It allows
 * Simbrain classes to easily use and filter the output of a rendered ThreeDWorld.
 * @author Tim Shea
 */
public class ThreeDRenderSource extends ImageSourceAdapter implements SceneProcessor {
    private boolean attachAsMain = false;
    private FrameBuffer frameBuffer;
    private ByteBuffer byteBuffer;
    private IntBuffer intBuffer;
    private RenderManager renderManager;
    private BufferedImage unfilteredImage;
    private BufferedImageOp flip;
    private List<ViewPort> viewPorts = new ArrayList<ViewPort>();

    /**
     * Create a new ThreeDRenderSource. The source will not be enabled until the initialization
     * callback from the rendering engine.
     * @param width The width of the rendered images.
     * @param height The height of the rendered images.
     */
    public ThreeDRenderSource(int width, int height) {
        //setWidth(width);
        //setHeight(height);
        setEnabled(false);
    }

    /**
     * @return Return the framebuffer in which the 3d engine draws viewports.
     */
    public FrameBuffer getFrameBuffer() {
        return frameBuffer;
    }

    /**
     * @return Return the integer buffer used to copy the framebuffer to an image.
     */
    public IntBuffer getIntBuffer() {
        return intBuffer;
    }

    /**
     * @return Return the image rendered by the engine before any filters are applied.
     */
    public BufferedImage getUnfilteredImage() {
        return unfilteredImage;
    }

    //@Override
    public void updateImage() {
        setCurrentImage(unfilteredImage);
        //super.updateImage();
    }

    /**
     * Attach a list of viewports to this render source, causing their contents to display
     * in the image.
     * @param overrideMainFramebuffer Use this source as the main accelerated frame buffer.
     * @param viewPorts The viewports to attach to this source.
     */
    public void attach(boolean overrideMainFramebuffer, ViewPort... viewPorts) {
        if (this.viewPorts.size() > 0) {
            throw new RuntimeException("ThreeDView already attached to ViewPort");
        }
        this.viewPorts.addAll(Arrays.asList(viewPorts));
        this.viewPorts.get(this.viewPorts.size() - 1).addProcessor(this);
        this.attachAsMain = overrideMainFramebuffer;
    }

    @Override
    public void initialize(RenderManager renderManager, ViewPort viewPort) {
        if (this.renderManager == null) {
            this.renderManager = renderManager;
            resize(getWidth(), getHeight());
            setEnabled(true);
        }
    }

    //@Override
    public void resize(int width, int height) {
        byteBuffer = BufferUtils.ensureLargeEnough(byteBuffer, width * height * 4);
        intBuffer = byteBuffer.asIntBuffer();
        frameBuffer = new FrameBuffer(width, height, 1);
        frameBuffer.setDepthBuffer(Format.Depth);
        frameBuffer.setColorBuffer(Format.RGB8);
        unfilteredImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        if (attachAsMain) {
            renderManager.getRenderer().setMainFrameBufferOverride(frameBuffer);
        }
        for (ViewPort viewPort : viewPorts) {
            if (!attachAsMain) {
                viewPort.setOutputFrameBuffer(frameBuffer);
            }
            viewPort.getCamera().resize(width, height, true);
            // NOTE: Hack alert. This is done ONLY for custom framebuffers.
            // Main framebuffer should use RenderManager.notifyReshape().
            for (SceneProcessor sceneProcessor : viewPort.getProcessors()) {
                sceneProcessor.reshape(viewPort, width, height);
            }
        }
        /*removeFilter(flip);
        flip = ImageFilters.flip(height);
        addFilter(flip);
        super.resize(width, height);
        */
    }

    @Override
    public boolean isInitialized() {
        return frameBuffer != null;
    }

    @Override
    public void preFrame(float tpf) { }

    @Override
    public void postQueue(RenderQueue renderQueue) { }

    @Override
    public void postFrame(FrameBuffer out) {
        if (!attachAsMain && out != frameBuffer) {
            throw new IllegalStateException("FrameBuffer was changed");
        }
        if (isEnabled()) {
            byteBuffer.clear();
            renderManager.getRenderer().readFrameBuffer(frameBuffer, byteBuffer);
            Screenshots.convertScreenShot2(intBuffer, unfilteredImage);
            updateImage();
        }
    }

    @Override
    public void reshape(ViewPort viewPort, int width, int height) { }

    @Override
    public void cleanup() {
        setEnabled(false);
        if (attachAsMain) {
            renderManager.getRenderer().setMainFrameBufferOverride(null);
        }
        for (ViewPort viewPort : viewPorts) {
            if (!attachAsMain) {
                viewPort.setOutputFrameBuffer(null);
            }
            viewPort.getProcessors().remove(this);
        }
    }
}
