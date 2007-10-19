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
package org.simbrain.world.dataworld;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.workspace.CouplingContainer;
import org.simbrain.workspace.WorkspaceComponent;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;


/**
 * <b>DataWorldComponent</b> is a "spreadsheet world" used to send rows of raw data to input nodes.
 */
public class DataWorldComponent extends WorkspaceComponent {

    
    /**
     * This method is the default constructor.
     */
    public DataWorldComponent(String name) {
        super(name);
    }

    /**
     * Read a world from a world-xml file.
     *
     * @param theFile the xml file containing world information
     */
    public void open(final File theFile) {
        // TODO implement
    }

    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(TableModel.class, "consumers");
        xstream.omitField(TableModel.class, "producers");
        xstream.omitField(TableModel.class, "couplingList");
        xstream.omitField(TableModel.class, "model");
        return xstream;
    }

    /**
     * Save a specified file.
     *
     * @param worldFile File to save world
     */
    public void save(final File worldFile) {
     // TODO implement
    }

    @Override
    public String getFileExtension() {
       return "xml";
    }

//    @Override
//    public void setCurrentDirectory(final String currentDirectory) {        
//        super.setCurrentDirectory(currentDirectory);
//        DataWorldPreferences.setCurrentDirectory(currentDirectory);
//    }
//
//    @Override
//    public String getCurrentDirectory() {
//        return DataWorldPreferences.getCurrentDirectory();
//    }

   /**
    * Returns reference to table model which contains couplings.
    */
//   public CouplingContainer getCouplingContainer() {
//       return this.getWorld().getTableModel();
//   }

    @Override
    public void update() {
        
    }

    @Override
    public void close() {
    }
}

