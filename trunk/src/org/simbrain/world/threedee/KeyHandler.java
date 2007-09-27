package org.simbrain.world.threedee;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Binds key events to Moveable actions and provides a Input instance
 * that can be set on a Moveable instance
 * 
 * @author Matt Watson
 */
public class KeyHandler implements KeyListener {
    private static final Logger LOGGER = Logger.getLogger(KeyHandler.class);
    
    /** the bindings set for this handler */
    private final Map<Integer, Moveable.Action> bindings = new HashMap<Integer, Moveable.Action>();
    /** An input that can be set on a Moveable instance */
    public final Moveable.Input input = new Moveable.Input();
    
    /**
     * adds an new binding in this handler
     * 
     * @param key the key to bind
     * @param action the Moveable action to bind the key to
     * 
     * @see java.awt.event.KeyEvent
     */
    public void addBinding(int key, Moveable.Action action) {
        bindings.put(key, action);
    }
    
    /**
     * handles a key being pressed down
     */
    public void keyPressed(KeyEvent e) {
        LOGGER.trace("keypressed:" + e);
        input.set(bindings.get(e.getKeyCode()));
    }

    /**
     * handles a key being released
     */
    public void keyReleased(KeyEvent e) {
        input.clear(bindings.get(e.getKeyCode()));
    }

    /**
     * handles a key being typed (no implementation)
     */
    public void keyTyped(KeyEvent e) {
        /* no implementation */
    }  
}
