package org.simbrain.world.threedee;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.apache.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;

public class OpenGLTool extends JPanel {
    private static final long serialVersionUID = 1L;

    public static Logger logger = Logger.getLogger(OpenGLTool.class);

    static {
        Sys.initialize();
    }

    /** the timer that controls animation */
    private Timer animationTimer = new Timer(10, new ActionListener() {
        /**
         * Instruct the component to paint its entire bounds
         * 
         * @param e
         */
        public void actionPerformed(ActionEvent e) {
            paintImmediately(0, 0, getWidth(), getHeight());
        }
    });

    /** the native opengl pBuffer that we render the scene into */
    private Pbuffer pBuffer;
    /** the image that will be rendered to the swing component */
    private BufferedImage renderedImage; 
    /** the int[] that represents the backing store of our BufferedImage */
    private int[] imageData;
    /** the int buffer that will host the captured opengl data */
    private IntBuffer intBuffer;
    
    private boolean isInitialized = false;
    
    private int width; 
    private int height; 
    private int bitDepth = 32;
    private int refreshRate = 60;
    
    private final int alphaBits = 0;
    private final int stencilBits = 0;
    private final int depthBufferBits = 0;
    private final int sampleBits = 0;

    private float rotation = 0.1f;
    
    /**
     * Creates a JPanel that can be rendered to by LWJGL. It sets itself to
     * opaque by default so that Swing knows that we will render our entire
     * bounds and therefore doesn't need to do any checks
     */
    public OpenGLTool() {
        setOpaque(true);
    }

    private DisplayMode getMode() throws LWJGLException {

        DisplayMode[] modes = Display.getAvailableDisplayModes();

        int width = 800;
        int height = 600;
        
        for (int i = 0; i < modes.length; i++) {
            System.out.println(modes[i].getWidth() + " / " + modes[i].getHeight());

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
        logger.debug("Creating headless window " + width + "/" + height + "/" + bitDepth);

        width = getWidth();
        height = getHeight();
        
        DisplayMode mode = getMode();

        PixelFormat format = new PixelFormat(bitDepth, alphaBits, depthBufferBits, stencilBits,
                sampleBits);

        Display.setDisplayMode(mode);

        // create a PbUffer that will store the contents of our renderings
        pBuffer = new Pbuffer(width, height, format, null, null);
        pBuffer.makeCurrent();
        
        /* 
         * create a buffered image that is of the size of this component
         * we will render directly into this buffered image in Swing
         * fashion so we can render our interface in Swing properly and get
         * the image data that is backing this buffered image so we can write
         * directly to the internal data structure with no unnecessary copy
         */
        renderedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        imageData = ((DataBufferInt) renderedImage.getRaster().getDataBuffer()).getData();
        /* create an int buffer to store the captured data from OpenGL */
        intBuffer = ByteBuffer.allocateDirect(width * height * 4)
            .order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();

        isInitialized = true;

        start();
    }

    /**
     * override update to avoid clearing
     */
    public void update(Graphics g) {
        paint(g);
    }

    public void start() {
        logger.debug("Starting animation timer");
        animationTimer.start();
        // startTime = System.currentTimeMillis();
    }

    public void stop() {
        logger.debug("Stopping animation timer");
        animationTimer.stop();
    }

    public void paintComponent(Graphics g) {
        if (!isInitialized) {
            try {
                initComponent();
            } catch (LWJGLException e) {
                throw new RuntimeException(e);
            }
        } else {
            render();

            /* make sure we're done drawing before capturing the frame */
            GL11.glFlush();
            /* capture the result */
            grabGLFrame();
            /* draw the result to this component */
            g.drawImage(renderedImage, 0, 0, null);
        }
    }

    /**
     * Captures the currently rendered frame to a buffered image so it can be
     * rendered to a Swing component without any repaint issues. Most assuredly
     * will be slower than direct rendering, but will be compliant with Swing
     */
    private void grabGLFrame() {
        GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, intBuffer);

        intBuffer.clear();

        /* dump the captured data into the buffered image */
        for (int x = height; --x >= 0;) {
            intBuffer.get(imageData, x * width, width);
        }

        intBuffer.flip();
    }
    
    private void render() {
         GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        
         GL11.glRotatef(rotation,0.0f,1.0f,0.0f); // Rotate The Triangle On
         GL11.glBegin(GL11.GL_TRIANGLES); // Drawing Using Triangles
         GL11.glVertex3f( 0.0f, 1.0f, 0.0f); // Top
         GL11.glVertex3f(-1.0f,-1.0f, 0.0f); // Bottom Left
         GL11.glVertex3f( 1.0f,-1.0f, 0.0f); // Bottom Right
         GL11.glEnd();
        
         rotation += 0.2f;
    }
}