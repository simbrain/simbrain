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
package org.simbrain.gauge;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.gauge.core.Gauge;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

/**
 * <b>GaugeComponent</b> wraps a Gauge object in a Simbrain workspace frame, which also stores information about the
 * variables the Gauge is representing.
 */
public class GaugeComponent extends WorkspaceComponent<WorkspaceComponentListener> implements ActionListener, MenuListener {

    public GaugeComponent(String name) {
        super(name);
    }

    /** Logger. */
//    private Logger logger = Logger.getLogger(GaugeComponent.class);

    /** Current gauge. */
    private Gauge gauge = new Gauge(this);
    
    public Gauge getGauge() {
        return gauge;
    }
    
    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
//    private XStream getXStream() {
//        XStream xstream = new XStream(new DomDriver());
//        xstream.omitField(Projector.class, "logger");
//        xstream.omitField(Dataset.class, "logger");
//        xstream.omitField(Dataset.class, "distances");
//        xstream.omitField(Dataset.class, "dataset");
//        return xstream;
//    }

    @Override
    protected void update() {
        gauge.updateCurrentState();
    }
    
    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getFileExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void open(File openFile) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void save(File saveFile) {
        // TODO Auto-generated method stub
        
    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        
    }

    public void menuCanceled(MenuEvent e) {
        // TODO Auto-generated method stub
        
    }

    public void menuDeselected(MenuEvent e) {
        // TODO Auto-generated method stub
        
    }

    public void menuSelected(MenuEvent e) {
        // TODO Auto-generated method stub
        
    }
}
