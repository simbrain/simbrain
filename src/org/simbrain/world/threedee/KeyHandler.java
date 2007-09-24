package org.simbrain.world.threedee;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class KeyHandler implements KeyListener {
private static final Logger LOGGER = Logger.getLogger(KeyHandler.class);
    
    private final Map<Integer, Moveable.Action> bindings = new HashMap<Integer, Moveable.Action>();
    public final Moveable.Input input = new Moveable.Input();
    
    public void addBinding(int key, Moveable.Action action) {
        bindings.put(key, action);
    }
    
//    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("keypressed");
        LOGGER.trace("keypressed:" + e);
        input.set(bindings.get(e.getKeyCode()));
    }

//    @Override
    public void keyReleased(KeyEvent e) {
        input.clear(bindings.get(e.getKeyCode()));
    }

//    @Override
    public void keyTyped(KeyEvent e) {
    }  
}
