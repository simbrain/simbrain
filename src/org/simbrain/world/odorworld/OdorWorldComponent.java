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
package org.simbrain.world.odorworld;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.Attribute;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.world.dataworld.DataModel;
import org.simbrain.world.dataworld.DataWorldComponent;
import org.simbrain.world.gameworld2d.GameWorld2DComponent;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * <b>WorldPanel</b> is the container for the world component.   Handles toolbar buttons, and serializing of world
 * data.  The main environment codes is in {@link OdorWorldPanel}.
 */
public class OdorWorldComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    /** Reference to model world. */
    private OdorWorld world = new OdorWorld(this);
    
    /**
     * Default constructor.
     */
    public OdorWorldComponent(final String name) {
        super(name);
        this.setStrategy(Strategy.TOTAL);
    }
    
    @SuppressWarnings("unchecked")
    private OdorWorldComponent(final String name, final OdorWorld world) {
        super(name);
        this.world = world;
        this.setStrategy(Strategy.TOTAL);
        world.setParent(this);
    }


    /**
     * Recreates an instance of this class from a saved component.
     * 
     * @param input
     * @param name
     * @param format
     * @return
     */
    public static OdorWorldComponent open(InputStream input, String name, String format) {
        OdorWorld newWorld = (OdorWorld) OdorWorld.getXStream().fromXML(input);
        return new OdorWorldComponent(name, newWorld);
    }

    @Override
    public String getXML() {
        return OdorWorld.getXStream().toXML(world);
    }

    @Override
    public void deserializeFromReader(FileReader reader) {
        world = (OdorWorld) OdorWorld.getXStream().fromXML(reader);
        world.setParent(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        OdorWorld.getXStream().toXML(world, output);
    }
    
    public OdorWorld getWorld() {
        return world;
    }
    

    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }
    
    @Override
    public List<? extends Consumer> getConsumers() {
        return world.getConsumers();
    }
    
    @Override
    public List<? extends Producer> getProducers() {
        return world.getProducers();
    }

    @Override
    protected void update() {
        /* no implementation */
    }

    @Override
    public Attribute getAttributeForKey(String key) {

        Matcher matcher = Pattern.compile("(.+):(.+)").matcher(key);
        
        if (!matcher.matches()) {
            System.out.println("No match");
            return null;
        }
        
        String agentName = matcher.group(1);
        String attribute = matcher.group(2);

        OdorWorldAgent theAgent = world.findAgent(agentName);
        
        //TODO: Make below a standard attribute holder function?        
        for (Attribute a : theAgent.getConsumingAttributes()) {
            if (a.getAttributeDescription().equals(attribute)) return a;
        }
        for (Attribute a : theAgent.getProducingAttributes()) {
            if (a.getAttributeDescription().equals(attribute)) return a;
        }
        // No match found
        return null;
    }

    @Override
    public String getKeyForAttribute(Attribute attribute) {
        String agentName = ((OdorWorldAgent) attribute.getParent()).getName();
        String attributeName = attribute.getAttributeDescription();
        return agentName + ":" + attributeName;
    }
    
    @Override
    public void setCurrentDirectory(final String currentDirectory) { 
        super.setCurrentDirectory(currentDirectory);
        OdorWorldPreferences.setCurrentDirectory(currentDirectory);
    }
    
    @Override
    public String getCurrentDirectory() {
       return OdorWorldPreferences.getCurrentDirectory();
    }
    
    
}