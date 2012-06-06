package org.simbrain.world.threedee.gui;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.apache.log4j.Logger;
import org.simbrain.world.threedee.Agent;
import org.simbrain.world.threedee.environment.Environment;

import com.jme.renderer.Renderer;
import com.jme.scene.state.CullState;
import com.jmex.awt.SimpleCanvasImpl;

/**
 * Implements a canvas that renders the perspective of an Agent.
 *
 * @author Matt Watson
 */
public class AgentView extends SimpleCanvasImpl {
    /** The static logger for the class. */
    private static final Logger LOGGER = Logger.getLogger(AgentView.class);
    /** The default serial version ID. */
    private static final long serialVersionUID = 1L;

    /** The environment this view is displaying. */
    private final Environment environment;

    /** The viewable that controls what is seen. */
    private final Agent agent;

    private final ConcurrentLinkedQueue<FutureTask<Matrix>> queue = new ConcurrentLinkedQueue<FutureTask<Matrix>>();

    /**
     * Constructs an instance with the provided Viewable and Environment at the
     * given width and height.
     *
     * @param viewable the viewable that controls the view
     * @param environment the environment this view displays
     * @param width the width
     * @param height the height
     */
    AgentView(final Agent viewable, final Environment environment,
            final int width, final int height) {
        super(width, height);

        this.environment = environment;
        this.agent = viewable;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Returns the renderer for the canvas.
     *
     * @return the renderer for the canvas
     */
    @Override
    public Renderer getRenderer() {
        return renderer;
    }

    /**
     * Calls init on the environment and viewable.
     */
    @Override
    public void simpleSetup() {
        LOGGER.debug("frustum left: " + cam.getFrustumLeft());
        LOGGER.debug("frustum right: " + cam.getFrustumRight());
        LOGGER.debug("frustum top: " + cam.getFrustumTop());
        LOGGER.debug("frustum bottom: " + cam.getFrustumBottom());

        /*
         * Sets up a cullstate to improve performance This will prevent
         * triangles that are not visible from be rendered.
         */
        CullState cs = renderer.createCullState();
        cs.setCullMode(CullState.CS_BACK);
        rootNode.setRenderState(cs);

        environment.init(agent, getRenderer(), rootNode);
    }

    public void close() {
        environment.remove(getRenderer());
    }

    /**
     * Calls update on the camera.
     */
    @Override
    public void simpleUpdate() {
        cam.update();
    }

    /**
     * Calls render on the viewable.
     */
    @Override
    public void simpleRender() {
        agent.render(cam);
    }

    @Override
    public void doRender() {
        super.doRender();

        for (FutureTask<Matrix> grab; (grab = queue.poll()) != null;) {
            grab.run();
        }
    }

    public BufferedImage getSnapshot() {
        Callable<Matrix> exe = new Callable<Matrix>() {
            public Matrix call() {
                Matrix matrix = new Matrix(width, height);
                renderer.grabScreenContents(matrix.buffer, 0, 0, width, height);

                return matrix;
            }
        };

        FutureTask<Matrix> grab = new FutureTask<Matrix>(exe);

        queue.add(grab);
        Matrix matrix;

        try {
            matrix = grab.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        int xOffset = 12;// (mode.getWidth() - width) / 2;
        int yOffset = 34;// (mode.getHeight() - height) / 2;

        BufferedImage image = new BufferedImage(width - xOffset, height
                - yOffset, BufferedImage.TYPE_INT_RGB);

        /* Grab each pixel information and set it to the BufferedImage info. */
        for (int x = 0; x < width - xOffset; x++) {
            for (int y = 0; y < height - yOffset; y++) {
                int rgb = matrix.get(x, (height - 1) - y);

                image.setRGB(x, y, rgb);
            }
        }

        return image;
    }

    private static class Matrix {
        final IntBuffer buffer;
        final int width;
        final int height;

        Matrix(int width, int height) {
            this.buffer = ByteBuffer.allocateDirect(width * height * 4)
                    .order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
            this.width = width;
            this.height = height;
        }

        int get(final int x, final int y) {
            try {
                return buffer.get((y * width) + x);
            } catch (Exception e) {
                System.err.println(x + ", " + y);
                e.printStackTrace();
                throw (RuntimeException) e;
            }
        }
    }
}
