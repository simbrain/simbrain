package org.simbrain.world.threedee.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.log4j.Logger;
import org.simbrain.world.threedee.Moveable;
import org.simbrain.world.threedee.Moveable.Action;

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
    public final Collection<Action> input = Collections.synchronizedCollection(new HashSet<Action>());
    
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
        LOGGER.trace("keypressed: " + e);
        input.add(bindings.get(e.getKeyCode()));
    }

    /**
     * Handles a key being released.
     */
    public void keyReleased(final KeyEvent e) {
        LOGGER.trace("keyreleased: " + e);
        input.remove(bindings.get(e.getKeyCode()));
        LOGGER.trace("input: " + input.size());
    }

    /**
     * Handles a key being typed (no implementation).
     */
    public void keyTyped(final KeyEvent e) {
        /* no implementation */
    }
}
