package org.simbrain.world.threedee.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.world.threedee.Agent;
import org.simbrain.world.threedee.CanvasHelper;
import org.simbrain.world.threedee.KeyHandler;
import org.simbrain.world.threedee.Moveable;
import org.simbrain.world.threedee.ThreeDeeComponent;

public class MainConsole extends DesktopComponent<ThreeDeeComponent> {

    private static final long serialVersionUID = 1L;

    private Map<AgentView, Component> views = new HashMap<AgentView, Component>();
    
    ThreeDeeComponent component;
    
    public MainConsole(ThreeDeeComponent component) {
        super(component);
        this.component = component;
    }

    private final int WIDTH = 512;
    private final int HEIGHT = 384;
    
    BorderLayout layout;// = new BorderLayout();
    JPanel root;// = new JPanel();
    JPanel agents;
    
    public void postAddInit() {
        layout = new BorderLayout();
        root = new JPanel();
        
        root.setLayout(layout);
        root.add(mainPanel(), BorderLayout.NORTH);
        
        agents = new JPanel(new GridLayout(0,1));
        root.add(agents, BorderLayout.CENTER);
        
        getContentPane().add(root);
        
        pack();
    }
    
    private JPanel mainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1,1));
        
        agents = new JPanel(new GridLayout(0,1));
        
        panel.add(new JButton(new NewAgentAction()));
        
        return panel;
    }
    
    private void agentPanel(Agent agent) {
        JPanel panel = new JPanel();
        JButton button = new JButton(new CreateAgentViewAction(agent));
        
        panel.add(button);
        agents.add(panel);
        pack();
    }
    
    private class NewAgentAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        {
            this.putValue(AbstractAction.NAME, "New Agent");
        }
        
        public void actionPerformed(ActionEvent e) {
            Agent agent = component.createAgent();
            
            agentPanel(agent);
        }
    };
    
    private class CreateAgentViewAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        
        private Agent agent;
        
        {
            this.putValue(AbstractAction.NAME, "Create View");
        }
        
        CreateAgentViewAction(Agent agent) {
            this.agent = agent;
        }
        
        public void actionPerformed(ActionEvent e) {
            createView(agent);
        }
    };
    
    private void createView(Agent agent) {
        AgentView view = new AgentView(agent, component.getEnvironment(), WIDTH, HEIGHT);
        CanvasHelper canvas = new CanvasHelper(WIDTH, HEIGHT, view);
        JFrame innerFrame = new JFrame("agent " + agent.getName());
        
        views.put(view, innerFrame);
        
        BorderLayout layout = new BorderLayout();
        
        innerFrame.getRootPane().setLayout(layout);
        innerFrame.getRootPane().add(canvas.getCanvas());
        
        KeyHandler handler = getHandler();
        
        agent.addInput(0, handler.input);
        innerFrame.addKeyListener(handler);
        innerFrame.setSize(WIDTH, HEIGHT);
        innerFrame.setVisible(true);
    }
    
    private static KeyHandler getHandler() {
        KeyHandler handler = new KeyHandler();
        
        handler.addBinding(KeyEvent.VK_LEFT, Moveable.Action.LEFT);
        handler.addBinding(KeyEvent.VK_RIGHT, Moveable.Action.RIGHT);
        handler.addBinding(KeyEvent.VK_UP, Moveable.Action.FORWARD);
        handler.addBinding(KeyEvent.VK_DOWN, Moveable.Action.BACKWARD);
//        handler.addBinding(KeyEvent.VK_A, Moveable.Action.DOWN);
//        handler.addBinding(KeyEvent.VK_Z, Moveable.Action.UP);
//        handler.addBinding(KeyEvent.VK_U, Moveable.Action.RISE);
//        handler.addBinding(KeyEvent.VK_J, Moveable.Action.FALL);
        
        return handler;
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
