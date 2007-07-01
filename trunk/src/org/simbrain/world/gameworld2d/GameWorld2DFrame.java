package org.simbrain.world.gameworld2d;

import java.awt.BorderLayout;

import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.simbrain.workspace.Workspace;
import org.simbrain.world.odorworld.OdorWorldFrame;

public class GameWorld2DFrame extends JInternalFrame implements InternalFrameListener  {

    /** Workspace containing frame. */
    private Workspace workspace;
    /** For workspace persistence. */
    private String path;
    /** X position of frame. */
    private int xpos;
    /** Y position of frame. */
    private int ypos;
    /** Width of frame . */
    private int theWidth;
    /** Height of frame. */
    private int theHeight;
    /** Has frame been changed since last save. */
    private boolean changedSinceLastSave = false;
    
    private GameWorld2D world;
    
    /**
     * Construct a new world panel.  Set up the toolbars.  Create an  instance of a world object.
     * @param ws the workspace associated with this frame
     */
    public GameWorld2DFrame(final Workspace ws) {
        workspace = ws;
        init();
    }
    
    /**
     * Initializes frame.
     */
    public void init() {
        this.setResizable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setClosable(true);
        this.addInternalFrameListener(this);
        getContentPane().setLayout(new BorderLayout());
        world = new GameWorld2D();
        world.initEngineApplet(60, 40, 10, 10, null, null, null);
        getContentPane().add("Center", world);


        setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);


        setVisible(true);
    }
    
    /**
     * Tasks to peform when frame is opened.
     * @param e Internal frame event
     */
    public void internalFrameOpened(final InternalFrameEvent e) {
    }

    /**
     * Tasks to perform when frame is closing.
     * @param e Internal frame event
     */
    public void internalFrameClosing(final InternalFrameEvent e) {
//        if (isChangedSinceLastSave()) {
//            hasChanged();
//        } else {
//            dispose();
//        }
    }

    /**
     * Tasks to perform when frame is closed.
     * @param e Internal frame event
     */
    public void internalFrameClosed(final InternalFrameEvent e) {
       // this.getWorkspace().removeAgentsFromCouplings(this.getWorld());
       // this.getWorkspace().getOdorWorldList().remove(this);

        OdorWorldFrame odo = workspace.getLastOdorWorld();

        if (odo != null) {
            odo.grabFocus();
            workspace.repaint();
        }

        //OdorWorldPreferences.setCurrentDirectory(currentDirectory);
    }

    /**
     * Tasks to perform when frame is iconified.
     * @param e Internal frame event
     */
    public void internalFrameIconified(final InternalFrameEvent e) {
    }

    /**
     * Tasks to peform when frame is deiconified.
     * @param e Internal frame event
     */
    public void internalFrameDeiconified(final InternalFrameEvent e) {
    }

    /**
     * Tasks to perform when frame is activated.
     * @param e Internal frame event
     */
    public void internalFrameActivated(final InternalFrameEvent e) {
    }

    /**
     * Tasks to perform when frame is deactivated.
     * @param e Internal frame event
     */
    public void internalFrameDeactivated(final InternalFrameEvent e) {
    }

    /**
     * @return Returns the workspace.
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    /**
     * @param workspace The workspace to set.
     */
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    /**
     * @return Returns the world.
     */
    public GameWorld2D getWorld() {
        return world;
    }

    /**
     * @param world The world to set.
     */
    public void setWorld(GameWorld2D world) {
        this.world = world;
    }


}
