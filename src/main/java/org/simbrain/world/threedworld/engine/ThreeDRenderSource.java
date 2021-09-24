package org.simbrain.world.threedworld.engine;

import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;
import org.simbrain.world.imageworld.ImageSource;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * ThreeDRenderer joins a JME3 rendering buffer to an ImageSource interface. It allows
 * Simbrain classes to easily use and filter the output of a rendered ThreeDWorld.
 *
 * @author Tim Shea
 */
public class ThreeDRenderSource extends ImageSource implements SceneProcessor {
    private boolean attachAsMain = false;
    private FrameBuffer frameBuffer;
    private ByteBuffer byteBuffer;
    private IntBuffer intBuffer;
    private RenderManager renderManager;
    private BufferedImage rawImage;
    private BufferedImage flippedImage;
    private BufferedImageOp flip;
    private ViewPort viewPort;

    /**
     * Create a new ThreeDRenderSource by specifying the viewport to render and whether the
     * source should use the main accelerated frame buffer.
     *
     * @param viewPort     The viewport to render to this image source.
     * @param attachAsMain Whether to use the main frame buffer. Set false for agent views.
     */
    public ThreeDRenderSource(ViewPort viewPort, boolean attachAsMain) {
        this.viewPort = viewPort;
        viewPort.addProcessor(this);
        this.attachAsMain = attachAsMain;
        setEnabled(false);
    }

    @Override
    public void initialize(RenderManager renderManager, ViewPort viewPort) {
        if (this.renderManager == null) {
            this.renderManager = renderManager;
            resize(viewPort.getCamera().getWidth(), viewPort.getCamera().getHeight());
            setEnabled(true);
        }
    }

    public void resize(int width, int height) {
        // Cannot resize until initialized
        if (renderManager == null) {
            return;
        }
        byteBuffer = BufferUtils.ensureLargeEnough(byteBuffer, width * height * 4);
        intBuffer = byteBuffer.asIntBuffer();
        frameBuffer = new FrameBuffer(width, height, 1);
        frameBuffer.setDepthBuffer(Format.Depth);
        frameBuffer.setColorBuffer(Format.RGB8);
        rawImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        flippedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        if (attachAsMain) {
            renderManager.getRenderer().setMainFrameBufferOverride(frameBuffer);
        } else {
            viewPort.setOutputFrameBuffer(frameBuffer);
        }

        viewPort.getCamera().resize(width, height, true);
        // NOTE: Hack alert. This is done ONLY for custom framebuffers.
        // Main framebuffer should use RenderManager.notifyReshape().
        for (SceneProcessor sceneProcessor : viewPort.getProcessors()) {
            sceneProcessor.reshape(viewPort, width, height);
        }

        AffineTransform flipTransform = AffineTransform.getScaleInstance(1, -1);
        flipTransform.translate(0, -height);
        flip = new AffineTransformOp(flipTransform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
    }

    @Override
    public boolean isInitialized() {
        return frameBuffer != null;
    }

    @Override
    public void preFrame(float tpf) {
    }

    @Override
    public void postQueue(RenderQueue renderQueue) {
    }

    @Override
    public void postFrame(FrameBuffer out) {
        if (!attachAsMain && out != frameBuffer) {
            throw new IllegalStateException("FrameBuffer was changed");
        }
        if (isEnabled()) {
            byteBuffer.clear();
            renderManager.getRenderer().readFrameBuffer(frameBuffer, byteBuffer);
            // TODO: Screenshots have changed in JME. Below is the start of new code.
            // ScreenshotAppState screenshot = new ScreenshotAppState();
            // screenshot.takeScreenshot();

            // Below is the prior code
            // Screenshots.convertScreenShot2(intBuffer, rawImage);
            flip.filter(rawImage, flippedImage);
            setCurrentImage(flippedImage);
        }
    }

    @Override
    public void reshape(ViewPort viewPort, int width, int height) {
    }

    @Override
    public void cleanup() {
        setEnabled(false);
        if (attachAsMain) {
            renderManager.getRenderer().setMainFrameBufferOverride(null);
        } else {
            viewPort.setOutputFrameBuffer(null);
        }
        viewPort.getProcessors().remove(this);
    }

    @Override
    public void setProfiler(AppProfiler profiler) {

    }
}
