package org.simbrain.world.threedee.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.world.threedee.Agent;
import org.simbrain.world.threedee.CanvasHelper;
import org.simbrain.world.threedee.Environment;
import org.simbrain.world.threedee.ThreeDeeComponent;

public class MainConsole extends DesktopComponent<ThreeDeeComponent> {

    private static final long serialVersionUID = 1L;

    private Map<AgentView, Component> views = new HashMap<AgentView, Component>();
    
    public MainConsole(ThreeDeeComponent workspaceComponent) {
        super(workspaceComponent);
    }

    private final int WIDTH = 512;
    private final int HEIGHT = 384;
    
    private void createView(Agent agent, Environment environment) {
        AgentView view = new AgentView(agent, environment, WIDTH, HEIGHT);
        
        CanvasHelper canvas = new CanvasHelper(WIDTH, HEIGHT, view);

        JFrame innerFrame = new JFrame("agent " + agent.getName());
        
        views.put(view, innerFrame);
        
        BorderLayout layout = new BorderLayout();
        innerFrame.getRootPane().setLayout(layout);
        
        innerFrame.getRootPane().add(canvas.getCanvas());
        
//        KeyHandler handler = getHandler();
//        
//        agent.addInput(0, handler.input);
        // TODO ???
//        innerFrame.addKeyListener(handler);

        innerFrame.setSize(WIDTH, HEIGHT);
    }
    
    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    @Override
    public String getFileExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void open(File openFile) {
        // TODO Auto-generated method stub
    }

    @Override
    public void save(File saveFile) {
        // TODO Auto-generated method stub
    }
}
