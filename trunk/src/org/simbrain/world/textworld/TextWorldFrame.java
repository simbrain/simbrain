package org.simbrain.world.textworld;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
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
    
    private JMenuBar menuBar = new JMenuBar();
    private JMenu file = new JMenu("File  ");
    private JMenuItem open = new JMenuItem("Open");
    private JMenuItem save = new JMenuItem("Save");
    private JMenuItem saveAs = new JMenuItem("Save As");
    private JMenuItem close = new JMenuItem("Close");
    private JMenu edit = new JMenu("Edit  ");
    private JMenuItem dictionary = new JMenuItem("Dictionary");
    private JMenu help = new JMenu("Help");
    
    public TextWorldFrame(Workspace ws){
        workspace = ws;
        init();
    }
    
    private void init(){
        
        this.setResizable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setClosable(true); 
        this.addInternalFrameListener(this);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add("Center", worldScroller);
        world = new TextWorld(this);
        addMenuBar();
        worldScroller.setViewportView(world);
        worldScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        worldScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        worldScroller.setEnabled(false);
        setVisible(true);
    }
    
    private void addMenuBar(){
        open.addActionListener(this);
        open.setActionCommand("open");
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        save.addActionListener(this);
        save.setActionCommand("save");
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        saveAs.addActionListener(this);
        saveAs.setActionCommand("saveAs");
        close.addActionListener(this);
        close.setActionCommand("close");
        close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuBar.add(file);
        file.add(open);
        file.add(save);
        file.add(saveAs);
        file.add(close);
        file.addMenuListener(this);
        
        dictionary.addActionListener(this);
        dictionary.setActionCommand("dictionary");
        dictionary.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuBar.add(edit);
        edit.add(dictionary);
        edit.addMenuListener(this);
        
        menuBar.add(help);
        
        setJMenuBar(menuBar);
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
        dispose();

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
