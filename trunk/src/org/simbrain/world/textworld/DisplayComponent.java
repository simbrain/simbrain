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
package org.simbrain.world.textworld;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * <b>DisplayComponent</b> is a component which wraps a display world with consumers.
 */
public class DisplayComponent extends WorkspaceComponent {

    /** Instance of world of type DisplayWorld. */
    private final DisplayWorld world;

    /**
     * Creates a new frame of type TextWorld.
     *
     * @param name name of this component
     */
    public DisplayComponent(String name) {
        super(name);
        world = new DisplayWorld();
        init();
    }

    /**
     * Construct a component from an existing world; used in deserializing.
     *
     * @param name name of component
     * @param newWorld provided world
     */
    public DisplayComponent(String name, DisplayWorld newWorld) {
        super(name);
        world = newWorld;
        init();
    }
    
    /**
     * Initialize attribute types.
     */
    private void init() {
        addConsumerType(new AttributeType(this, "StringReader", String.class, false));
        addConsumerType(new AttributeType(this, "WordReader", double.class, true));
    }

    @Override
    public List<PotentialConsumer> getPotentialConsumers() {
        List<PotentialConsumer> returnList = new ArrayList<PotentialConsumer>();
        for (AttributeType type : getVisibleConsumerTypes()) {
            if (type.getTypeName().equalsIgnoreCase("StringReader")) {
                returnList.add(new PotentialConsumer(this, world, "addText", String.class, "StringReader"));                
            }
            if (type.getTypeName().equalsIgnoreCase("WordReader")) {
                for (String word : world.getDictionary()) {
                    returnList.add(new PotentialConsumer(this, world,
                            "addTextIfAboveThreshold", new Class<?>[] {
                                    double.class, String.class },
                            new Object[] { word }, word));
                }                
            }
        }
       return returnList;
    }


    /**
     * {@inheritDoc}
     */
    public static DisplayComponent open(InputStream input, String name,
            String format) {
        DisplayWorld newWorld = (DisplayWorld) DisplayWorld.getXStream().fromXML(input);
        return new DisplayComponent(name, newWorld);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        DisplayWorld.getXStream().toXML(world, output);
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }

    @Override
    public void update() {
        world.update();
    }

    /**
     * @return the world
     */
    public DisplayWorld getWorld() {
        return world;
    }
}
