package org.simbrain.world.threedworld.engine;

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.awt.AwtKeyInput;
import com.jme3.input.awt.AwtMouseInput;
import com.jme3.opencl.Context;
import com.jme3.renderer.Renderer;
import com.jme3.system.*;
import org.simbrain.world.threedworld.ThreeDImagePanel;

import java.awt.event.MouseEvent;

public class ThreeDContext implements JmeContext {

    private JmeContext actualContext;
    private AppSettings settings = new AppSettings(true);
    private SystemListener listener;
    private ThreeDImagePanel panel;
    private AwtMouseInput mouseInput = new AwtMouseInput() {
        @Override
        public void mousePressed(MouseEvent event) {
            int button = event.getButton();
            if (button == MouseEvent.BUTTON1 && event.isControlDown())
                button = MouseEvent.BUTTON3;
            MouseEvent flippedEvent = new MouseEvent(event.getComponent(), event.getID(), event.getWhen(), 0, event.getX(), event.getComponent().getHeight() - event.getY(), 1, false, button);
            super.mousePressed(flippedEvent);
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            int button = event.getButton();
            if (button == MouseEvent.BUTTON1 && event.isControlDown())
                button = MouseEvent.BUTTON3;
            MouseEvent flippedEvent = new MouseEvent(event.getComponent(), event.getID(), event.getWhen(), 0, event.getX(), event.getComponent().getHeight() - event.getY(), 1, false, button);
            super.mouseReleased(flippedEvent);
        }
    };
    private AwtKeyInput keyInput = new AwtKeyInput();
    private boolean lastThrottleState = false;

    public ThreeDContext() {}

    @Override
    public Type getType() {
        return Type.OffscreenSurface;
    }

    @Override
    public void setSystemListener(SystemListener listener) {
        this.listener = listener;
    }

    @Override
    public AppSettings getSettings() {
        return settings;
    }

    @Override
    public Renderer getRenderer() {
        return actualContext.getRenderer();
    }

    @Override
    public Context getOpenCLContext() {
        return (Context) actualContext;
    }

    @Override
    public MouseInput getMouseInput() {
        return mouseInput;
    }

    @Override
    public KeyInput getKeyInput() {
        return keyInput;
    }

    @Override
    public JoyInput getJoyInput() {
        return null;
    }

    @Override
    public TouchInput getTouchInput() {
        return null;
    }

    @Override
    public Timer getTimer() {
        return actualContext.getTimer();
    }

    @Override
    public boolean isCreated() {
        return actualContext != null && actualContext.isCreated();
    }

    @Override
    public boolean isRenderable() {
        return actualContext != null && actualContext.isRenderable();
    }

    public ThreeDImagePanel createPanel() {
        panel = new ThreeDImagePanel();
        mouseInput.setInputSource(panel);
        keyInput.setInputSource(panel);
        return panel;
    }

    private void initInThread() {
        listener.initialize();
    }

    private void updateInThread() {
        boolean needThrottle = !panel.isShowing() || !panel.getImageSource().isEnabled();
        if (lastThrottleState != needThrottle) {
            lastThrottleState = needThrottle;
        }
        if (needThrottle) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
        listener.update();
    }

    private void destroyInThread() {
        listener.destroy();
    }

    @Override
    public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
        this.settings.setRenderer(AppSettings.LWJGL_OPENGL2);
        if (actualContext != null) {
            actualContext.setSettings(settings);
        }
    }

    @Override
    public void create(boolean wait) {
        if (actualContext != null) {
            throw new IllegalStateException("ThreeDContext cannot be recreated");
        }
        actualContext = JmeSystem.newContext(settings, Type.OffscreenSurface);
        actualContext.setSystemListener(new Listener());
        actualContext.create(wait);
    }

    @Override
    public void destroy(boolean wait) {
        if (actualContext == null)
            throw new IllegalStateException("ThreeDContext cannot be destroyed");
        actualContext.destroy(wait);
    }

    @Override
    public void setTitle(String title) {
    }

    @Override
    public void setAutoFlushFrames(boolean enabled) {
    }

    @Override
    public void restart() {
    }

    private class Listener implements SystemListener {
        @Override
        public void initialize() {
            initInThread();
        }

        @Override
        public void reshape(int width, int height) {
            throw new IllegalStateException();
        }

        @Override
        public void update() {
            updateInThread();
        }

        @Override
        public void requestClose(boolean escapeIsPressed) {
            throw new IllegalStateException();
        }

        @Override
        public void gainFocus() {
            throw new IllegalStateException();
        }

        @Override
        public void loseFocus() {
            throw new IllegalStateException();
        }

        @Override
        public void handleError(String message, Throwable throwable) {
            listener.handleError(message, throwable);
        }

        @Override
        public void destroy() {
            destroyInThread();
        }
    }

}
