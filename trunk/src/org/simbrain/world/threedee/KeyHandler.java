package org.simbrain.world.threedee;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class KeyHandler implements KeyListener {
    private static final Logger LOGGER = Logger.getLogger(KeyHandler.class);
    
    private final Map<Integer, Agent.Action> bindings = new HashMap<Integer, Agent.Action>();
    public final Agent.Input input = new Agent.Input();
    
    public void addBinding(int key, Agent.Action action) {
        bindings.put(key, action);
    }
    
//    @Override
    public void keyPressed(KeyEvent e) {
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
