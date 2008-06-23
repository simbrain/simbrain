package org.simbrain.world.threedee.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.world.threedee.Agent;
import org.simbrain.world.threedee.CanvasHelper;
import org.simbrain.world.threedee.ThreeDeeComponent;
import org.simbrain.world.threedee.sensors.Sight;

/**
 * The main panel from which the 3D environment can be controlled.
 * 
 * @author Matt Watson
 */
public class MainConsole extends GuiComponent<ThreeDeeComponent> {
    /** The number of milliseconds between refresh events. */
    public static final int REFRESH_WAIT = 50;
    
    /** The default serial version ID. */
    private static final long serialVersionUID = 1L;
    
    /** Temporary hard-coded width of the frames. */
    private static final int WIDTH = 512;
    /** Temporary hard-coded height of the frames. */
    private static final int HEIGHT = 384;
    
    /** All the current views. */
    private final Map<AgentView, JFrame> views = new HashMap<AgentView, JFrame>();
    /** The parent Component. */
    private final ThreeDeeComponent component;
    
    /** BorderLayout for root panel. */
    private BorderLayout layout;
    /** The custom root panel. */
    private JPanel root;
    /** The panel that hold all the Agent controls. */
    private JPanel agents;
    
    /** Timer that fires the update operation. */
    private final Timer timer;
    
    /**
     * Creates a new main console.
     * 
     * @param component The parent component.
     */
    public MainConsole(GenericFrame frame, final ThreeDeeComponent component) {
        super(frame, component);
        this.component = component;
        this.timer = component.getTimer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postAddInit() {
        layout = new BorderLayout();
        root = new JPanel();
        
        root.setLayout(layout);
        root.add(mainPanel(), BorderLayout.NORTH);
        
        agents = new JPanel(new GridLayout(0, 1));
        root.add(agents, BorderLayout.CENTER);
        
        add(root);
        
        for (Agent agent : component.getAgents()) {
            newAgentPanel(agent);
        }
        
        getParentFrame().pack();
    }
    
    /**
     * Returns a new main panel.
     * 
     * @return A new main panel.
     */
    private JPanel mainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 1));
        
        agents = new JPanel(new GridLayout(0, 1));
        
        panel.add(new JButton(new NewAgentAction()));
        
        return panel;
    }
    
    Map<Agent, JPanel> panels = new HashMap<Agent, JPanel>();
    
    /**
     * Creates a new Agent panel for the provided panel.
     * 
     * @param agent The Agent to create the panel for.
     */
    private void newAgentPanel(final Agent agent) {
        JPanel panel = new JPanel();
        
        panels.put(agent, panel);
        
        panel.add(new JButton(new CreateAgentViewAction(agent)));
//        panel.add(new JButton(new AbstractAction("Vision") {
//            private static final long serialVersionUID = 1L;
//
//            public void actionPerformed(ActionEvent e) {
//                new Thread(new Runnable(){
//                    public void run() {
//                        final Sight sight = agent.getBindings().createSight();
//                        
//                        innerFrame.addWindowListener(new WindowAdapter() {
//                            public void windowClosed(final WindowEvent e) {
//                                sight.close();
//                            }
//                        });
//                    }
//                }).start();
//            }
//        }));
        agents.add(panel);
        getParentFrame().pack();
    }
    
    /**
     * Action for a new agent.
     * 
     * @author Matt Watson
     */
    private class NewAgentAction extends AbstractAction {
        /** The default serial version ID. */
        private static final long serialVersionUID = 1L;

        /* init block */
        {
            this.putValue(AbstractAction.NAME, "New Agent");
        }
        
        /**
         * {@inheritDoc}
         */
        public void actionPerformed(final ActionEvent e) {
            Agent agent = component.createAgent();
            
            newAgentPanel(agent);
        }
    };
    
    /**
     * Action for a new agent view.
     * 
     * @author Matt Watson
     */
    private class CreateAgentViewAction extends AbstractAction {
        /** The default serial version ID. */
        private static final long serialVersionUID = 1L;
        /** The agent to create a view for. */
        private Agent agent;
        
        /**
         * Creates a new instance.
         * 
         * @param agent The agent to create a view for.
         */
        CreateAgentViewAction(final Agent agent) {
            this.agent = agent;
            this.putValue(AbstractAction.NAME, "Create View");
        }
        
        /**
         * {@inheritDoc}
         */
        public void actionPerformed(final ActionEvent e) {
            createView(agent);
        }
    };
    
    JFrame innerFrame;
    
    /**
     * Creates a new view for an agent.
     * 
     * @param agent the agent to create a view for.
     */
    private void createView(final Agent agent) {
        final AgentView view = new AgentView(agent, component.getEnvironment(), WIDTH, HEIGHT);
        final CanvasHelper canvas = new CanvasHelper(WIDTH, HEIGHT, view);
        innerFrame = new JFrame("Agent " + agent.getName());
        innerFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                canvas.getCanvas().repaint();
            }
        };
        
        innerFrame.addWindowListener(new WindowAdapter() {
            public void windowClosed(final WindowEvent e) {
                view.close();
                task.cancel();
            }
        });
        
        views.put(view, innerFrame);
        
        BorderLayout layout = new BorderLayout();
        
        innerFrame.getRootPane().setLayout(layout);
        innerFrame.getRootPane().add(canvas.getCanvas());
        
        timer.schedule(task, REFRESH_WAIT, REFRESH_WAIT);
        
        KeyHandler handler = getHandler(agent);
        agent.addInput(0, handler.getInput());
        innerFrame.addKeyListener(handler);
        innerFrame.setSize(WIDTH, HEIGHT);
        innerFrame.setResizable(false);
        innerFrame.setVisible(true);
        
        final JButton button = new JButton(new AbstractAction("Vision") {
            private static final long serialVersionUID = 1L;
            
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable(){
                    public void run() {
                        final Sight sight = agent.getBindings().createSight();
                        
                        innerFrame.addWindowListener(new WindowAdapter() {
                            public void windowClosed(final WindowEvent e) {
                                System.out.println("closed");
                                sight.close();
                            }
                        });
                    }
                }).start();
            }
        });
        
        panels.get(agent).add(button);
        
        innerFrame.addWindowListener(new WindowAdapter() {
            public void windowClosed(final WindowEvent e) {
                panels.get(agent).remove(button);
                getParentFrame().pack();
            }
        });
        
        getParentFrame().pack();
    }
    
    /**
     * Gets a key handler for an agent.
     * 
     * @param agent the agent to get a key handler for.
     * 
     * @return the new key handler.
     */
    private KeyHandler getHandler(final Agent agent) {
        KeyHandler handler = new KeyHandler();
        
        handler.addBinding(KeyEvent.VK_LEFT, agent.left());
        handler.addBinding(KeyEvent.VK_RIGHT, agent.right());
        handler.addBinding(KeyEvent.VK_UP, agent.forward());
        handler.addBinding(KeyEvent.VK_DOWN, agent.backward());
//        handler.addBinding(KeyEvent.VK_A, Moveable.Action.DOWN);
//        handler.addBinding(KeyEvent.VK_Z, Moveable.Action.UP);
//        handler.addBinding(KeyEvent.VK_U, Moveable.Action.RISE);
//        handler.addBinding(KeyEvent.VK_J, Moveable.Action.FALL);
        
        return handler;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void closing() {
//        component.closing();
        
        for (JFrame frame : views.values()) {
            frame.setVisible(false);
            frame.dispose();
        }
    }

}
