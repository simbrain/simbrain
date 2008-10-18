package org.simbrain.workspace.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

/**
 * Abstraction which is neutral between JFrames and JInternalFrames.
 *
 * @author jyoshimi
 */
public interface GenericFrame {

    public void dispose();
    
    public void pack();
    
    public void setTitle(String title);
    
    public String getTitle();
    
    public void setJMenuBar(JMenuBar menuBar);
    
    public void setBounds(int x, int y, int width, int height);

    public Rectangle getBounds();
    
}
