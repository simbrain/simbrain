package org.simbrain.world.threedee.junk;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;
import com.jme.system.lwjgl.LWJGLDisplaySystem;
import com.jme.util.Timer;

/**
 * experiment and as-of-yet not working implementation of 
 * a Swing compatible window on the environment
 * 
 * @author Matt Watson
 */
public abstract class ThreeDeeJPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(ThreeDeeJPanel.class);
    
    private static final long serialVersionUID = 1L;

    static {
        Sys.initialize();
    }

    /** the timer that controls animation */
    private final javax.swing.Timer animationTimer = new javax.swing.Timer(10, new ActionListener() {
        /**
         * Instruct the component to paint its entire bounds
         * 
         * @param e
         */
        public void actionPerformed(ActionEvent e) {
            paintImmediately(0, 0, getWidth(), getHeight());
        }
    });

    private final Timer timer =  Timer.getTimer();
    /** the image that will be rendered to the swing component */
//    private final BufferedImage renderedImage; 
    /** image data */
//    private int[] imageData;
    /** the int buffer that will host the captured opengl data */
    private final Matrix matrix;
    
    private boolean isInitialized = false;
    
//    protected int actualWidth; 
//    protected int actualHeight; 
//    private final int bitDepth = 32;
//    private final int refreshRate = 60;

    private final DisplayMode mode;
    
    protected Renderer renderer;
    protected Camera camera;
    protected final Node rootNode = new Node("rootNode");

//    Implementor implementor;
    
    /**
     * Creates a JPanel that can be rendered to by LWJGL. It sets itself to
     * opaque by default so that Swing knows that we will render our entire
     * bounds and therefore doesn't need to do any checks
     */
    public ThreeDeeJPanel(final int width, final int height, int bitDepth) {
        setOpaque(true);
        matrix = new Matrix(ByteBuffer.allocateDirect(width * height * 4)
            .order(ByteOrder.LITTLE_ENDIAN).asIntBuffer(), width, height);
        
        try {
            mode = getMode(width, height, bitDepth);
        } catch (LWJGLException e) {
            throw new RuntimeException(e);
        }
    }

    public ThreeDeeJPanel(final int width, final int height) {
        this(width, height, 32);
    }
    
    private DisplayMode getMode(int width, int height, int bitDepth) throws LWJGLException {

        DisplayMode[] modes = Display.getAvailableDisplayModes();
        final int refreshRate = 60;
        
        for (int i = 0; i < modes.length; i++) {
            LOGGER.trace(modes[i].getWidth() + " / " + modes[i].getHeight() + " / " + modes[i].getBitsPerPixel());

            if (modes[i].getWidth() == width && modes[i].getHeight() == height
                    && modes[i].getBitsPerPixel() == bitDepth
                    && (refreshRate == 0 || modes[i].getFrequency() == refreshRate)) {
                return modes[i];
            }
        }

        throw new RuntimeException("no mode found for width " + width + ", height " + height
                + ", bitdepth " + bitDepth + ", frequency " + refreshRate);
    }
    
    /**
     * @throws LWJGLException 
     * 
     */
    private void initComponent() throws LWJGLException {
        LOGGER.debug("Creating headless window " + mode.getWidth() + "/" 
            + mode.getHeight() + "/" + mode.getBitsPerPixel());
        
//        final int alphaBits = 0;
//        final int stencilBits = 0;
//        final int depthBufferBits = 0;
//        final int sampleBits = 0;
        
//        PixelFormat format = new PixelFormat(mode.getBitsPerPixel(), alphaBits, depthBufferBits, stencilBits,
//                sampleBits);

//        Display.setDisplayMode(mode);
        
        LWJGLDisplaySystem display = (LWJGLDisplaySystem) DisplaySystem.getDisplaySystem("lwjgl");
        
//        display.initForCanvas(width, height);
        
        display.createHeadlessWindow(mode.getWidth(), mode.getHeight(), mode.getBitsPerPixel());
//        JMECanvas canvas = (LWJGLCanvas) display.createCanvas(width, height);
//        Implementor implementor = new Implementor(width, height);
        
//        canvas.setImplementor(implementor);
        
//        display.setCurrentCanvas(canvas);
//        display.initForCanvas(width, height);
//        renderer = new LWJGLRenderer(width, height);
//        renderer.setHeadless(true);
//        display.getCurrentContext().setupRecords(renderer);
//        DisplaySystem.updateStates(renderer);
//        renderer = implementor.getRenderer();//display.getRenderer();

        renderer = display.getRenderer();
        LOGGER.debug("renderer: " + renderer);
        
        /**
         * Create a camera specific to the DisplaySystem that works with the
         * width and height
         */
        camera = renderer.createCamera(getWidth(), getHeight());
        renderer.setCamera(camera);

        // create a PbUffer that will store the contents of our renderings
//        pBuffer = display.getHeadlessDisplay();
//        pBuffer = new Pbuffer(width, height, format, null, null);
//        pBuffer.makeCurrent();
        
        /* 
         * create a buffered image that is of the size of this component
         * we will render directly into this buffered image in Swing
         * fashion so we can render our interface in Swing properly and get
         * the image data that is backing this buffered image so we can write
         * directly to the internal data structure with no unnecessary copy
         */
        
//        imageData = ((DataBufferInt) renderedImage.getRaster().getDataBuffer()).getData();
        
        /** Create rootNode */
//        rootNode = new Node("rootNode");

        /**
         * Create a ZBuffer to display pixels closest to the camera above
         * farther ones.
         */
        ZBufferState buf = renderer.createZBufferState();
        buf.setEnabled(true);
        buf.setFunction(ZBufferState.CF_LEQUAL);

        rootNode.setRenderState(buf);

        init();
        
        rootNode.updateGeometricState(0.0f, true);
        rootNode.updateRenderState();
        
        animationTimer.start();
        
        isInitialized = true;
    }

    /**
     * override update to avoid clearing
     */
    public void update(Graphics g) {
        paint(g);
    }

    public void stop() {
        LOGGER.debug("Stopping animation timer");
        animationTimer.stop();
    }
    
    public void paintComponent(Graphics g) {
        LOGGER.trace("rendering");
        if (!isInitialized) {
            try {
                initComponent();
            } catch (LWJGLException e) {
//                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            timer.update();
            float tpf = timer.getTimePerFrame();
            
            update();
            
            rootNode.updateGeometricState(tpf, true);
            
            renderer.clearBuffers();
            renderer.draw(rootNode);
            
            render();
            
            renderer.displayBackBuffer();
            
            /* make sure we're done drawing before capturing the frame */
            renderer.flush();
            
            matrix.buffer.clear();
            
            /* capture the result */
            renderer.grabScreenContents(matrix.buffer, 0, 0, mode.getWidth(), mode.getHeight());
            
//            /* dump the captured data into the buffered image */
//            for (int x = height - 1; x >= 0; x--) {
//                intBuffer.get(imageData, x * width, width);
//            }
            
            int width = getWidth();
            int height = getHeight();
            
            LOGGER.trace("width, height: " + width + ", " + height);
            
            BufferedImage renderedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//            int[] imageData = ((DataBufferInt) renderedImage.getRaster().getDataBuffer()).getData();
            
            int xOffset = (mode.getWidth() - width) / 2;
            int yOffset = (mode.getHeight() - height) / 2;
            
            LOGGER.trace("xOffset, yOffset: " + xOffset + ", " + yOffset);
            
            // TODO fix this!
         // Grab each pixel information and set it to the BufferedImage info.
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int rgb = matrix.get(x + xOffset, (mode.getHeight() - yOffset) - y);
                    
                    renderedImage.setRGB(x, y, rgb);
                }
            }
            
            matrix.buffer.flip();
            
            /* draw the result to this component */
            g.drawImage(renderedImage, 0, 0, null);
        }
    }
    
    private static class Matrix {
        final IntBuffer buffer;
        final int width;
        final int height;
        
        Matrix(IntBuffer buffer, int width, int height) {
            this.buffer = buffer;
            this.width = width;
            this.height = height;
        }
        
        int get(int x, int y) {
            return buffer.get((y * width) + x);
        }
    }
    
    protected abstract void init();
    
    protected abstract void update();
    
    protected abstract void render();
    
