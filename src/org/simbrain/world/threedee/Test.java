package org.simbrain.world.threedee;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.UIManager;

import com.jme.renderer.Renderer;

public class Test {
    static Environment environment = new Environment();
    
//    public static final void main(String[] args ) {
//        JFrame frame = new JFrame();
//        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//        
//        frame.setBounds(50, 50,
//                screenSize.width  - 50*2,
//                screenSize.height - 50*2);
//        
//        JDesktopPane desktop = new JDesktopPane();
//        JInternalFrame internal = new JInternalFrame();
//        internal.setVisible(true);
////        OpenGLTool panel = new OpenGLTool();
//        
////        panel.setPreferredSize( new Dimension( 800, 600 ) );
//        
//        
////        internal.getContentPane().add(panel);
////        desktop.add(internal);
//        frame.getContentPane().add( desktop );
//        
//        desktop.add(internal);
//        
////        internal.setVisible(true);
//        
////        frame.pack();
//        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//        frame.setVisible( true );
//    }
    
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
        
        createView1(test1, "1", 512, 384);
        createView1(test2, "2", 512, 384);
//        createView1(test2, "1", 512, 384);
//        createView2(test1, "1", 410, 315);
//        createView3(512, 384, view);
        
//        System.out.println("buffer? " + view.getBuffer());
        
//        createLWIF(400, 300);
        
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
    
    private static void createLWIF(int width, int height) {
        JInternalFrame internal = new JInternalFrame();
        internal.setSize(width, height);
        
        internal.getContentPane().add(new JButton("test"));
        
        internal.setVisible(true);
        mainPanel.add(internal);
    }
    
    private static AwtView createView1(Test demo, String title, int width, int height) {
        environment.add(demo.agent);
        AwtView view = new AwtView(demo.agent, environment, width, height);
        CanvasHelper canvas = new CanvasHelper(width, height, view);

        JFrame innerFrame = new JFrame();
        shutdown.frames.add(innerFrame);
                
        BorderLayout layout = new BorderLayout();
        innerFrame.getRootPane().setLayout(layout);
        
        innerFrame.getRootPane().add(canvas.getCanvas());
        innerFrame.addKeyListener(demo.handler);
        innerFrame.setSize(width, height);
        
        return view;
    }
    
    private static void createView2(Test demo, String title, int width, int height) {
        JInternalFrame internal = new JInternalFrame();
        internal.setSize(width, height);
        
        environment.add(demo.agent);
        
        View panel = new View(demo.agent, environment, 400, 300);
//        panel.setPreferredSize( new Dimension( width, height ) );      
        internal.getContentPane().add(panel);
        
        frame.addKeyListener(demo.handler);
        
        internal.setVisible(true);
        mainPanel.add(internal);
    }
    
    private static void createView3(int width, int height, AwtView view) {
        JInternalFrame internal = new JInternalFrame();
        internal.setSize(width + 10, height + 35);
        
//        environment.add(demo.agent);
        
        TestJPanel panel = new TestJPanel(width, height, view);
        internal.getContentPane().add(panel);
        
        internal.setVisible(true);
        mainPanel.add(internal);
    }
    
    private static void finish() {
        frame.setSize(800, 600);
        frame.setState(JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        handler.addBinding(KeyEvent.VK_A, Agent.Action.UP);
        handler.addBinding(KeyEvent.VK_Z, Agent.Action.DOWN);
        handler.addBinding(KeyEvent.VK_U, Agent.Action.RISE);
        handler.addBinding(KeyEvent.VK_J, Agent.Action.FALL);
        handler.addBinding(KeyEvent.VK_G, Agent.Action.DROP);
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