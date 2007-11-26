package org.simbrain.world.threedee.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.simbrain.world.threedee.Moveable;

/**
 * Binds key events to Moveable actions and provides a Input instance that can
 * be set on a Moveable instance.
 * 
 * @author Matt Watson
 */
public class KeyHandler implements KeyListener {
    private static final Logger LOGGER = Logger.getLogger(KeyHandler.class);

    /** The bindings set for this handler. */
    private final Map<Integer, Moveable.Action> bindings = new HashMap<Integer, Moveable.Action>();

    /** An input that can be set on a Moveable instance. */
    public final Moveable.Input input = new Moveable.Input();

    /**
     * Adds an new binding in this handler.
     * 
     * @param key the key to bind
     * @param action the Moveable action to bind the key to
     * 
     * @see java.awt.event.KeyEvent
     */
    public void addBinding(final int key, final Moveable.Action action) {
        bindings.put(key, action);
    }

    /**
     * Handles a key being pressed down.
     */
    public void keyPressed(final KeyEvent e) {
        LOGGER.trace("keypressed:" + e);
        input.set(bindings.get(e.getKeyCode()));
    }

    /**
     * Handles a key being released.
     */
    public void keyReleased(final KeyEvent e) {
        input.clear(bindings.get(e.getKeyCode()));
    }

    /**
     * Handles a key being typed (no implementation).
     */
    public void keyTyped(final KeyEvent e) {
        /* no implementation */
    }
}
