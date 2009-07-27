/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.workspace.gui;

import java.awt.Rectangle;
import java.beans.PropertyVetoException;

import javax.swing.JMenuBar;

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
    
    public void setIcon(boolean b) throws PropertyVetoException;
    
    public void setJMenuBar(JMenuBar menuBar);
    
    public JMenuBar getJMenuBar();
    
    public void setBounds(int x, int y, int width, int height);

    public Rectangle getBounds();
    
    public void setVisible(boolean isVisible);
    
}
