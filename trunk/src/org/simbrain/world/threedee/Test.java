package org.simbrain.world.threedee;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.UIManager;

public class Test {
    static Environment environment = new Environment();
    
    public static void main(String[] args)
    {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        Test test1 = new Test();
        Test test2 = new Test();
        
        init("3D Demo");
        
        createView(test1, "1", 500, 500);
        createView(test2, "2", 500, 500);
        
        finish();
    }

    static JDesktopPane mainPanel = new JDesktopPane();
    static JFrame frame;
    static Shutdown shutdown = new Shutdown();
    
    private static void init(String title) {
        frame = new JFrame();
        
        frame.setTitle(title);
//        frame.addKeyListener(listener);
        mainPanel = new JDesktopPane();
        frame.getContentPane().add(mainPanel);
        frame.addWindowListener(shutdown);
    }
    
    private static void createView(Test demo, String title, int width, int height) {
        environment.add(demo.agent);
        View view = new View(demo.agent, environment, width, height);
        CanvasHelper canvas = new CanvasHelper(width, height, view);

        JFrame innerFrame = new JFrame();
        shutdown.frames.add(innerFrame);
                
        BorderLayout layout = new BorderLayout();
        innerFrame.getRootPane().setLayout(layout);
        
        innerFrame.getRootPane().add(canvas.getCanvas());
        innerFrame.addKeyListener(demo.handler);
        innerFrame.setSize(width, height);        
    }
    
    private static void finish() {
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
        
        for (JFrame frame : shutdown.frames) {
            frame.setVisible(true);
        }
    }
    
    final Agent agent;
    final KeyHandler handler;
    
    Test() {
        agent = new Agent();
        handler = new KeyHandler();
        agent.addInput(0, handler.input);
        setBindings();
    }
    
    private void setBindings() {
        handler.addBinding(KeyEvent.VK_LEFT, Agent.Action.LEFT);
        handler.addBinding(KeyEvent.VK_RIGHT, Agent.Action.RIGHT);
        handler.addBinding(KeyEvent.VK_UP, Agent.Action.FORWARD);
        handler.addBinding(KeyEvent.VK_DOWN, Agent.Action.BACKWARD);
    }
    
    private static class Shutdown implements WindowListener {//extends WindowAdapter {
        List<JFrame> frames = new ArrayList<JFrame>();
        
        public void windowClosed(WindowEvent e) {
            for (JFrame frame : frames) {
                frame.dispose();
            }
        }

        public void windowActivated(WindowEvent e) {
            for (JFrame frame : frames) {
                frame.setVisible(true);
            }
        }

        public void windowClosing(WindowEvent e) {
            // TODO Auto-generated method stub
            
        }

        public void windowDeactivated(WindowEvent e) {
            // TODO Auto-generated method stub
            
        }

        public void windowDeiconified(WindowEvent e) {
            // TODO Auto-generated method stub
            
        }

        public void windowIconified(WindowEvent e) {
            // TODO Auto-generated method stub
            
        }

        public void windowOpened(WindowEvent e) {
            // TODO Auto-generated method stub
            
        }
    }
}