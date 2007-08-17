package org.simbrain.world.threedee;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

public class KeyHandler implements KeyListener {
    private final Map<Integer, Agent.Action> bindings = new HashMap<Integer, Agent.Action>();
    public final Agent.Input input = new Agent.Input();
    
    public void addBinding(int key, Agent.Action action) {
        bindings.put(key, action);
    }
    
//    @Override
    public void keyPressed(KeyEvent e) {
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
