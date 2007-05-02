package org.simbrain.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JScrollPane;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.simbrain.workspace.Workspace;

import ptolemy.plot.Plot;

public class StandardPlot extends JInternalFrame implements InternalFrameListener  {

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

    private Plot plot;

    /**
     * Construct a new world panel.  Set up the toolbars.  Create an  instance of a world object.
     * @param ws the workspace associated with this frame
     */
    public StandardPlot(final Workspace ws) {
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
        plot = new Plot();
        plot.samplePlot();
        getContentPane().add("Center", plot);


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
        dispose();
    }

    /**
     * Tasks to perform when frame is closed.
     * @param e Internal frame event
     */
    public void internalFrameClosed(final InternalFrameEvent e) {
       // this.getWorkspace().removeAgentsFromCouplings(this.getWorld());
       // this.getWorkspace().getOdorWorldList().remove(this);


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

}