//    private class Implementor extends SimpleCanvasImpl {
//        final LWJGLCanvas canvas;
//        
//        Implementor(final int width, final int height) {
//            super(width, height);
//            
//            System.out.println("implementor: " + renderer);
//            
//            /* make the canvas */
//            canvas = (LWJGLCanvas) DisplaySystem.getDisplaySystem("lwjgl").createCanvas(width, height);
//
////            /* add a listener... if window is resized, we can do something about it */
////            canvas.addComponentListener(new ComponentAdapter() {
////                public void componentResized(ComponentEvent ce) {
////                    Implementor.this.resizeCanvas(width, height);
////                }
////            });
//
//            /* if canvas.setUpdateInput(false) is not called, these two lines must be called */
////                KeyInput.setProvider(KeyInput.INPUT_AWT);
////                AWTMouseInput.setup(canvas, false);
//
//            JMECanvas jmeCanvas = ( (JMECanvas) canvas );
//            jmeCanvas.setImplementor(this);
//            jmeCanvas.setUpdateInput( true );
//            
//            canvas.setBounds(0, 0, width, height);
//            
//            /* this prevents issues with LWJGL trying to handle input */
//            canvas.setFocusable(false);
//            canvas.setUpdateInput(false);
//            
////            canvas.paint(null);
//            
//            JFrame innerFrame = new JFrame();
//            innerFrame.getRootPane().add(canvas);
//            innerFrame.setVisible(true);
//            
//            startRepaintThread();
//        }
//        
//        private void startRepaintThread() {
//            new Thread() {
//                { setDaemon(true); }
//                public void run() {
//                    while (true) {
////                        System.out.println("repaint");
//                        canvas.repaint();
//                        try {
//                            sleep(10);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }.start();
//        }
//        
//        @Override
//        public void simpleSetup() {
////            System.out.println("simpleSetup: " + renderer);
//        }
//      
//        @Override
//        public void simpleRender() {
////            System.out.println("simpleRender: " + renderer);
//        }
//    }
}