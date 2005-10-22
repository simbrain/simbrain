package org.simbrain.world.textworld;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.workspace.Workspace;

public class TextWorldFrame extends JInternalFrame implements ActionListener,
        InternalFrameListener, MenuListener {
    
    private JScrollPane worldScroller = new JScrollPane();
    private TextWorld world;
    private Workspace workspace;
    
    public TextWorldFrame(Workspace ws){
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add("Center", worldScroller);
        world = new TextWorld(this);
        worldScroller.setViewportView(world);
        worldScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        worldScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        worldScroller.setEnabled(false);
        setVisible(true);
    }

    public void setName(String name) {
        setTitle(name);     
        world.setName(name);
        
    }
    
    public TextWorld getWorld() {
        return world;
    }
    
    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void internalFrameActivated(InternalFrameEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void internalFrameClosed(InternalFrameEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void internalFrameClosing(InternalFrameEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void internalFrameDeactivated(InternalFrameEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void internalFrameDeiconified(InternalFrameEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void internalFrameIconified(InternalFrameEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void internalFrameOpened(InternalFrameEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void menuCanceled(MenuEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void menuDeselected(MenuEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void menuSelected(MenuEvent arg0) {
        // TODO Auto-generated method stub

    }

}
