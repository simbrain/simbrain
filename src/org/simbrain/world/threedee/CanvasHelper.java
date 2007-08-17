package org.simbrain.world.threedee;

import java.awt.Canvas;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import com.jme.system.DisplaySystem;
import com.jmex.awt.JMECanvas;
import com.jmex.awt.JMECanvasImplementor;
import com.jmex.awt.lwjgl.LWJGLCanvas;

public class CanvasHelper {
    final LWJGLCanvas canvas;  
    final int width;
    final int height;
    
    public CanvasHelper(final int width, final int height, final JMECanvasImplementor impl) {
        this.width = width;
        this.height = height;
        
        /* make the canvas */
        canvas = (LWJGLCanvas) DisplaySystem.getDisplaySystem("lwjgl").createCanvas(width, height);

        /* add a listener... if window is resized, we can do something about it */
        canvas.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent ce) {
                impl.resizeCanvas(width, height);
            }
        });

        /* if canvas.setUpdateInput(false) is not called, these two lines must be called */
//            KeyInput.setProvider(KeyInput.INPUT_AWT);
//            AWTMouseInput.setup(canvas, false);

        JMECanvas jmeCanvas = ( (JMECanvas) canvas );
        jmeCanvas.setImplementor(impl);
        jmeCanvas.setUpdateInput( true );
        
        canvas.setBounds(0, 0, width, height);
        
        /* this prevents issues with LWJGL trying to handle input */
        canvas.setFocusable(false);
        canvas.setUpdateInput(false);
        
        startRepaintThread();
    }
    
    public Canvas getCanvas()
    {
        return canvas;
    }
    
    private void startRepaintThread() {
        new Thread() {
            { setDaemon(true); }
            public void run() {
                while (true) {
                    canvas.repaint();
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
}